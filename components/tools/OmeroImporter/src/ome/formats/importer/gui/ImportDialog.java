/*
 * ome.formats.importer.gui.ImportDialog
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2005 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *------------------------------------------------------------------------------
 */

package ome.formats.importer.gui;

import static omero.rtypes.rstring;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;

import layout.TableLayout;
import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.gui.GuiCommonElements.DecimalNumberField;
import ome.formats.importer.gui.GuiCommonElements.WholeNumberField;
import omero.RLong;
import omero.model.Dataset;
import omero.model.DatasetI;
import omero.model.Project;
import omero.model.ProjectI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author "Brian W. Loranger"
 */
@SuppressWarnings("serial")
public class ImportDialog extends JDialog implements ActionListener
{
    boolean debug = false;

    private GuiCommonElements       gui;

    private Integer                 dialogHeight = 360;
    private Integer                 dialogWidth = 400;

    private JTabbedPane tabbedPane;
    
    private JPanel  importPanel;
    private JPanel  pdPanel;
    private JPanel  namedPanel;
    
    private JPanel  metadataPanel;
    private JPanel  pixelPanel;
    private JPanel  channelPanel;

    // Add graphic for add button
    String addIcon = "gfx/add_text.png";

    private JRadioButton fullPathButton;
    private JRadioButton partPathButton;

    private WholeNumberField numOfDirectoriesField;
    private DecimalNumberField xPixelSize, yPixelSize, zPixelSize;
    private WholeNumberField rChannel, gChannel, bChannel;

    public JCheckBox archiveImage, fileCheckBox;

    private JButton             addProjectBtn;
    private JButton             addDatasetBtn;
    private JButton             cancelBtn;
    private JButton             importBtn;

    private JComboBox pbox;
    private JComboBox dbox;

    public  Dataset dataset;
    public  Project project;
    
    Double pixelSizeX, pixelSizeY, pixelSizeZ;
    public  int redChannel, greenChannel, blueChannel;
    
    public  ProjectI newProject;
    
    public  DatasetItem[] datasetItems = null;
    public  ProjectItem[] projectItems = null;

    public boolean    cancelled = true;
    
    private boolean ARCHIVE_ENABLED = true;

    /** Logger for this class. */
    @SuppressWarnings("unused")
    private static Log          log     = LogFactory.getLog(ImportDialog.class);

    public OMEROMetadataStoreClient store;

