/*
 * org.openmicroscopy.shoola.env.ui.UserNotifierDialog
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

package org.openmicroscopy.shoola.env.ui;



//Java imports
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

//Third-party libraries

//Application-internal dependencies



/** 
 * Brings a dialog to tell the user about something that has happened.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author  <br>Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:a.falconi@dundee.ac.uk">
 * 					a.falconi@dundee.ac.uk</a>
 * @version 2.2 
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */

public class UserNotifierDialog
	extends JDialog
	implements ActionListener
{
	/** Icons ID. */
	static final int				INFORMATION_MESSAGE = 0;
	static final int				WARNING_MESSAGE = 1;
	static final int				ERROR_MESSAGE = 2;
	
	/** The width of the dialog window. */
	private static final int		WIN_W = 150;  
	
	/** The height of the dialog window. */
	private static final int		WIN_H = 150;
	
	/** Summary's Detail to display if requested. */
	private String 					detail;
	
	/** Value used to hide or display the message. */
	private boolean					isShown;
	
	/** 
	 * The image icons needed to build the GUI.
	 * The first icon is the information, the second is the warning, the third
	 * is the error.
	*/
	private Icon[]  				images;
	
	/** Contents the component to display in the JDialog. */
	private Container				contentPane;
	
	/** Details's button. */
	private JButton					button;
	
	/**
	 * Creates a new instance of {@link UserNotifierDialog}.
	 * 
	 * @param frame			parentComponent, reference to the topFrame.
	 * @param title			dialog's window title.
	 * @param summary		summary to display.
	 * @param iconID		icon ID.
	 */
	public UserNotifierDialog(JFrame frame, String title, String summary, 
								int iconID)
	{
		super(frame, title, true);
		isShown = false;
		loadImages();
		setResizable(false);
		contentPane = super.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS)); 	
		buildGUI(summary, iconID);	
		pack();
		setVisible(true);
	}
	
	/**
	 * Creates a new instance of {@link UserNotifierDialog}.
	 * 
	 * @param frame			parentComponent, reference to the topFrame.
	 * @param title			dialog's window title.
	 * @param summary		summary to display.
	 * @param message		complement of informations.
	 * @param iconID		icon ID.
	 */
	public UserNotifierDialog(JFrame frame, String title, String summary, 
								String detail, int iconID)
	{
		super(frame, title, true);
		this.detail = detail;
		isShown = false;
		loadImages();
		setResizable(false);
		contentPane = super.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS)); 	
		buildGUIWithButton(summary, iconID);
		pack();
		setVisible(true);
	}
	
	/** Handles event fired by the JButton. */
	 public void actionPerformed(ActionEvent e)
	 {
		try {
			if (isShown) hideError();
			else displayError();
		} catch(NumberFormatException nfe) {
				throw nfe;  //just to be on the safe side...
		}    
	 }
	 
	 /** Hides the error's details. */
	 private void hideError()
	 {
		button.setText("Details >>");
		isShown = false;
		Component[] list = contentPane.getComponents();
		Component c = null;
		for (int i = 0; i < list.length; i++) {
			if (list[i] instanceof JScrollPane) c = list[i];
		}
		contentPane.remove(c);
		pack();	
	 }
	 
	/** Shows the summary's details. */
	 private void displayError()
	 {
		button.setText("<< Details");
		isShown = true;
		JTextArea txtArea = new JTextArea(detail);
		txtArea.setEditable(false);
		txtArea.setLineWrap(true);
		txtArea.setWrapStyleWord(true);
		JScrollPane scrollPane  = new JScrollPane(txtArea);
		scrollPane.setPreferredSize(new Dimension(WIN_W, WIN_H));
		contentPane.add(scrollPane);
		pack(); 
	 }
	 
	/**
	 * Fills up the <code>images</code> array.
	 */
	private void loadImages()
	{
		//TODO: upload icon from registry
		images = new Icon[3];
		//Paths relative to UIFactory.
		images[INFORMATION_MESSAGE] = UIFactory.createIcon("graphx/OME16.png");
		images[WARNING_MESSAGE] = UIFactory.createIcon("graphx/OME16.png");
		images[ERROR_MESSAGE] = UIFactory.createIcon("graphx/OME16.png");
		//TODO: handle nulls (image not loaded).
	}
	
	/**
	 * Buils a basic GUI.
	 * 
	 * @param summary		summary of information/ warning.
	 * @param iconID		iconID.
	 */
	private void buildGUI(String summary, int iconID)
	{
		JPanel content = new JPanel(), iconPanel = new JPanel();
		JTextArea  label = new JTextArea(summary);
		label.setLineWrap(true);
		label.setWrapStyleWord(true);
		label.setBorder(null);
		label.setEditable(false);
		label.setOpaque(false);
		iconPanel.add(new JLabel(images[iconID]));
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		content.setLayout(gridbag);
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		gridbag.setConstraints(iconPanel, c); 
		content.add(iconPanel);
		c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, c);
		content.add(label);
		Dimension d = new Dimension(WIN_W, WIN_H);
		content.setPreferredSize(d);
		content.setSize(d);
		contentPane.add(content);
	}
	
	/**
	 * Builds a GUI with option to display the details of the error message.
	 * 
	 * @param summary		summary of the error message.
	 * @param iconID		iconID.
	 */
	private void buildGUIWithButton(String summary, int iconID)
	{
		JPanel content = new JPanel(), iconPanel = new JPanel();
		JTextArea  label = new JTextArea(summary);
		label.setLineWrap(true);
		label.setWrapStyleWord(true);
		label.setBorder(null);
		label.setEditable(false);
		label.setOpaque(false);
		iconPanel.add(new JLabel((ImageIcon) images[iconID]));
		button = new JButton("Details >>");
		button.addActionListener(this);
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		content.setLayout(gridbag);
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		gridbag.setConstraints(iconPanel, c); 
		content.add(iconPanel);
		c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, c);
		content.add(label); 
		c.insets = new Insets(10,0,0,0);  //top padding
		c.gridy = 1;
		gridbag.setConstraints(button, c); 
		content.add(button);
		Dimension d = new Dimension(WIN_W, WIN_H);
		content.setPreferredSize(d);
		content.setSize(d);
		contentPane.add(content);
	}
	
}