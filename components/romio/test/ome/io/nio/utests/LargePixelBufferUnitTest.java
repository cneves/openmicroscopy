/*
 *   $Id: LargePixelBufferUnitTest.java 3841 2009-02-26 13:08:12Z callan $
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.io.nio.utests;

import org.testng.annotations.*;
import ome.io.nio.DimensionsOutOfBoundsException;
import ome.io.nio.OriginalFileMetadataProvider;
import ome.io.nio.PixelBuffer;
import ome.io.nio.PixelsService;
import ome.model.core.Pixels;
import ome.model.enums.PixelsType;

import junit.framework.TestCase;

public class LargePixelBufferUnitTest extends TestCase {
    private Pixels pixels;

    private PixelBuffer pixelBuffer;

    private static final int planeSize = 512 * 512 * 2;

    private static final int stackSize = planeSize * 64;

    private static final int timepointSize = stackSize * 3;
	
	private static final String ROOT = PathUtil.getInstance().getDataFilePath();

    @Override
    @Configuration(beforeTestMethod = true)
    protected void setUp() {
    	OriginalFileMetadataProvider provider =
    		new TestingOriginalFileMetadataProvider();
        pixels = new Pixels();

        pixels.setId(1L);
        pixels.setSizeX(512);
        pixels.setSizeY(512);
        pixels.setSizeZ(64);
        pixels.setSizeC(3);
        pixels.setSizeT(50);

        PixelsType type = new PixelsType();
        type.setValue("uint16");
        pixels.setPixelsType(type);

        PixelsService service = new PixelsService(ROOT);
        pixelBuffer = service.getPixelBuffer(pixels, provider, true);
    }

    @Test
    public void testGetPlaneSize() {
        assertEquals(pixelBuffer.getPlaneSize().intValue(), planeSize);
    }

    @Test
    public void testGetStackSize() {
        assertEquals(pixelBuffer.getStackSize().intValue(), stackSize);
    }

    @Test
    public void testGetTimepointSize() {
        assertEquals(pixelBuffer.getTimepointSize().intValue(), timepointSize);
    }

    @Test
    public void testGetInitialPlaneOffset()
            throws DimensionsOutOfBoundsException {
        assertEquals(pixelBuffer.getPlaneOffset(0, 0, 0).longValue(), 0L);
    }

    @Test
    public void testGetPlaneOffset1() throws DimensionsOutOfBoundsException {
        long offset = (long) timepointSize * 25 + (long) planeSize * 25;
        assertEquals(pixelBuffer.getPlaneOffset(25, 0, 25).longValue(), offset);
    }

    @Test
    public void testGetPlaneOffset2() throws DimensionsOutOfBoundsException {
        long offset = (long) timepointSize * 25 + (long) stackSize * 1
                + (long) planeSize * 25;
        assertEquals(pixelBuffer.getPlaneOffset(25, 1, 25).longValue(), offset);
    }
}