    ImportDialog(GuiCommonElements gui, JFrame owner, String title, boolean modal, OMEROMetadataStoreClient store)
    {
        this.store = store;

        if (store != null)
        {
            projectItems = ProjectItem.createProjectItems(store.getProjects());
            datasetItems = DatasetItem.createEmptyDataset();
        }

        setLocation(200, 200);
        setTitle(title);
        setModal(modal);
        setResizable(false);
        setSize(new Dimension(dialogWidth, dialogHeight));
        setLocationRelativeTo(owner);

        tabbedPane = new JTabbedPane();
        tabbedPane.setOpaque(false); // content panes must be opaque

        this.gui = gui;

        
        /////////////////////// START IMPORT PANEL ////////////////////////
        
        // Set up the import panel for tPane, quit, and send buttons
        
        double mainTable[][] =
            {{TableLayout.FILL, 120, 5, 160, TableLayout.FILL}, // columns
            {TableLayout.PREFERRED, 10, TableLayout.PREFERRED, 
                TableLayout.FILL, 40, 30}}; // rows

        importPanel = gui.addMainPanel(tabbedPane, mainTable, 0,10,0,10, debug);

        String message = "Import these images into which dataset?";
        gui.addTextPane(importPanel, message, "0, 0, 4, 0", debug);

        // Set up the project/dataset table
        double pdTable[][] =
        {{TableLayout.FILL, 5, 40}, // columns
                {35, 35}}; // rows

        // Panel containing the project / dataset layout

        pdPanel = gui.addMainPanel(importPanel, pdTable, 0, 0, 0, 0, debug);

        pbox = gui.addComboBox(pdPanel, "Project: ", projectItems, 'P', 
                "Select dataset to use for this import.", 60, "0,0,f,c", debug);

        // Fixing broken mac buttons.
        String offsetButtons = ",c";
        //if (gui.offsetButtons == true) offsetButtons = ",t";

        addProjectBtn = gui.addIconButton(pdPanel, "", addIcon, 20, 60, null, null, "2,0,f" + offsetButtons, debug);
        addProjectBtn.addActionListener(this);
        
        dbox = gui.addComboBox(pdPanel, "Dataset: ", datasetItems, 'D',
                "Select dataset to use for this import.", 60, "0,1,f,c", debug);

        dbox.setEnabled(false);

        addDatasetBtn = gui.addIconButton(pdPanel, "", addIcon, 20, 60, null, null, "2,1,f" + offsetButtons, debug);
        addDatasetBtn.addActionListener(this);
        
        addDatasetBtn.setEnabled(false);
        
        importPanel.add(pdPanel, "0, 2, 4, 2");

        // File naming section

        double namedTable[][] =
        {{30, TableLayout.FILL}, // columns
                {24, TableLayout.PREFERRED, 
            TableLayout.PREFERRED, TableLayout.FILL}}; // rows      

        namedPanel = gui.addBorderedPanel(importPanel, namedTable, "File Naming", debug);

        fileCheckBox = gui.addCheckBox(namedPanel, "Override default file naming. Instead use:", "0,0,1", debug);
       	fileCheckBox.setSelected(!gui.config.overrideImageName.get());
        

        String fullPathTooltip = "The full file+path name for " +
        "the file. For example: \"c:/myfolder/mysubfolder/myfile.dv\"";

        String partPathTooltip = "A partial path and file name for " +
        "the file. For example: \"mysubfolder/myfile.dv\"";

        fullPathButton = gui.addRadioButton(namedPanel, 
                "the full path+file name of your file", 'u', 
                fullPathTooltip, "1,1", debug);

        partPathButton = gui.addRadioButton(namedPanel, 
                "a partial path+file name with...", 'u', 
                partPathTooltip, "1,2", debug);

        numOfDirectoriesField = gui.addWholeNumberField(namedPanel, 
                "" , "0", "of the directories immediately before it.", 0, 
                "Add this number of directories to the file names",
                3, 40, "1,3,l,c", debug);
        
        numOfDirectoriesField.setText(Integer.toString(gui.config.numOfDirectories.get()));

        // focus on the partial path button if you enter the numofdirfield
        numOfDirectoriesField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                partPathButton.setSelected(true);
            }

