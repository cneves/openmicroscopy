/*
 * ome.formats.importer.ImportLibrary
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2005 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *------------------------------------------------------------------------------
 */

package ome.formats.importer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import loci.common.DataTools;
import loci.formats.FormatException;
import loci.formats.FormatReader;
import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import loci.formats.MissingLibraryException;
import loci.formats.StatusEvent;
import loci.formats.StatusListener;
import loci.formats.UnknownFormatException;
import loci.formats.in.MIASReader;
import ome.formats.OMEROMetadataStoreClient;
import ome.formats.OverlayMetadataStore;
import ome.formats.importer.util.ErrorHandler;
import ome.formats.model.InstanceProvider;
import omero.ServerError;
import omero.api.ServiceFactoryPrx;
import omero.model.Annotation;
import omero.model.Dataset;
import omero.model.DatasetI;
import omero.model.FileAnnotation;
import omero.model.IObject;
import omero.model.Image;
import omero.model.OriginalFile;
import omero.model.Pixels;
import omero.model.Plate;
import omero.model.Screen;
import omero.model.WellSample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * support class for the proper usage of {@link OMEROMetadataStoreClient} and
 * {@link FormatReader} instances. This library was factored out of
 * ImportHandler to support ImportFixture The general workflow
 * for this class (as seen in {@link ImportFixture} is: <code>
 *   ImportLibrary library = new ImportLibrary(store,reader,files);
 *   for (File file : files) {
 *     String fileName = file.getAbsolutePath();
 *     library.open(fileName);
 *     int count = library.calculateImageCount(fileName);
 *     long pixId = library.importMetadata();
 *     library.importData(pixId, fileName, new ImportLibrary.Step(){
 *       public void step(int i) {}});
 *   }
 * </code>
 * 
 * @author Josh Moore, josh.moore at gmx.de
 * @version $Revision: 1167 $, $Date: 2006-12-15 10:39:34 +0000 (Fri, 15 Dec 2006) $
 * @see FormatReader
 * @see OMEROMetadataStoreClient
 * @see ImportFixture
 * @see IObservable
 * @see IObserver
 * @since 3.0-M3
 */
public class ImportLibrary implements IObservable, StatusListener
{
   
    private static Log log = LogFactory.getLog(ImportLibrary.class);

    private final ArrayList<IObserver> observers = new ArrayList<IObserver>();

    private final boolean dumpPixels = false;    

    private final OMEROMetadataStoreClient store;

    private final OMEROWrapper reader;
    
    private byte[] arrayBuf;

    /**
     * The library will not close the client instance. The reader will be closed
     * between calls to import.
     * 
     * @param store not null
     * @param reader not null
     */
    public ImportLibrary(OMEROMetadataStoreClient client, OMEROWrapper reader)
    {
        if (client == null || reader == null)
        {
            throw new NullPointerException(
                    "All arguments to ImportLibrary() must be non-null.");
        }
        
        this.store = client;
        this.reader = reader;
        reader.addStatusListener(this);
    }

    //
    // Delegation methods
    //
    

    public long getExperimenterID() {
        return store.getExperimenterID();
    }


    public InstanceProvider getInstanceProvider() {
        return store.getInstanceProvider();
    }

    //
    // Observable methods
    //
    
    public boolean addObserver(IObserver object) {
        return observers.add(object);
    }

    public boolean deleteObserver(IObserver object) {
        return observers.remove(object);

    }

    public void notifyObservers(ImportEvent event) {
        for (IObserver observer : observers) {
            observer.update(this, event);
        }
    }
    
    public void statusUpdated(StatusEvent e) {
    	String currentFile = reader.getCurrentFile();
        notifyObservers(new ImportEvent.BIOFORMATS_STATUS(currentFile, e));
    	
    }

    
    // ~ Actions
    // =========================================================================


    /**
     * Primary user method for importing a number 
     */
    public void importCandidates(ImportConfig config, ImportCandidates candidates) {
        List<ImportContainer> containers = candidates.getContainers();
        if (containers != null) {
            int numDone = 0;
            for (int index = 0; index < containers.size(); index++) {
                ImportContainer ic = containers.get(index);
                
                if (config.targetClass.get() == "omero.model.Dataset")
                {
                	ic.setTarget(store.getTarget(Dataset.class, config.targetId.get()));
                }
                else if (config.targetClass.get() == "omero.model.Screen")
                {
                	ic.setTarget(store.getTarget(Screen.class, config.targetId.get()));                	
                }
                
                try {
                    importImage(
                            ic.file,
                            index, numDone, containers.size(),
                            ic.getUserSpecifiedName(),
                            "",
                            ic.getArchive(),
                            true,
                            ic.getUserPixels(),
                            ic.getTarget());
                    numDone++;
                } catch (Throwable t) {                    
                    if (!config.contOnError.get()) {
                        log.info("Exiting on error");
                        return;
                    } else {
                        log.info("Continuing after error");
                    }
                }
            }
        }
    }

    /** opens the file using the {@link FormatReader} instance */
    private void open(String fileName) throws IOException, FormatException
    {
        reader.close();
        reader.setMetadataStore(store);
        reader.setMinMaxStore(store);
        reader.setId(fileName);
        store.setReader(reader.getImageReader());
        //reset series count
        log.debug("Image Count: " + reader.getImageCount());
    }

    /**
     * Uses the {@link OMEROMetadataStoreClient} to save the current all
     * image metadata provided.
     * 
     * @param index Index of the file being imported.
     * @param userSpecifiedImageName A user specified image name.
     * @param userSpecifiedImageDescription A user specified description.
     * @return the newly created {@link Pixels} id.
	 * @throws FormatException if there is an error parsing metadata.
	 * @throws IOException if there is an error reading the file.
     */
	private List<Pixels> importMetadata(
			                            int index,
	                                    IObject userSpecifiedTarget,
	                                    String userSpecifiedImageName,
			                            String userSpecifiedImageDescription,
			                            Double[] userPixels
			                            )
    	throws FormatException, IOException
    {
    	// 1st we post-process the metadata that we've been given.
    	notifyObservers(new ImportEvent.BEGIN_POST_PROCESS(index, null, userSpecifiedTarget, null, 0, null));
    	store.setUserSpecifiedImageName(userSpecifiedImageName);
    	store.setUserSpecifiedImageDescription(userSpecifiedImageDescription);
    	if (userPixels != null)
    	    store.setUserSpecifiedPhysicalPixelSizes(userPixels[0], userPixels[1], userPixels[2]);
    	store.setUserSpecifiedTarget(userSpecifiedTarget);
        store.postProcess();
        notifyObservers(new ImportEvent.END_POST_PROCESS(index, null, userSpecifiedTarget, null, 0, null));
        
        notifyObservers(new ImportEvent.BEGIN_SAVE_TO_DB(index, null, userSpecifiedTarget, null, 0, null));
        List<Pixels> pixelsList = store.saveToDB();
        notifyObservers(new ImportEvent.END_SAVE_TO_DB(index, null, userSpecifiedTarget, null, 0, null));
        return pixelsList;
    }
    
	/**
	 * If available, populates overlays for a given set of pixels objects.
	 * @param pixelsList Pixels objects to populate overlays for.
	 * @param plateIds Plate object IDs to populate overlays for.
	 */
	private void importOverlays(List<Pixels> pixelsList, List<Long> plateIds)
		throws ServerError, FormatException, IOException
	{
		IFormatReader baseReader = reader.getImageReader().getReader();
		if (baseReader instanceof MIASReader)
		{
			try
			{
				MIASReader miasReader = (MIASReader) baseReader;
				String currentFile = miasReader.getCurrentFile();
				reader.close();
				miasReader.setAutomaticallyParseMasks(true);
				ServiceFactoryPrx sf = store.getServiceFactory();
				OverlayMetadataStore s = new OverlayMetadataStore();
				s.initialize(sf, pixelsList, plateIds);
				reader.setMetadataStore(s);
				miasReader.close();
				miasReader.setAutomaticallyParseMasks(true);
				miasReader.setId(currentFile);
				s.complete();
			}
			catch (ServerError e)
			{
				log.warn("Error while populating MIAS overlays.", e);
			}
			finally
			{
				reader.close();
				reader.setMetadataStore(store);
			}
		}
	}
	
    /**
     * Perform an image import.  <em>Note: this method both notifes {@link #observers}
     * of error states AND throws the exception to cancel processing.</em>
     * {@link #importCandidates(ImportConfig, ImportCandidates)}
     * uses {@link ImportConfig#contOnError} to act on these exceptions.
     * 
     * @param file Target file to import.
     * @param index Index of the import in a set. <code>0</code> is safe if 
     * this is a singular import.
     * @param numDone Number of imports completed in a set. <code>0</code> is 
     * safe if this is a singular import.
     * @param total Total number of imports in a set. <code>1</code> is safe
     * if this is a singular import.
     * @param userSpecifiedImageName Name to use for all images that are imported from the
     * target file <code>file</code>.
     * @param userSpecifiedImageDescription Description to use for all images that are
     * imported from target file <code>file</code>
     * @param archive Whether or not to archive target file <code>file</code>
     * and all sub files.
     * @param useMetadataFile Whether or not to dump all metadata to a flat
     * file annotation on the server.
     * @param userSpecifiedTarget the IObject instances which will be used by
     * the {@link #importMetadata(String, String, boolean, boolean, Double[])}
     * method.
     * @return List of Pixels that have been imported.
     * @throws FormatException If there is a Bio-Formats image file format
     * error during import.
     * @throws IOException If there is an I/O error.
     * @throws ServerError If there is an error communicating with the OMERO
     * server we're importing into.
     */
    public List<Pixels> importImage(File file, int index, int numDone,
    		                        int total, String userSpecifiedImageName, 
    		                        String userSpecifiedImageDescription,
    		                        boolean archive, boolean useMetadataFile,
    		                        Double[] userPixels, IObject userSpecifiedTarget)
    	throws FormatException, IOException, Throwable
    {   

        String fileName = file.getAbsolutePath();
        String shortName = file.getName();
        String format = null;
        String[] domains = null;
        String[] usedFiles = new String[1];
        boolean isScreeningDomain = false;
        usedFiles[0] = file.getAbsolutePath();

        try {
            
            notifyObservers(new ImportEvent.LOADING_IMAGE(shortName, index, numDone, total));
        
            open(file.getAbsolutePath());
            format = reader.getFormat();
            domains = reader.getDomains();
            if (reader.getUsedFiles() != null)
            {
            	usedFiles = reader.getUsedFiles();
            }
            for (String domain : domains)
            {
            	if (domain.equals(FormatTools.HCS_DOMAIN))
            	{
            		isScreeningDomain = true;
            		break;
            	}
            }
            notifyObservers(new ImportEvent.LOADED_IMAGE(shortName, index, numDone, total));
            
            String formatString = reader.getImageReader().getReader().getClass().toString();
            formatString = formatString.replace("class loci.formats.in.", "");
            formatString = formatString.replace("Reader", "");
            
            // Save metadata and prepare the RawPixelsStore for our arrival.
            List<File> metadataFiles;
            if (isScreeningDomain)
            {
            	log.info("Reader is of HCS domain, disabling metafile.");
            	
            	metadataFiles = store.setArchiveScreeningDomain(archive);
            }
            else
            {
            	log.info("Reader is not of HCS domain, use metafile: "
            			 + useMetadataFile);
            	metadataFiles = store.setArchive(archive, useMetadataFile);
            }
            List<Pixels> pixList = 
            	importMetadata(index,
            			       userSpecifiedTarget,
            	               userSpecifiedImageName,
            			       userSpecifiedImageDescription,
            			       userPixels);
        	List<Long> plateIds = new ArrayList<Long>();
        	Image image = pixList.get(0).getImage();
        	if (image.sizeOfWellSamples() > 0)
        	{
        		Plate plate = image.copyWellSamples().get(0).getWell().getPlate();
        		plateIds.add(plate.getId().getValue());
        	}
            List<Long> pixelsIds = new ArrayList<Long>(pixList.size());
            for (Pixels pixels : pixList)
            {
            	pixelsIds.add(pixels.getId().getValue());
            }
            store.preparePixelsStore(pixelsIds);
        
            int seriesCount = reader.getSeriesCount();
            boolean saveSha1 = false;
            for (int series = 0; series < seriesCount; series++)
            {
                
                // Calculate the dimensions for import this single file.
                ImportSize size = new ImportSize(fileName, pixList.get(series), reader.getDimensionOrder());
                
                Pixels pixels = pixList.get(series); 
                long pixId = pixels.getId().getValue(); 
                
                notifyObservers(new ImportEvent.DATASET_STORED(index, fileName, userSpecifiedTarget, pixId, series, size, numDone, total));
        
                MessageDigest md = importData(pixId, fileName, series, size);
                if (md != null)
                {
                	String s = OMEROMetadataStoreClient.byteArrayToHexString(md.digest());
                	pixels.setSha1(store.toRType(s));
                	saveSha1 = true;
                }
                
                notifyObservers(new ImportEvent.DATA_STORED(index, fileName, userSpecifiedTarget, pixId, series, size));
            }
            
            // Original file absolute path to original file map for uploading
        	Map<String, OriginalFile> originalFileMap =
        		new HashMap<String, OriginalFile>();
        	for (Pixels pixels : pixList)
        	{
        		Image i = pixels.getImage();
        		for (Annotation annotation : i.linkedAnnotationList())
        		{
        			if (annotation instanceof FileAnnotation)
        			{
        				FileAnnotation fa = (FileAnnotation) annotation;
        				OriginalFile of = fa.getFile();
        				originalFileMap.put(of.getPath().getValue(), of);
        			}
        		}
        		for (OriginalFile of : pixels.linkedOriginalFileList())
        		{
        			originalFileMap.put(of.getPath().getValue(), of);
        		}
        		for (WellSample ws : i.copyWellSamples())
        		{
        			Plate plate = ws.getWell().getPlate();
        			for (Annotation annotation : plate.linkedAnnotationList())
        			{
        				if (annotation instanceof FileAnnotation)
        				{
        					FileAnnotation fa = (FileAnnotation) annotation;
            				OriginalFile of = fa.getFile();
            				originalFileMap.put(of.getPath().getValue(), of);
        				}
        			}
        		}
        	}
        	
        	List<File> fileNameList = new ArrayList<File>();
            if (archive)
            {
            	for (String filename : reader.getUsedFiles())
            	{
            		fileNameList.add(new File(filename));
            	}
            } 
            else
            {
            	for (String filename : store.getFilteredCompanionFiles())
            	{
            		fileNameList.add(new File(filename));
            	}
            }
            
            fileNameList.addAll(metadataFiles);
            if (fileNameList.size() != originalFileMap.size())
            {
            	log.warn(String.format("Original file number mismatch, %d!=%d.", 
            			fileNameList.size(), originalFileMap.size()));
            }
            
            if (archive)
            {
                notifyObservers(new ImportEvent.IMPORT_ARCHIVING(index, null, userSpecifiedTarget, null, 0, null));
            }
        	store.writeFilesToFileStore(fileNameList, originalFileMap);
            
            if (saveSha1)
            {
            	store.updatePixels(pixList);
            }
            
            if (reader.isMinMaxSet() == false)
            {
                store.populateMinMax();
            }

            notifyObservers(new ImportEvent.IMPORT_OVERLAYS(index, null, userSpecifiedTarget, null, 0, null));
            importOverlays(pixList, plateIds);
            notifyObservers(new ImportEvent.IMPORT_THUMBNAILING(index, null, userSpecifiedTarget, null, 0, null));
            store.resetDefaultsAndGenerateThumbnails(plateIds, pixelsIds);
            store.launchProcessing(); // Use or return value here later. TODO
            notifyObservers(new ImportEvent.IMPORT_DONE(index, null, userSpecifiedTarget, null, 0, null, pixList));
            
            return pixList;
        } catch (MissingLibraryException mle) {
            notifyObservers(new ErrorHandler.MISSING_LIBRARY(fileName, mle, usedFiles, format));
            throw mle;
        } catch (IOException io) {
            notifyObservers(new ErrorHandler.FILE_EXCEPTION(fileName, io, usedFiles, format));
            throw io;
        } catch (UnknownFormatException ufe) {
            notifyObservers(new ErrorHandler.UNKNOWN_FORMAT(fileName, ufe, this));
            throw ufe;
        } catch (FormatException fe) {
            notifyObservers(new ErrorHandler.FILE_EXCEPTION(fileName, fe, usedFiles, format));
            throw fe;
        } catch (Exception e) {
            notifyObservers(new ErrorHandler.INTERNAL_EXCEPTION(fileName, e, usedFiles, format));
            throw e;
        } catch (Throwable t) {
        	notifyObservers(new ErrorHandler.INTERNAL_EXCEPTION(fileName, new RuntimeException(t), usedFiles, format));
        	throw t;
        } finally {
            store.createRoot(); // CLEAR MetadataStore
        }
    }
    
    /**
     * saves the binary data to the server. After each successful save,
     * an {@link ImportEvent.IMPORT_STEP} is raised with the number of the iteration just
     * completed.
     * @param series 
     * @return The SHA1 message digest for the Pixels saved.
     */
    public MessageDigest importData(Long pixId, String fileName, int series, ImportSize size)
    throws FormatException, IOException, ServerError
    {
        int i = 1;
        reader.setSeries(series);
        int bytesPerPixel = getBytesPerPixel(reader.getPixelType());
        int arraySize = size.sizeX * size.sizeY * bytesPerPixel;
        if (arrayBuf == null || arrayBuf.length != arraySize)
        {
            arrayBuf = new byte[arraySize];
        }

        MessageDigest md;
        
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    "Required SHA-1 message digest algorithm unavailable.");
        }
        
        FileChannel wChannel = null;
        File f;
        
        if (dumpPixels)
        {
            f = new File("pixeldump");
            boolean append = true;
            wChannel = new FileOutputStream(f, append).getChannel();   
        }
        
        for (int t = 0; t < size.sizeT; t++)
        {
            for (int c = 0; c < size.sizeC; c++)
            {
                for (int z = 0; z < size.sizeZ; z++)
                {
                    int planeNumber = reader.getIndex(z, c, t);
                    //int planeNumber = getTotalOffset(z, c, t);
                    ByteBuffer buf =
                        reader.openPlane2D(fileName, planeNumber, arrayBuf).getData();
                    arrayBuf = swapIfRequired(buf, fileName);
                    try {
                        md.update(arrayBuf);
                    } catch (Exception e) {
                        // This better not happen. :)
                        throw new RuntimeException(e);
                    }
                    
                    notifyObservers(new ImportEvent.IMPORT_STEP(i, series, reader.getSeriesCount()));
                    
                    store.setPlane(pixId, arrayBuf, z, c, t);
                    if (dumpPixels)
                        wChannel.write(buf);
                    i++;
                }
            }
        }
        
        if (dumpPixels)
            wChannel.close();
        return md;
    }
    
    // ~ Helpers
    // =========================================================================
    
    /**
     * Return true if the data is in little-endian format. 
     * @throws IOException 
     * @throws FormatException */
    private boolean isLittleEndian(String fileName) throws FormatException, IOException {
      return reader.isLittleEndian();
    }
    
    /**
     * Examines a byte array to see if it needs to be byte swapped and modifies
     * the byte array directly.
     * @param byteArray The byte array to check and modify if required.
     * @return the <i>byteArray</i> either swapped or not for convenience.
     * @throws IOException if there is an error read from the file.
     * @throws FormatException if there is an error during metadata parsing.
     */
    private byte[] swapIfRequired(ByteBuffer buffer, String fileName)
    throws FormatException, IOException
  {
    int pixelType = reader.getPixelType();
    int bytesPerPixel = getBytesPerPixel(pixelType);
    
    // We've got nothing to do if the samples are only 8-bits wide.
    if (bytesPerPixel == 1) 
        return buffer.array();

    int length;
    
    if (isLittleEndian(fileName)) {
      if (bytesPerPixel == 2) { // short
        ShortBuffer buf = buffer.asShortBuffer();
        length = buffer.capacity() / 2;
        short x;
        for (int i = 0; i < length; i++) {
          x = buf.get(i);
          buf.put(i, (short) ((x << 8) | ((x >> 8) & 0xFF)));
        }
      } else if (bytesPerPixel == 4) { // int/uint/float
          IntBuffer buf = buffer.asIntBuffer();
          length = buffer.capacity() / 4;
          for (int i = 0; i < length; i++) {
            buf.put(i, DataTools.swap(buf.get(i)));
          }
      } else if (bytesPerPixel == 8) // double
      {
          LongBuffer buf = buffer.asLongBuffer();
          length = buffer.capacity() / 8;
          for (int i = 0; i < length ; i++) {
            buf.put(i, DataTools.swap(buf.get(i)));
          }
      } else {
        throw new FormatException(
          "Unsupported sample bit width: '" + bytesPerPixel + "'");
      }
    }
    // We've got a big-endian file with a big-endian byte array.
    return buffer.array();
  }


    /**
     * Retrieves how many bytes per pixel the current plane or section has.
     * @return the number of bytes per pixel.
     */
    private int getBytesPerPixel(int type) {
      switch(type) {
      case 0:
      case 1:
        return 1;  // INT8 or UINT8
      case 2:
      case 3:
        return 2;  // INT16 or UINT16
      case 4:
      case 5:
      case 6:
        return 4;  // INT32, UINT32 or FLOAT
      case 7:
        return 8;  // DOUBLE
      }
      throw new RuntimeException("Unknown type with id: '" + type + "'");
    }

    public void clear() {
        store.createRoot();
    }
}
