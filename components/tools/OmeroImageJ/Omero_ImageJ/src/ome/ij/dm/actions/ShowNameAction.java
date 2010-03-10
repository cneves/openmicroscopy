/*
 * ome.ij.dm.actions.ShowNameAction 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2009 University of Dundee. All rights reserved.
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
package ome.ij.dm.actions;



//Java imports
import java.awt.event.ActionEvent;
import javax.swing.Action;

//Third-party libraries

//Application-internal dependencies
import ome.ij.dm.browser.Browser;
import org.openmicroscopy.shoola.util.ui.IconManager;
import org.openmicroscopy.shoola.util.ui.UIUtilities;

/** 
 * Shows a truncated name of the images or the full path is selected.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since 3.0-Beta4
 */
public class ShowNameAction 
	extends BrowserAction
{

    /** Description of the action. */
    private static final String DESCRIPTION = "Show the full name of " +
    									"the image.";
    
    /** 
     * Reacts to {@link Browser}'s state change.
     *  @see BrowserAction#onStateChange()
     */
    protected void onStateChange()
    {
    	setEnabled(model.getState() == Browser.READY);
    }
    
    /**
     * Creates a new instance.
     * 
     * @param model Reference to the Model. Mustn't be <code>null</code>.
     */
    public ShowNameAction(Browser model)
    {
    	super(model);
    	putValue(Action.SHORT_DESCRIPTION, 
    			UIUtilities.formatToolTipText(DESCRIPTION));
    	IconManager im = IconManager.getInstance();
    	putValue(Action.SMALL_ICON, im.getIcon(IconManager.MESSED_WORDS));
    }
    
    /**
     * Shows a truncated name or the full path depending on the selection.
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    { 
        model.displaysImagesName();
    }
    
}
