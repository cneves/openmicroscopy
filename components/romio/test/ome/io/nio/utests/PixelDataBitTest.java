package ome.io.nio.utests;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import ome.io.nio.PixelData;
import ome.model.enums.PixelsType;

/**
 * Integration tests of the PixelData implementation for the bit pixel type.
 * @author Chris Allan <callan at blackcat dot ca>
 */
public class PixelDataBitTest extends TestCase
{
	private PixelData data;

    public void setUp()
    {
    	PixelsType type = new PixelsType();
    	byte[] byteArray = new byte[] { (byte) 0xFF, (byte) 193 }; 
    	type.setValue("bit");
    	data = new PixelData(type, ByteBuffer.wrap(byteArray));
    }
 
    public void testGetBits()
    {
    	assertEquals(1.0, data.getPixelValue(0));  // Start byte 1
    	assertEquals(1.0, data.getPixelValue(1));
    	assertEquals(1.0, data.getPixelValue(2));
    	assertEquals(1.0, data.getPixelValue(3));
    	assertEquals(1.0, data.getPixelValue(4));
    	assertEquals(1.0, data.getPixelValue(5));
    	assertEquals(1.0, data.getPixelValue(6));
    	assertEquals(1.0, data.getPixelValue(7));
    	assertEquals(1.0, data.getPixelValue(8));  // Start byte 2 
    	assertEquals(1.0, data.getPixelValue(9));
    	assertEquals(0.0, data.getPixelValue(10));
    	assertEquals(0.0, data.getPixelValue(11));
    	assertEquals(0.0, data.getPixelValue(12));
    	assertEquals(0.0, data.getPixelValue(13));
    	assertEquals(0.0, data.getPixelValue(14));
    	assertEquals(1.0, data.getPixelValue(15));
    }
    
    public void testSetAndGetBits()
    {
    	// 0
    	assertEquals(1.0, data.getPixelValue(0));
    	data.setPixelValue(0, 0.0);
    	assertEquals(0.0, data.getPixelValue(0));
    	// 7
    	assertEquals(1.0, data.getPixelValue(7));
    	data.setPixelValue(7, 0.0);
    	assertEquals(0.0, data.getPixelValue(7));
    	// 8
    	assertEquals(1.0, data.getPixelValue(8));
    	data.setPixelValue(8, 1.0);
    	assertEquals(1.0, data.getPixelValue(8));
    	// 14
    	assertEquals(0.0, data.getPixelValue(14));
    	data.setPixelValue(14, 1.0);
    	assertEquals(1.0, data.getPixelValue(14));    	
    }
    
    public void testSize()
    {
    	assertEquals(2, data.size());
    }
}
