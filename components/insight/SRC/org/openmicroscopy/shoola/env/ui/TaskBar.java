/*
 * org.openmicroscopy.shoola.env.ui.TaskBar
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.env.ui;

//Java imports
import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

//Third-party libraries

//Application-internal dependencies

/** 
 * Defines the functionality of the task bar UI.
 * <p>The task bar is a top level window that contains a menu bar and a series
 * of toolbars.  The container brings this window up after initialization for
 * the user to control some of the container's tasks &#151; like the connection
 * to remote services or quitting the application.</p>
 * <p>Agents that have a UI typically add an entry to the {@link #WINDOW_MENU}
 * and to the {@link #QUICK_LAUNCH_TOOLBAR} (during the linking phase) for top
 * level windows that the user can bring up.<br>
 * The {@link TopWindow} class has built-in functionality to provide this 
 * linkage as well as functionality to manage the display state of the window.
 * So agents with a single top level window may want to have their window
 * inherit from {@link TopWindow}.  If an agent allows multiple simultaneous
 * instances of the same top level window, then it can use the 
 * {@link TopWindowGroup} to group all those instances together in the task bar
 * under a common {@link #WINDOW_MENU} entry and a drop-down button in the 
 * {@link #QUICK_LAUNCH_TOOLBAR}.  Like {@link TopWindow}, 
 * {@link TopWindowGroup} also takes care of managing the display state of its
 * windows; however, it doesn't require its managed windows to inherit from 
 * {@link TopWindow}.</p> 
 *
 * @see org.openmicroscopy.shoola.env.ui.TopWindow
 * @see org.openmicroscopy.shoola.env.ui.TopWindowGroup
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author  <br>Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:a.falconi@dundee.ac.uk">
 * 					a.falconi@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */
public interface TaskBar 
{
	
	//NOTE: The TaskBarView uses these constants to do direct indexing.
	//So changing these values requires a review of TaskBarView as well.  
	
	/** Identifies the file menu within the menu bar. */
	//public static final int		FILE_MENU = 0;
    
    /** 
     * Identifies the tasks menu within the menu bar.
     * Entries in this menu trigger actions related to the application
     * workflow.
     */
    //public static final int     TASKS_MENU = 0;
    
	/** 
	 * Identifies the connect menu within the menu bar.
	 * Entries in this menu trigger actions to connect/disconnect to/from
	 * remote services.
	 */
	//public static final int		CONNECT_MENU = 1;
	
	/** 
	 * Identifies the window menu within the menu bar.
	 * Entries in this menu trigger actions to bring up top level windows.
	 */
	public static final int		WINDOW_MENU = 0;
	
	/** Identifies the help menu within the menu bar. */
	public static final int		HELP_MENU = 1;
	
	/** Identifies the <code>Send Comment</code> menu item. */
	public static final int		COMMENT = 100;
	
	/** Identifies the <code>Help content</code> menu item. */
	public static final int		HELP_CONTENTS = 101;
    
	/**
	 * Adds <code>entry</code> to the specified menu.
	 * 
	 * @param menuID	ID of one of the menus supported by the task bar.  Must
	 * 					be one of the constants defined by this interface.
	 * @param entry		The item to add.
	 * @throws IllegalArgumentException	If <code>menuID</code> is not valid.
	 * @throws NullPointerException	If <code>entry</code> is <code>null</code>.
	 */
	public void addToMenu(int menuID, JMenuItem entry);
	
	/**
	 * Removes <code>entry</code> from the specified menu.  
	 * 
	 * @param menuID	ID of one of the menus supported by the task bar.  Must
	 * 					be one of the constants defined by this interface.
	 * @param entry		The item to remove.
	 * @throws IllegalArgumentException	If <code>menuID</code> is not valid.
	 */
	public void removeFromMenu(int menuID, JMenuItem entry);
	
	/**
	 * Adds <code>entry</code> to the specified toolbar.
	 * The <code>entry</code> is assumed to have a <code>16x16</code> icon and
	 * no text.
	 * 
	 * @param toolBarID	ID of one of the toolbars supported by the task bar. 
	 * 					Must be one of the constants defined by this interface.
	 * @param entry		The item to add.
	 * @throws IllegalArgumentException	If <code>toolBarID</code> is not valid.
	 * @throws NullPointerException	If <code>entry</code> is <code>null</code>.
	 */
	public void addToToolBar(int toolBarID, AbstractButton entry);
	
	/**
	 * Removes <code>entry</code> from the specified toolbar.  
	 * 
	 * @param toolBarID	ID of one of the toolbars supported by the task bar. 
	 * 					Must be one of the constants defined by this interface.
	 * @param entry		The item to remove.
	 * @throws IllegalArgumentException	If <code>toolBarID</code> is not valid.
	 */
	public void removeFromToolBar(int toolBarID, AbstractButton entry);
    
    /**
     * Returns a reference to the task bar window.
     * 
     * @return See above.
     */
    public JFrame getFrame();

    /**
     * Returns the <code>JMenuBar</code> of the task bar.
     * 
     * @return See above.
     */
    public JMenuBar getTaskBarMenuBar();
    
    /**
     * Returns a copy of the <code>Windows</code> menu. New items should
     * be added using the method {@link #addToMenu(int, JMenuItem)}
     * 
     * @return See above.
     */
    public JMenu getWindowsMenu();
    
    /**
     * Returns a copy of <code>Help</code> menu. New items should
     * be added using the method {@link #addToMenu(int, JMenuItem)}
     * 
     * @return See above.
     */
    public JMenu getHelpMenu();
    
    /**
     * Builds and returns a copy of the menu item specified by the passed index.
     * If the passed value is not supported, a <code>null</code> value
     * is returned.
     * 
     * @param index	The index of the item to copy.
     * @return See above.
     */
    public JMenuItem getCopyMenuItem(int index);
    
    /**
     * Returns <code>true</code> if already connected,
     * <code>false</code> otherwise.
     * 
     * @return See above.
     */
    public boolean login();

}