            public void focusLost(FocusEvent e) {}

        });        

        ButtonGroup group = new ButtonGroup();
        group.add(fullPathButton);
        group.add(partPathButton);

        if (gui.config.useFullPath.get() == true )
            group.setSelected(fullPathButton.getModel(), true);
        else
            group.setSelected(partPathButton.getModel(), true);
        
        
        

        importPanel.add(namedPanel, "0, 3, 4, 2");

        // Buttons at the bottom of the form

        cancelBtn = gui.addButton(importPanel, "Cancel", 'L',
                "Cancel", "1, 5, f, c", debug);
        cancelBtn.addActionListener(this);

        importBtn = gui.addButton(importPanel, "Add to Queue", 'Q',
                "Import", "3, 5, f, c", debug);
        importBtn.addActionListener(this);

        this.getRootPane().setDefaultButton(importBtn);
        gui.enterPressesWhenFocused(importBtn);

        
            archiveImage = gui.addCheckBox(importPanel, 
                    "Archive the original imported file(s) to the server.", "0,4,4,t", debug);
            archiveImage.setSelected(false);
            if (ARCHIVE_ENABLED)
            {
                archiveImage.setVisible(true);
            } else {
                archiveImage.setVisible(false);                
            }

        /////////////////////// START METADATA PANEL ////////////////////////
        
        double metadataTable[][] =
        {{TableLayout.FILL}, // columns
        {TableLayout.FILL, 10, TableLayout.FILL}}; // rows
        
        metadataPanel = gui.addMainPanel(tabbedPane, metadataTable, 0,10,0,10, debug);
        
        double pixelTable[][] =
            {{10,TableLayout.FILL, 10,TableLayout.FILL, 10, TableLayout.FILL,10}, // columns
             {68, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.FILL}}; // rows      

        pixelPanel = gui.addBorderedPanel(metadataPanel, pixelTable, "Pixel Size Defaults", debug);
        
        message = "These X, Y & Z pixel size values (typically measured in microns) " +
        		"will be used if no values are included in the image file metadata:";
        gui.addTextPane(pixelPanel, message, "1, 0, 6, 0", debug);
        
        xPixelSize = gui.addDecimalNumberField(pixelPanel, 
                "X: " , null, "", 0, "", 8, 80, "1,1,l,c", debug);

        yPixelSize = gui.addDecimalNumberField(pixelPanel, 
                "Y: " , null, "", 0, "", 8, 80, "3,1,l,c", debug);

        zPixelSize = gui.addDecimalNumberField(pixelPanel, 
                "Z: " , null, "", 0, "", 8, 80, "5,1,l,c", debug);
        
        metadataPanel.add(pixelPanel, "0, 0");

        double channelTable[][] =
        {{10,TableLayout.FILL, 10,TableLayout.FILL, 10, TableLayout.FILL,10}, // columns
         {68, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.FILL}}; // rows      

        channelPanel = gui.addBorderedPanel(metadataPanel, channelTable, "Channel Defaults", debug);
        
        rChannel = gui.addWholeNumberField(channelPanel, 
                "R: " , "0", "", 0, "", 8, 80, "1,1,l,c", debug);

        gChannel = gui.addWholeNumberField(channelPanel, 
                "G: " , "1", "", 0, "", 8, 80, "3,1,l,c", debug);

        bChannel = gui.addWholeNumberField(channelPanel, 
                "B: " , "2", "", 0, "", 8, 80, "5,1,l,c", debug);
        
        message = "These RGB channel wavelengths (typically measured in nanometers)" +
        		" will be used if no channel values are included in the image file metadata:";
        gui.addTextPane(channelPanel, message, "1, 0, 6, 0", debug);
        
        //metadataPanel.add(channelPanel, "0, 2");

    
        /////////////////////// START TABBED PANE ////////////////////////
        
        this.add(tabbedPane);
        tabbedPane.addTab("Import Settings", null, importPanel, "Import Settings");
        tabbedPane.addTab("Metadata Defaults", null, metadataPanel, "Metadata Defaults");
        //this.add(mainPanel);

        importBtn.setEnabled(false);
        //this.getRootPane().setDefaultButton(importBtn);

        fullPathButton.addActionListener(this);
        partPathButton.addActionListener(this);
        numOfDirectoriesField.addActionListener(this);
        cancelBtn.addActionListener(this);
        importBtn.addActionListener(this);
        pbox.addActionListener(this);
        fileCheckBox.addActionListener(this);
        buildProjectsAndDatasets();
        setVisible(true);
    }

    private void buildProjectsAndDatasets()
    {
        long savedProject = gui.config.savedProject.get();
        long savedDataset = gui.config.savedDataset.get();
        
        if (savedProject != 0 && projectItems != null) {
            for (int i = 0; i < projectItems.length; i++)
            {
                RLong pId = projectItems[i].getProject().getId();

                if (pId != null && pId.getValue() == savedProject)
                {
                    pbox.setSelectedIndex(i);

                    Project p = ((ProjectItem) pbox.getSelectedItem()).getProject();
                    datasetItems = 
                        DatasetItem.createDatasetItems(store.getDatasets(p));
                    dbox.removeAllItems();
                    if (datasetItems.length == 0 || pbox.getSelectedIndex() == 0)
                    {
                        datasetItems = 
                            DatasetItem.createEmptyDataset();
                        dbox.addItem(datasetItems[0]);
                        dbox.setEnabled(false);
                        addDatasetBtn.setEnabled(false);
                        importBtn.setEnabled(false);
                    } else {
                        for (int k = 0; k < datasetItems.length; k++ )
                        {
                            RLong dId = datasetItems[k].getDataset().getId();
                            dbox.setEnabled(true);
                            addDatasetBtn.setEnabled(true);
                            importBtn.setEnabled(true);
                            dbox.addItem(datasetItems[k]);
                            if (dId != null && dId.getValue() == savedDataset)
                            {
                                dbox.setSelectedIndex(k);
                            }                        
                        }
                    }
                }
            }
        }

    }

    private void refreshAndSetProject()
    {
        if (store != null)
        {
            //pbox.removeAllItems();
            projectItems = ProjectItem.createProjectItems(store.getProjects());            
            for (int k = 0; k < projectItems.length; k++ )
            {
                RLong pId = projectItems[k].getProject().getId();                
                if (pId != null && pId.getValue() == gui.config.savedProject.get())
                {
                    pbox.insertItemAt(projectItems[k], k);
                    pbox.setSelectedIndex(k);
                }                        
            }
            datasetItems = DatasetItem.createEmptyDataset();
            buildProjectsAndDatasets();
            addDatasetBtn.setEnabled(true);
        }
    }

    private void refreshAndSetDataset(Project p)
    {
        datasetItems = 
            DatasetItem.createDatasetItems(store.getDatasets(p));
        dbox.removeAllItems();
        for (int k = 0; k < datasetItems.length; k++ )
        {
            RLong dId = datasetItems[k].getDataset().getId();
            dbox.setEnabled(true);
            addDatasetBtn.setEnabled(true);
            importBtn.setEnabled(true);
            dbox.insertItemAt(datasetItems[k], k);
            if (dId != null && dId.getValue() == gui.config.savedDataset.get())
            {
                dbox.setSelectedIndex(k);
            }                        
        }
    }
    
    public void sendingNamingWarning(Component frame)
    {
        final JOptionPane optionPane = new JOptionPane(
                "\nNOTE: Some file formats do not include the file name in their metadata, " +
        		"\nand disabling this option may result in files being imported without a " +
        		"\nreference to their file name. For example, 'myfile.lsm [image001]' " +
        		"\nwould show up as 'image001' with this optioned turned off.", JOptionPane.WARNING_MESSAGE);
        final JDialog warningDialog = new JDialog(this, "Naming Warning!", true);
        warningDialog.setContentPane(optionPane);

        optionPane.addPropertyChangeListener(
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent e) {
                        String prop = e.getPropertyName();

                        if (warningDialog.isVisible() 
                                && (e.getSource() == optionPane)
                                && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                            warningDialog.dispose();
                        }
                    }
                });

        warningDialog.toFront();
        warningDialog.pack();
        warningDialog.setLocationRelativeTo(frame);
        warningDialog.setVisible(true);
    }

    
    public void actionPerformed(ActionEvent e)
    {
       
        if (e.getSource() == fileCheckBox && !fileCheckBox.isSelected())
        {
            sendingNamingWarning(this);   
        } 
        else if (e.getSource() == addProjectBtn)
        {
            new AddProjectDialog(gui, this, "Add a new Project", true, store);
            refreshAndSetProject();
        } 
        else if (e.getSource() == addDatasetBtn)
        {
            project = ((ProjectItem) pbox.getSelectedItem()).getProject();
            new AddDatasetDialog(gui, this, "Add a new Dataset to: " + project.getName().getValue(), true, project, store);
            refreshAndSetDataset(project);
        } 
        else if (e.getSource() == fullPathButton)
        {
            gui.config.useFullPath.set(true);

        }
        else if (e.getSource() == partPathButton)
        {
            gui.config.useFullPath.set(false);
        }
        else if (e.getSource() == cancelBtn)
        {
            cancelled = true;
            this.dispose();
        }
        else if (e.getSource() == importBtn)
        {
            cancelled = false;
            importBtn.requestFocus();
            gui.config.numOfDirectories.set(numOfDirectoriesField.getValue());
            dataset = ((DatasetItem) dbox.getSelectedItem()).getDataset();
            project = ((ProjectItem) pbox.getSelectedItem()).getProject();
            gui.config.savedProject.set(
                    ((ProjectItem) pbox.getSelectedItem()).getProject().getId().getValue());
            gui.config.savedDataset.set(dataset.getId().getValue());
            gui.config.overrideImageName.set(!fileCheckBox.isSelected());
            gui.config.savedFileNaming.set(fullPathButton.isSelected());
            
            pixelSizeX = xPixelSize.getValue();
            pixelSizeY = yPixelSize.getValue();
            pixelSizeZ = zPixelSize.getValue();
            
            redChannel = rChannel.getValue();
            greenChannel = gChannel.getValue();
            blueChannel = bChannel.getValue();
            
            this.dispose();
        }
        else if (e.getSource() == pbox)
        {
            cancelled = false;

            if (pbox.getSelectedIndex() == 0)
            {
                dbox.setEnabled(false);
                addDatasetBtn.setEnabled(false);
            } else
            {
                Project p = ((ProjectItem) pbox.getSelectedItem()).getProject();
                datasetItems = 
                    DatasetItem.createDatasetItems(store.getDatasets(p));
                addDatasetBtn.setEnabled(true);
            }

            dbox.removeAllItems();
            if (datasetItems.length == 0 || pbox.getSelectedIndex() == 0)
            {
                datasetItems = 
                    DatasetItem.createEmptyDataset();
                dbox.addItem(datasetItems[0]);
                dbox.setEnabled(false);
                importBtn.setEnabled(false);
            } else {
                for (int i = 0; i < datasetItems.length; i++ )
                {
                    dbox.setEnabled(true);
                    importBtn.setEnabled(true);
                    dbox.addItem(datasetItems[i]);
                }
            }

        }
    }

    public static void main (String[] args) {

        String laf = UIManager.getSystemLookAndFeelClassName() ;
        //laf = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
        //laf = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
        //laf = "javax.swing.plaf.metal.MetalLookAndFeel";
        //laf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";

        try {
            UIManager.setLookAndFeel(laf);
        } catch (Exception e) 
        { System.err.println(laf + " not supported."); }

        ImportDialog dialog = new ImportDialog(null, null, "Import Dialog", true, null);
        if (dialog != null) System.exit(0);
    }

    public void stateChanged(ChangeEvent e)
    {
        System.err.println("TESt");
    }
}

