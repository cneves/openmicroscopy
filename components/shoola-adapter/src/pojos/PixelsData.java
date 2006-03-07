/*
 * pojos.PixelsData
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2004 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */

package pojos;

//Java imports

//Third-party libraries

//Application-internal dependencies
import ome.adapters.pojos.Model2PojosMapper;
import ome.model.IObject;
import ome.model.containers.CategoryGroup;
import ome.model.core.Pixels;
import ome.model.core.PixelsDimensions;
import ome.util.ModelMapper;
import ome.util.ReverseModelMapper;

/** 
 * The data that makes up an <i>OME</i> Pixels object along with a back pointer
 * to the Image that owns this Pixels.
 * A Pixels object represents a 5D raw data array that stores the Image pixels.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author  <br>Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:a.falconi@dundee.ac.uk">
 * 					a.falconi@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision: 1.1 $ $Date: 2005/05/09 19:50:41 $)
 * </small>
 * @since OME2.2
 */
public class PixelsData
    extends DataObject
{
    
    public final static String IMAGE = Pixels.IMAGE;
    
    /** 
     * Identifies the type used to store pixel values.
     * Maps onto the <i>OME</i> <code>"INT8"</code> string identifier. 
     */
    public static final int     INT8_TYPE = 0;
    
    /** 
     * Identifies the type used to store pixel values.
     * Maps onto the <i>OME</i> <code>"INT16"</code> string identifier. 
     */
    public static final int     INT16_TYPE = 1;
    
    /** 
     * Identifies the type used to store pixel values.
     * Maps onto the <i>OME</i> <code>"INT32"</code> string identifier. 
     */
    public static final int     INT32_TYPE = 2;
    
    /** 
     * Identifies the type used to store pixel values.
     * Maps onto the <i>OME</i> <code>"UINT8"</code> string identifier. 
     */
    public static final int     UINT8_TYPE = 3;
    
    /** 
     * Identifies the type used to store pixel values.
     * Maps onto the <i>OME</i> <code>"UINT16"</code> string identifier. 
     */
    public static final int     UINT16_TYPE = 4;
    
    /** 
     * Identifies the type used to store pixel values.
     * Maps onto the <i>OME</i> <code>"UINT32"</code> string identifier. 
     */
    public static final int     UINT32_TYPE = 5;
    
    /** 
     * Identifies the type used to store pixel values.
     * Maps onto the <i>OME</i> <code>"FLOAT"</code> string identifier. 
     */
    public static final int     FLOAT_TYPE = 6;
    
    /** 
     * Identifies the type used to store pixel values.
     * Maps onto the <i>OME</i> <code>"DOUBLE"</code> string identifier. 
     */
    public static final int     DOUBLE_TYPE = 7;
    
    /** The ID used by <i>OMEIS</i> to identify these Pixels. */
    // private long         imageServerID;
    
    /** The URL of the <i>OMEIS</i> instance that manages these Pixels. */
    // private String       imageServerURL;
    
    /** 
     * The X dimension of the 5D data array.
     * That is, the number of pixels along the X-axis in a 2D-plane. 
     */
    private int          sizeX;
    
    /** 
     * The Y dimension of the 5D data array.
     * That is, the number of pixels along the Y-axis in a 2D-plane. 
     */
    private int          sizeY;
    
    /** 
     * The Z dimension of the 5D data array.
     * That is, the number of focal planes in the 3D-stack. 
     */
    private int          sizeZ;
    
    /** 
     * The C dimension of the 5D data array.
     * That is, the number of wavelengths.
     */
    private int          sizeC;
    
    /** 
     * The T dimension of the 5D data array.
     * That is, the number of timepoints. 
     */ 
    private int          sizeT;
    
    /** The X-size of a pixel in microns. */
    private double       pixelSizeX;
    
    /** The Y-size of a pixel in microns. */
    private double       pixelSizeY;
    
    /** The Z-size of a pixel in microns. */
    private double       pixelSizeZ;
    
    /** One of the Pixels type identifiers defined by this class. */
    private int          pixelType;
    
    /** The Image these Pixels belong to. */
    private ImageData    image;
    
    public void copy(IObject model, ModelMapper mapper) {
		if (model instanceof Pixels) {
			Pixels pix = (Pixels) model;
            super.copy(model,mapper);
            
            // Fields
			this.setImage((ImageData)mapper.findTarget(pix.getImage()));
			PixelsDimensions dim = pix.getPixelsDimensions();
			if (dim !=null){
			    this.setPixelSizeX(mapper.nullSafeFloat(dim.getSizeX()));
			    this.setPixelSizeY(mapper.nullSafeFloat(dim.getSizeY()));
				this.setPixelSizeZ(mapper.nullSafeFloat(dim.getSizeZ())); 
			}
			if (pix.getPixelsType() != null){
			    this.setPixelType(Model2PojosMapper.getPixelTypeID(
                        pix.getPixelsType().getValue()));
            }
            this.setSizeC(mapper.nullSafeInt(pix.getSizeC()));
			this.setSizeT(mapper.nullSafeInt(pix.getSizeT()));
			this.setSizeX(mapper.nullSafeInt(pix.getSizeX()));
			this.setSizeY(mapper.nullSafeInt(pix.getSizeY()));
			this.setSizeZ(mapper.nullSafeInt(pix.getSizeZ())); 
			
		} else {
			throw new IllegalArgumentException("PixelData copies only from ImagePixel");
		}
    }

    public IObject newIObject()
    {
        return new Pixels();
    }
    
    public IObject fillIObject( IObject obj, ReverseModelMapper mapper)
    {
        if ( obj instanceof Pixels)
        {
            Pixels p = (Pixels) obj;
            if (super.fill(p)){
                // Pixels is immutable. No reason to be screwing around with it.
                p.unload(); 
            }
            return p;
        } else {
            throw new IllegalArgumentException(
                    "PixelsData can only fill Pixels.");
        }
    }
    
//	public void setImageServerID(long imageServerID) {
//		this.imageServerID = imageServerID;
//	}
//
//	public long getImageServerID() {
//		return imageServerID;
//	}

//	public void setImageServerURL(String imageServerURL) {
//		this.imageServerURL = imageServerURL;
//	}
//
//	public String getImageServerURL() {
//		return imageServerURL;
//	}

	public void setSizeX(int sizeX) {
		this.sizeX = sizeX;
	}

	public int getSizeX() {
		return sizeX;
	}

	public void setSizeY(int sizeY) {
		this.sizeY = sizeY;
	}

	public int getSizeY() {
		return sizeY;
	}

	public void setSizeZ(int sizeZ) {
		this.sizeZ = sizeZ;
	}

	public int getSizeZ() {
		return sizeZ;
	}

	public void setSizeC(int sizeC) {
		this.sizeC = sizeC;
	}

	public int getSizeC() {
		return sizeC;
	}

	public void setSizeT(int sizeT) {
		this.sizeT = sizeT;
	}

	public int getSizeT() {
		return sizeT;
	}

	public void setPixelSizeX(double pixelSizeX) {
		this.pixelSizeX = pixelSizeX;
	}

	public double getPixelSizeX() {
		return pixelSizeX;
	}

	public void setPixelSizeY(double pixelSizeY) {
		this.pixelSizeY = pixelSizeY;
	}

	public double getPixelSizeY() {
		return pixelSizeY;
	}

	public void setPixelSizeZ(double pixelSizeZ) {
		this.pixelSizeZ = pixelSizeZ;
	}

	public double getPixelSizeZ() {
		return pixelSizeZ;
	}

	public void setPixelType(int pixelType) {
		this.pixelType = pixelType;
	}

	public int getPixelType() {
		return pixelType;
	}

	public void setImage(ImageData image) {
		this.image = image;
	}

	public ImageData getImage() {
		return image;
	}
    
}
