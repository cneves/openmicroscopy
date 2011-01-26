/*
 * org.openmicroscopy.shoola.agents.treeviewer.actions.PasteAction
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

package org.openmicroscopy.shoola.agents.treeviewer.actions;

//Java imports
import java.awt.event.ActionEvent;
import javax.swing.Action;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.treeviewer.IconManager;
import org.openmicroscopy.shoola.agents.treeviewer.browser.Browser;
import org.openmicroscopy.shoola.agents.treeviewer.browser.TreeImageDisplay;
import org.openmicroscopy.shoola.agents.treeviewer.cmd.PasteCmd;
import org.openmicroscopy.shoola.agents.treeviewer.view.TreeViewer;
import org.openmicroscopy.shoola.util.ui.UIUtilities;
import pojos.DatasetData;
import pojos.ImageData;
import pojos.PlateData;
import pojos.ProjectData;
import pojos.ScreenData;
import pojos.TagAnnotationData;

/** 
 * Action to paste a previously copied element, a {@link PasteCmd} is executed.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */
public class PasteAction
    extends TreeViewerAction
{

    /** Name of the action. */
    private static final String NAME = "Paste";
    
    /** Description of the action. */
    private static final String DESCRIPTION = "Paste the selected elements.";
    
    /** 
     * Sets the action enabled depending on the state of the {@link Browser}.
     * @see TreeViewerAction#onBrowserStateChange(Browser)
     */
    protected void onBrowserStateChange(Browser browser)
    {
        if (browser == null) return;
        switch (browser.getState()) {
            case Browser.LOADING_DATA:
            case Browser.LOADING_LEAVES:
            case Browser.COUNTING_ITEMS:  
                setEnabled(false);
                break;
            default:
                onDisplayChange(browser.getLastSelectedDisplay());
                break;
        }
    }
    
    /**
     * Sets the action enabled depending on the selected type.
     * @see TreeViewerAction#onDisplayChange(TreeImageDisplay)
     */
    protected void onDisplayChange(TreeImageDisplay selectedDisplay)
    {
        if (selectedDisplay == null) {
            setEnabled(false);
            return;
        }
        Class klass = model.hasDataToCopy();
        if (klass == null) {
        	setEnabled(false);
            return;
        }
        Object ho = selectedDisplay.getUserObject(); 
        if (ho instanceof ProjectData) {
        	if (DatasetData.class.equals(klass))
        		setEnabled(model.isObjectWritable(ho));
        	else setEnabled(false);
        } else if (ho instanceof ScreenData) {
        	if (PlateData.class.equals(klass))
        		setEnabled(model.isObjectWritable(ho));
        	else setEnabled(false);
        } else if (ho instanceof DatasetData) {
        	if (ImageData.class.equals(klass))
        		setEnabled(model.isObjectWritable(ho));
        	else setEnabled(false);
        } else if (ho instanceof TagAnnotationData) {
        	if (TagAnnotationData.class.equals(klass)) {
        		TagAnnotationData tag = (TagAnnotationData) ho;
        		if (TagAnnotationData.INSIGHT_TAGSET_NS.equals(
        				tag.getNameSpace())) {
        			setEnabled(model.isObjectWritable(ho));
        		}
        	} else setEnabled(false);
        } else setEnabled(false);
        
        /*
        Object ho = selectedDisplay.getUserObject(); 
        if (!(ho instanceof ImageData))
            setEnabled(model.isObjectWritable(ho));
        else setEnabled(false);
        */
    }
    
    /**
     * Creates a new instance.
     * 
     * @param model Reference to the Model. Mustn't be <code>null</code>.
     */
    public PasteAction(TreeViewer model)
    {
        super(model);
        setEnabled(false);
        name = NAME;
        putValue(Action.NAME, NAME);
        putValue(Action.SHORT_DESCRIPTION, 
                UIUtilities.formatToolTipText(DESCRIPTION));
        IconManager im = IconManager.getInstance();
        putValue(Action.SMALL_ICON, im.getIcon(IconManager.PASTE));
    }
    
    /**
     * Creates a {@link PasteCmd} command to execute the action.  
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        PasteCmd cmd = new PasteCmd(model);
        cmd.execute();
    }
    
}
