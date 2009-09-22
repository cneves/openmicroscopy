"""
    OMERO.fs Monitor module for Linux.


"""
import logging
import fsLogger
log = logging.getLogger("fsserver."+__name__)

import pyinotify

import threading
import sys, traceback
import uuid
import socket

# Third party path package. It provides much of the 
# functionality of os.path but without the complexity.
# Imported as pathModule to avoid potential clashes.
import path as pathModule

from omero.grid import monitors
 

class PlatformMonitor(object):
    """
        A Thread to monitor a path.
        
        :group Constructor: __init__
        :group Other methods: run, stop

    """

    def setUp(self, eventTypes, pathMode, pathString, whitelist, blacklist, ignoreSysFiles, monitorId, proxy):
        """
            Set-up Monitor thread.
            
            After initialising the superclass and some instance variables
            try to create an FSEventStream. Throw an exeption if this fails.
            
            :Parameters:
                eventTypes : 
                    A list of the event types to be monitored.          
                    
                pathMode : 
                    The mode of directory monitoring: flat, recursive or following.

                pathString : string
                    A string representing a path to be monitored.
                  
                whitelist : list<string>
                    A list of files and extensions of interest.
                    
                blacklist : list<string>
                    A list of subdirectories to be excluded.

                ignoreSysFiles :
                    If true platform dependent sys files should be ignored.
                    
                monitorId :
                    Unique id for the monitor included in callbacks.
                    
                proxy :
                    A proxy to be informed of events
                    
        """
        threading.Thread.__init__(self)
 
        if str(pathMode) not in ['Flat', 'Follow', 'Recurse']:
            raise UnsupportedPathMode("Path Mode " + str(pathMode) + " not yet supported on this platform")

        recurse = False
        follow = False
        if str(pathMode) == 'Follow':
            recurse = True
            follow = True
        elif str(pathMode) == 'Recurse':
            recurse = True
        
        if str(eventType) not in ['Create']:
            raise UnsupportedEventType("Event Type " + str(eventType) + " not yet supported on this platform")
            
        self.whitelist = whitelist
        
        self.proxy = proxy
            
        pathsToMonitor = pathString
        if pathsToMonitor == None:
            pathsToMonitor = pathModule.path.getcwd()

        wm = pyinotify.WatchManager()
        pr = ProcessEvent(id=monitorId, func=self.callback, wl=whitelist)
        self.notifier = pyinotify.Notifier(wm, pr)
        
        if str(eventType) == 'Create':
            wm.add_watch(pathsToMonitor, (pyinotify.IN_CLOSE_WRITE | pyinotify.IN_MOVED_TO), rec=recurse, auto_add=follow)
            log.info('Monitor set-up on =' + str(pathsToMonitor))

    def callback(self, id, eventPath):
        """
            Callback required by FSEvents.FSEventStream.
        
            :Parameters:
                    
                id : string
		    watch id.
                    
                eventPath : string
                    File paths of the event.
                    
            :return: No explicit return value.
            
        """     
        monitorId = id        
        eventList = []
        eventType = monitors.EventType.Create
        eventList.append((eventPath,eventType))  
        log.info('Event notification on monitor id=' + monitorId + ' => ' + str(eventList))
        self.proxy.callback(monitorId, eventList)

                
    def start(self):
        """
            Start monitoring an FSEventStream.
   
            This method, overridden from Thread, is run by 
            calling the inherited method start(). The method attempts
            to schedule an FSEventStream and then run its CFRunLoop.
            The method then blocks until stop() is called.
            
            :return: No explicit return value.
            
        """

        # Blocks
        self.notifier.loop()
        
    def stop(self):        
        """
            Stop monitoring an FSEventStream.
   
            This method attempts to stop the CFRunLoop. It then
            stops, invalidates and releases the FSEventStream.
            
            There should be a more robust approach in here that 
            still kils the thread even if the first call fails.
            
            :return: No explicit return value.
            
        """
        self.notifier.stop()
                

class ProcessEvent(pyinotify.ProcessEvent):

    def my_init(self, **kwargs):
        self.callback = kwargs['func'] 
        self.id = kwargs['id']
        self.whitelist = kwargs['wl']

    def process_IN_CLOSE_WRITE(self, event):
        # Explicitely registered for this kind of event.
        log.info("Raw pyinotify event = %s", str(event))
        try:
            if (len(self.whitelist) == 0) or (pathModule.path(event.pathname).ext in self.whitelist):
                self.callback(self.id, event.pathname)
        except:
            log.exception("Failed to process event: ")

    def process_IN_MOVED_TO(self, event):
        # Explicitely registered for this kind of event.
        log.info("Raw pyinotify event = %s", str(event))
        try:
            if (len(self.whitelist) == 0) or (pathModule.path(event.pathname).ext in self.whitelist):
                self.callback(self.id, event.pathname)
        except:
            log.exception("Failed to process event: ")

    def process_default(self, event):
        # Implicitely IN_CREATE and IN_DELETE are watched. They are
        # quietly ignored at the present time.
        pass
	

