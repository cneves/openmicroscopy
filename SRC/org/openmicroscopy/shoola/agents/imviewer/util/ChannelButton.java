/*
 * org.openmicroscopy.shoola.util.ui.ColouredButton
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

package org.openmicroscopy.shoola.agents.imviewer.util;


//Java imports
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.util.ui.ColouredButton;

/** 
 * Customized button used to select the rendered channel.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author	Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $ $Date: $)
 * </small>
 * @since OME2.2
 */
public class ChannelButton
    extends ColouredButton 
    implements ActionListener
{
    
	/** The default size of the component. */
	public static final Dimension 	DEFAULT_MIN_SIZE = new Dimension(30, 30);
	
	
    /** Bound property indicating to bring up the info dialog. */
    public static final String  INFO_PROPERTY = "info";
    
    /** 
     * Bound property name indicating that the channel is or is not selected. 
     */
    public static final String  CHANNEL_SELECTED_PROPERTY = "channelSelected";
    
    /** 
     * Bound property name indicating that the channel is mapped to a new color. 
     */
    public static final String  CHANNEL_COLOR_PROPERTY = "channelColor";
    
    /** The index of the channel. */
    private final int               index;
    
    /** The pop up menu associated to this component. */
    private ChannelButtonPopupMenu  popupMenu;
    
    /** 
     * The number of milliseconds we'll wait till we know we've got a single
     * click event. 
     */
    //private static final int DOUBLE_CLICK_THRESHOLD = 200; // ms
   
    /** Timer scheduling the double click task. */
    //private Timer timer;  
    
    /** Fires an event to select the channel. */
    private final void setChannelSelected()
    {
        Boolean value =  Boolean.TRUE;
        if (isSelected()) value = Boolean.FALSE;
        HashMap<Integer, Boolean> map = new HashMap<Integer, Boolean>(1);
        map.put(new Integer(index), value);
        firePropertyChange(CHANNEL_SELECTED_PROPERTY, null, map);
    }
  
    /**
     * Selects the channel or displays the pop up menu.
     * 
     * @param e The mouse event to handle.
     */
    private void onClick(MouseEvent e)
    {
    	boolean mask = (e.isControlDown() || e.isMetaDown());
    	if (e.getButton() == MouseEvent.BUTTON1 && !(mask) ) {
    		//if (e.getClickCount() == 1) timer.start();
    		//else if (e.getClickCount() == 2) {
    		//	timer.stop();
    		//	doubleClick();
    		//}
    		singleClick();
    	} else if ((e.getButton() == MouseEvent.BUTTON2 || mask)) 
        	onReleased(e);
    }
   
    /**
     * Executed by the timer when the double click threshold has expired and
     * we know we have a single click.
     */
    private void singleClick()   { setChannelSelected(); }
    
    /**
     * Executed in the {@link #onClick(MouseEvent)} method when the second click 
     * event has been received before the double click threshold time has 
     * expired. 
     */
    //private void doubleClick()   { showColorPicker(); }
       
    /** 
     * Handles the mouse released event because Popup menus are triggered 
     * differently on different systems.
     * 
     * @param e The The mouse event to handle.
     */
    private void onReleased(MouseEvent e)
    {
    	if (e.isPopupTrigger()) {
            if (popupMenu == null) 
                popupMenu = new ChannelButtonPopupMenu(this);
            popupMenu.show(this, e.getX(), e.getY());
        } 
    }
    
    /**
     * Creates a new instance.
     * 
     * @param text      The text of the button. The text should correspond to
     *                  the emission wavelength, fluor used or the index.
     * @param color     The background color of the button. Corresponds to the
     *                  color associated to the channel.
     * @param index     The channel index.
     * @param selected  Pass <code>true</code> to select the channel (i.e.
     *                  the channel is rendered), <code>false</code> otherwise
     *                  (i.e. the channel is not rendered.)
     */
    public ChannelButton(String text, Color color, int index, boolean selected)
    {
        super(text, color);
        this.index = index;
        setSelected(selected);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { onClick(e); }
            public void mouseReleased(MouseEvent e) { onReleased(e); }
        });
        //timer = new Timer(DOUBLE_CLICK_THRESHOLD, this);
        //timer.setCoalesce(true);
    }
    
    /**
     * Creates a deselected button. 
     * 
     * @param text      The text of the button. The text should correspond to
     *                  the emission wavelength, fluor used or the index.
     * @param color     The background color of the button. Corresponds to the
     *                  color associated to the channel.
     * @param index     The channel index.
     */
    public ChannelButton(String text, Color color, int index)
    {
        this(text, color, index, false);
    }
    
    /** Fires a property change to bring up on screen the info dialog. */
    void showInfo()
    {
        firePropertyChange(INFO_PROPERTY, null, new Integer(index));
    }
    
    /** Fires a property change to bring up on screen the color picker. */
    void showColorPicker()
    {
        firePropertyChange(CHANNEL_COLOR_PROPERTY, null, new Integer(index));
    }
    
    /**
     * Returns the index of the channel.
     * 
     * @return See above.
     */
    public int getChannelIndex() { return index; }
    
	/**
     * Handles click performed on the channel button.
	 * @see ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
    {
		singleClick();
		//timer.stop();
	}
    
}
