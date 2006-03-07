/*
 * pojos.CategoryData
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

//Third-party libraries

//Application-internal dependencies
import ome.adapters.pojos.MapperBlock;
import ome.model.IObject;
import ome.model.containers.Category;
import ome.model.containers.CategoryGroup;
import ome.model.containers.Project;
import ome.model.core.Image;
import ome.model.internal.Details;
import ome.util.ModelMapper;
import ome.util.ReverseModelMapper;

/** 
 * The data that makes up an <i>OME</i> Category along with a back pointer
 * to the Category Group that contains this Category and all the Images that
 * have been classified under this Category.
 * Morover there's a link to the Experimenter that defined this Category &#151;
 * and hence, owns it.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author  <br>Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:a.falconi@dundee.ac.uk">
 * 					a.falconi@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision: $ $Date: $)
 * </small>
 * @since OME2.2
 */
public class CategoryData
    extends DataObject
{
    public final static String NAME = Category.NAME;
    public final static String DESCRIPTION = Category.DESCRIPTION;
    public final static String IMAGES = Category.IMAGELINKS;
    public final static String CATEGORY_GROUP_LINKS = Category.CATEGORYGROUPLINKS;

    /** 
     * The Category's name.
     * This field may not be <code>null</code>.  
     */
    private String   name;
    
    /** The Category's description. */
    private String   description;
    
    /** 
     * All the Images classified under this Category.
     * The elements of this set are {@link ImageData} objects.  If this
     * Category contains no Images, then this set will be empty &#151;
     * but never <code>null</code>. 
     */
    private Set      images;
    
    /** The Category Group this Category belongs in. */
    private CategoryGroupData group;
    
    /** 
     * The Experimenter that defined this Category Group.
     * This field may not be <code>null</code>.  
     */
    private ExperimenterData owner;
    
    public void copy(IObject model, ModelMapper mapper) {
    	if (model instanceof Category) {
			Category c = (Category) model;
			super.copy(model,mapper);

            // Details
            Details details = c.getDetails();
            if (details!=null){
                this.setOwner((ExperimenterData) mapper.findTarget(details.getOwner()));
            }
            
            // Fields
			this.setName(c.getName());
			this.setDescription(c.getDescription());
            
            // Collections
            MapperBlock block = new MapperBlock( mapper );
            setImages(new HashSet( c.collectFromImageLinks( block )));
            List cats = c.collectFromCategoryGroupLinks( block );
            this.setGroup( cats.size() > 0 ? (CategoryGroupData) cats.get(0) : null);    
            if ( cats.size() > 1 )
            {
                //  FIXME and if size > 1
            }
            
		} else {
			throw new IllegalArgumentException("CategoryData can only copy Category type.");
		}
    }

    public IObject newIObject()
    {
        return new Category();
    }
    
    public IObject fillIObject( IObject obj, ReverseModelMapper mapper)
    {
        if ( obj instanceof Category)
        {
            Category c = (Category) obj;
            
            if (super.fill(c)) {
                c.setName(this.getName());
                c.setDescription(this.getDescription());
                c.linkCategoryGroup((CategoryGroup) mapper.map(this.getGroup()));
                if (this.getImages() != null) {
                    for (Iterator it = this.getImages().iterator(); it.hasNext();)
                    {
                        ImageData i = (ImageData) it.next();
                        c.linkImage((Image) mapper.map(i));
                    }
                }
            }
            return c;
        
        } else {
            
            throw new IllegalArgumentException(
                    "CategoryData can only fill Category.");
            
        }
        
    }
    
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setImages(Set images) {
		this.images = images;
	}

	public Set getImages() {
		return images;
	}

	public void setGroup(CategoryGroupData group) {
		this.group = group;
	}

	public CategoryGroupData getGroup() {
		return group;
	}

	public void setOwner(ExperimenterData owner) {
		this.owner = owner;
	}

	public ExperimenterData getOwner() {
		return owner;
	}
    
}