//Helper classes used by the dialog comboboxes
class DatasetItem
{
    private Dataset dataset;

    public DatasetItem(Dataset dataset)
    {
        this.dataset = dataset;
    }

    public Dataset getDataset()
    {
        return dataset;
    }

    @Override
    public String toString()
    {
        if (dataset == null) return "";
        return dataset.getName().getValue();
    }

    public Long getId() {
        return dataset.getId().getValue();
    }

    public static DatasetItem[] createDatasetItems(List<Dataset> datasets)
    {
        DatasetItem[] items = new DatasetItem[datasets.size()];
        for (int i = 0; i < datasets.size(); i++)
        {
            items[i] = new DatasetItem(datasets.get(i));
        }
        return items;
    }

    public static DatasetItem[] createEmptyDataset()
    {
        DatasetI d = new DatasetI();
        d.setName(rstring("--- Empty Set ---"));
        DatasetItem[] items = new DatasetItem[1];
        items[0] = new DatasetItem(d);
        return items;
    }
}

class ProjectItem
{
    private Project project;

    public ProjectItem(Project project)
    {
        this.project = project;
    }

    public Project getProject()
    {
        return project;
    }

    @Override
    public String toString()
    {
        return project.getName().getValue();
    }

    public Long getId()
    {
        return project.getId().getValue();
    }

    public static ProjectItem[] createProjectItems(List<Project> projects)
    {
        ProjectItem[] items = new ProjectItem[projects.size() + 1];
        ProjectI p = new ProjectI();
        p.setName(rstring("--- Select Project ---"));
        items[0] = new ProjectItem(p);

        for (int i = 1; i < (projects.size() + 1); i++)
        {
            items[i] = new ProjectItem(projects.get(i - 1));
        }
        return items;
    }
}