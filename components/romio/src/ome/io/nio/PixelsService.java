/*
 * ome.io.nio.PixelsService
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.io.nio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ome.conditions.ResourceError;
import ome.model.core.OriginalFile;
import ome.model.core.Pixels;
import ome.model.enums.PixelsType;

/**
 * @author <br>
 *         Chris Allan&nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:callan@blackcat.ca">callan@blackcat.ca</a>
 * @version 3.0 <small> (<b>Internal version:</b> $Revision: 5709 $ $Date: 2009-11-13 14:39:15 +0000 (Fri, 13 Nov 2009) $) </small>
 * @since OMERO-Beta1.0
 */
public class PixelsService extends AbstractFileSystemService {

	/** The logger for this class. */
	private transient static Log log = LogFactory.getLog(PixelsService.class);
	
	/** The DeltaVision file format enumeration value */
	public static final String DV_FORMAT = "DV";

	/** Null plane size constant. */
	public static final int NULL_PLANE_SIZE = 64;

	/** Null plane byte array. */
	public static final byte[] nullPlane = new byte[] { -128, 127, -128, 127,
			-128, 127, -128, 127, -128, 127, // 10
			-128, 127, -128, 127, -128, 127, -128, 127, -128, 127, // 20
			-128, 127, -128, 127, -128, 127, -128, 127, -128, 127, // 30
			-128, 127, -128, 127, -128, 127, -128, 127, -128, 127, // 40
			-128, 127, -128, 127, -128, 127, -128, 127, -128, 127, // 50
			-128, 127, -128, 127, -128, 127, -128, 127, -128, 127, // 60
			-128, 127, -128, 127 }; // 64
	
	/**
	 * Constructor.
	 * 
	 * @param path The root of the ROMIO proprietary pixels store. (usually
	 * <code>/OMERO/Pixels</code>).
	 */
	public PixelsService(String path) {
		super(path);
	}

	/**
	 * Creates a PixelBuffer for a given pixels set.
	 * 
	 * @param pixels Pixels set to create a pixel buffer for.
	 * @return Allocated pixel buffer ready to be used.
	 * @throws IOException If there is an I/O error creating the pixel buffer
	 * backing file.
	 */
	public PixelBuffer createPixelBuffer(Pixels pixels) throws IOException {
		RomioPixelBuffer pixbuf = new RomioPixelBuffer(getPixelsPath(pixels
				.getId()), pixels);
		initPixelBuffer(pixbuf);
		return pixbuf;
	}

	/**
	 * Returns a pixel buffer for a given set of pixels. Either a proprietary
	 * ROMIO pixel buffer or a file format specific file buffer if available.
	 * 
	 * @param pixels Pixels set to retrieve a pixel buffer for.
	 * @param provider Original file metadata provider.
	 * @param bypassOriginalFile Do not check for the existence of an original
	 * file to back this pixel buffer. 
	 * @return See above.
	 */
	public PixelBuffer getPixelBuffer(Pixels pixels,
			                          OriginalFileMetadataProvider provider,
			                          boolean bypassOriginalFile)
	{
		String pixelsFilePath = getPixelsPath(pixels.getId());
		if (!(new File(pixelsFilePath).exists()) && !bypassOriginalFile)
		{
			OriginalFile originalFile =
				provider.getOriginalFileWhereFormatStartsWith(pixels, DV_FORMAT);
			if (originalFile != null)
			{
				String originalFilePath =
					getFilesPath(originalFile.getId());
				if (new File(originalFilePath).exists())
				{
					log.info(
						"Non-existant pixel buffer file, using DeltaVision " +
						"original file: " + originalFilePath);
					return new DeltaVision(originalFilePath, originalFile);
				}
			}
		}
		log.info("Pixel buffer file exists returning ROMIO pixel buffer.");
		createSubpath(pixelsFilePath);
		return new RomioPixelBuffer(pixelsFilePath, pixels);
	}
	
	/**
	 * Initializes each plane of a PixelBuffer using a null plane byte array.
	 * 
	 * @param pixbuf Pixel buffer to initialize.
	 * @throws IOException If there is an I/O error during initialization.
	 */
	private void initPixelBuffer(RomioPixelBuffer pixbuf) throws IOException {
		String path = getPixelsPath(pixbuf.getId());
		createSubpath(path);
		byte[] padding = new byte[pixbuf.getPlaneSize() - NULL_PLANE_SIZE];
		FileOutputStream stream = new FileOutputStream(path);

		for (int z = 0; z < pixbuf.getSizeZ(); z++) {
			for (int c = 0; c < pixbuf.getSizeC(); c++) {
				for (int t = 0; t < pixbuf.getSizeT(); t++) {
					stream.write(nullPlane);
					stream.write(padding);
				}
			}
		}
	}

	/**
	 * Retrieves the bit width of a particular <code>PixelsType</code>.
	 * 
	 * @param type
	 *            a pixel type.
	 * @return width of a single pixel value in bits.
	 */
	public static int getBitDepth(PixelsType type) {
		if (type.getValue().equals("int8") || type.getValue().equals("uint8")) {
			return 8;
		} else if (type.getValue().equals("int16")
				|| type.getValue().equals("uint16")) {
			return 16;
		} else if (type.getValue().equals("int32")
				|| type.getValue().equals("uint32")
				|| type.getValue().equals("float")) {
			return 32;
		} else if (type.getValue().equals("double")) {
			return 64;
		} else if (type.getValue().equals("bit")) {
			return 1;
		}

		throw new RuntimeException("Pixels type '" + type.getValue()
				+ "' unsupported by nio.");
	}

	/**
	 * Removes files from data repository based on a parameterized List of Long
	 * pixels ids
	 * 
	 * @param pixelsIds Long file keys to be deleted
	 * @throws ResourceError If deletion fails.
	 */
	public void removePixels(List<Long> pixelIds) {
		File file;
		String fileName;
		boolean success = false;

		for (Iterator<Long> iter = pixelIds.iterator(); iter.hasNext();) {
			Long id = iter.next();

			String pixelPath = getPixelsPath(id);
			file = new File(pixelPath);
			fileName = file.getName();
			if (file.exists()) {
				success = file.delete();
				if (!success) {
					throw new ResourceError(
							"Pixels " + fileName + " deletion failed");
				} else {
					if (log.isInfoEnabled()) {
						log.info("INFO: Pixels " + fileName + " deleted.");
					}
				}
			}
		}
	}
}
