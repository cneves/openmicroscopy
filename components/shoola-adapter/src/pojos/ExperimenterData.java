/*
 * pojos.Experimenter
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
import java.util.Set;

//Third-party libraries

//Application-internal dependencies
import ome.adapters.pojos.MapperBlock;
import ome.model.IObject;
import ome.model.containers.CategoryGroup;
import ome.model.meta.Experimenter;
import ome.model.meta.ExperimenterGroup;
import ome.util.ModelMapper;
import ome.util.ReverseModelMapper;

/** 
 * The data that makes up an <i>OME</i> Experimenter along with information
 * about the Group the Experimenter belongs in.
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
public class ExperimenterData
    extends DataObject
{
    
    public final static String FIRSTENAME = Experimenter.FIRSTNAME;
    public final static String MIDDLENAME = Experimenter.MIDDLENAME;
    public final static String LASTNAME = Experimenter.LASTNAME;
    public final static String EMAIL = Experimenter.EMAIL;
    public final static String OMENAME = Experimenter.OMENAME;
    public final static String INSTITUTION = Experimenter.INSTITUTION;
    public final static String GROUP_EXPERIMENTER_MAP = Experimenter.GROUPEXPERIMENTERMAP;
    
    /** The Experimenter's first name. */
    private String      firstName;
    
    /** The Experimenter's last name. */
    private String      lastName;
    
    /** The Experimenter's email. */
    private String      email;
    
    /** The Experimenter's institution. */
    private String      institution;
    
    /** The main Group this Experimenter belongs in. */
    // TODO private GroupData   group;
    
    /** The other Groups this Experimenter belongs in. */
    private Set         groups;
     
    public void copy(IObject model, ModelMapper mapper) {
    	if (model instanceof Experimenter) {
			Experimenter exp = (Experimenter) model;
            super.copy(model,mapper);
            
            // Fields
			this.setFirstName(exp.getFirstName());
			this.setLastName(exp.getLastName());
			this.setEmail(exp.getEmail());
			this.setInstitution(exp.getInstitution());
            
            // Collections
            MapperBlock block = new MapperBlock( mapper );
            setGroups( new HashSet( exp.collectFromExperimenterGroupLinks( block )));

		} else {
			throw new IllegalArgumentException(
                    "ExperimenterData can only copy from Experimenter");
		}
    }
    
    public IObject newIObject()
    {
        return new Experimenter();
    }
    
    public IObject fillIObject( IObject obj, ReverseModelMapper mapper)
    {
        if ( obj instanceof Experimenter)
        {
            Experimenter e = (Experimenter) obj;
          
            if (super.fill(e)) {
                e.setFirstName(this.getFirstName());
                e.setLastName(this.getLastName());
                e.setEmail(this.getEmail());
                e.setInstitution(this.getInstitution());
         
                if (this.getGroups() != null) {
                    for (Iterator it = this.getGroups().iterator(); it.hasNext();)
                    {
                        GroupData g = (GroupData) it.next();
                        e.linkExperimenterGroup((ExperimenterGroup) mapper.map(g));
                    }
                }
                
            }
            return e;
            
        } else {
            
            throw new IllegalArgumentException(
                    "ExperimenterData can only fill Experimenter.");
        }
    }

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getEmail() {
		return email;
	}

	public void setInstitution(String institution) {
		this.institution = institution;
	}

	public String getInstitution() {
		return institution;
	}

//	public void setGroup(GroupData group) {
//		this.group = group;
//	}
//
//	public GroupData getGroup() {
//		return group;
//	}

	public void setGroups(Set groups) {
		this.groups = groups;
	}

	public Set getGroups() {
		return groups;
	}
	
}
