/*
 *   $Id$
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.formats.importer.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import ome.formats.importer.Main;
import ome.formats.importer.IObservable;
import ome.formats.importer.IObserver;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportContainer;
import ome.formats.importer.ImportEvent;
import ome.formats.importer.util.ETable;
import omero.model.IObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileQueueTable 
    extends JPanel
    implements ActionListener, IObserver
{
	/** Logger for this class */
	private Log log = LogFactory.getLog(FileQueueTable.class);
	
    public QueueTableModel table = new QueueTableModel();
    public ETable queue = new ETable(table);
    
    private static final long serialVersionUID = -4239932269937114120L;


    JButton         refreshBtn;
    JButton         addBtn;
    JButton         removeBtn;
    JButton         importBtn;
    JButton         clearDoneBtn;
    JButton         clearFailedBtn;
    
    private int row;
    private int maxPlanes;
    public boolean cancel = false;
    public boolean abort = false;
    public boolean importing = false;
    public boolean failedFiles;
    public boolean doneFiles;
    
    private MyTableHeaderRenderer headerCellRenderer;
    private LeftDotRenderer fileCellRenderer;
    private CenterTextRenderer dpCellRenderer;
    private CenterTextRenderer statusCellRenderer;
    
    FileQueueTable(ImportConfig config) {
            
// ----- Variables -----
        // Debug Borders
        Boolean debugBorders = false;
        
        // Size of the add/remove/refresh buttons (which are square).
        int buttonSize = 40;
        // Add graphic for the refresh button
        //String refreshIcon = "gfx/recycled.png";
        // Add graphic for add button
        String addIcon = "gfx/add.png";
        // Remove graphics for remove button
        String removeIcon = "gfx/remove.png";
        
        // Width of the status columns
        int statusWidth = 100;

// ----- GUI Layout Elements -----
        
        GuiCommonElements gui = new GuiCommonElements(config);
        
        // Start layout here
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setBorder(BorderFactory.createEmptyBorder(6,5,9,8));
        
        JPanel buttonPanel = new JPanel();
        if (debugBorders == true) 
            buttonPanel.setBorder(BorderFactory.createLineBorder(Color.red, 1));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
        
//        refreshBtn = addButton("+", refreshIcon, null);
//        refreshBtn.setMaximumSize(new Dimension(buttonSize, buttonSize));
//        refreshBtn.setPreferredSize(new Dimension(buttonSize, buttonSize));
//        refreshBtn.setMinimumSize(new Dimension(buttonSize, buttonSize));
//        refreshBtn.setSize(new Dimension(buttonSize, buttonSize));
//        refreshBtn.setActionCommand(Actions.REFRESH);
//        refreshBtn.addActionListener(this);
        
        addBtn = addButton(">>", addIcon, null);
        addBtn.setMaximumSize(new Dimension(buttonSize, buttonSize));
        addBtn.setPreferredSize(new Dimension(buttonSize, buttonSize));
        addBtn.setMinimumSize(new Dimension(buttonSize, buttonSize));
        addBtn.setSize(new Dimension(buttonSize, buttonSize));
        addBtn.setActionCommand(FileQueueHandler.ADD);
        addBtn.addActionListener(this);
        
        removeBtn = addButton("<<", removeIcon, null);
        removeBtn.setMaximumSize(new Dimension(buttonSize, buttonSize));
        removeBtn.setPreferredSize(new Dimension(buttonSize, buttonSize));
        removeBtn.setMinimumSize(new Dimension(buttonSize, buttonSize));
        removeBtn.setSize(new Dimension(buttonSize, buttonSize));
        removeBtn.setActionCommand(FileQueueHandler.REMOVE);
        removeBtn.addActionListener(this);
        
        buttonPanel.add(Box.createRigidArea(new Dimension(0,60)));
        //buttonPanel.add(refreshBtn);
        buttonPanel.add(Box.createVerticalGlue());
        buttonPanel.add(addBtn);
        buttonPanel.add(Box.createRigidArea(new Dimension(0,5)));
        buttonPanel.add(removeBtn);
        buttonPanel.add(Box.createVerticalGlue());
        buttonPanel.add(Box.createRigidArea(new Dimension(0,60)));
        add(buttonPanel);
        add(Box.createRigidArea(new Dimension(5,0)));

        JPanel queuePanel = new JPanel();
        if (debugBorders == true)
            queuePanel.setBorder(BorderFactory.createLineBorder(Color.red, 1));
        queuePanel.setLayout(new BoxLayout(queuePanel, BoxLayout.PAGE_AXIS));
        queuePanel.add(Box.createRigidArea(new Dimension(0,10)));
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.LINE_AXIS)); 
        JLabel label = new JLabel("Import Queue:");
        labelPanel.add(label);
        labelPanel.add(Box.createHorizontalGlue());
        queuePanel.add(labelPanel);
        queuePanel.add(Box.createRigidArea(new Dimension(0,5)));
        
        TableColumnModel cModel =  queue.getColumnModel();
        
        headerCellRenderer = new MyTableHeaderRenderer();
        fileCellRenderer = new LeftDotRenderer();
        dpCellRenderer = new CenterTextRenderer();
        statusCellRenderer = new CenterTextRenderer();
              
        // Create a custom header for the table
        cModel.getColumn(0).setHeaderRenderer(headerCellRenderer);
        cModel.getColumn(1).setHeaderRenderer(headerCellRenderer);
        cModel.getColumn(2).setHeaderRenderer(headerCellRenderer);
        cModel.getColumn(0).setCellRenderer(fileCellRenderer);
        cModel.getColumn(1).setCellRenderer(dpCellRenderer);
        cModel.getColumn(2).setCellRenderer(statusCellRenderer);            
        
        // Set the width of the status column
        TableColumn statusColumn = queue.getColumnModel().getColumn(2);
        statusColumn.setPreferredWidth(statusWidth);
        statusColumn.setMaxWidth(statusWidth);
        statusColumn.setMinWidth(statusWidth);
              

        SelectionListener listener = new SelectionListener(queue);
        queue.getSelectionModel().addListSelectionListener(listener);
        //queue.getColumnModel().getSelectionModel()
        //    .addListSelectionListener(listener);
        
        // Hide 3rd to 6th columns
        TableColumnModel tcm = queue.getColumnModel();
        TableColumn projectColumn = tcm.getColumn(6);
        tcm.removeColumn(projectColumn);
        TableColumn userPixelColumn = tcm.getColumn(6);
        tcm.removeColumn(userPixelColumn);
        TableColumn userSpecifiedNameColumn = tcm.getColumn(6);
        tcm.removeColumn(userSpecifiedNameColumn);
        TableColumn datasetColumn = tcm.getColumn(3);
        tcm.removeColumn(datasetColumn);
        TableColumn pathColumn = tcm.getColumn(3);
        tcm.removeColumn(pathColumn);
        TableColumn archiveColumn = tcm.getColumn(3);
        tcm.removeColumn(archiveColumn);
        
        // Add the table to the scollpane
        JScrollPane scrollPane = new JScrollPane(queue);

        queuePanel.add(scrollPane);
        
        JPanel importPanel = new JPanel();
        importPanel.setLayout(new BoxLayout(importPanel, BoxLayout.LINE_AXIS));
        clearDoneBtn = addButton("Clear Done", null, null);
        clearFailedBtn = addButton("Clear Failed", null, null);
        importBtn = addButton("Import", null, null);
        importPanel.add(Box.createHorizontalGlue());
        importPanel.add(clearDoneBtn);
        clearDoneBtn.setEnabled(false);
        clearDoneBtn.setActionCommand(FileQueueHandler.CLEARDONE);
        clearDoneBtn.addActionListener(this);
        importPanel.add(Box.createRigidArea(new Dimension(0,5)));
        importPanel.add(clearFailedBtn);
        clearFailedBtn.setEnabled(false);
        clearFailedBtn.setActionCommand(FileQueueHandler.CLEARFAILED);
        clearFailedBtn.addActionListener(this);
        importPanel.add(Box.createRigidArea(new Dimension(0,10)));
        importPanel.add(importBtn);
        importBtn.setEnabled(false);
        importBtn.setActionCommand(FileQueueHandler.IMPORT);
        importBtn.addActionListener(this);
        gui.enterPressesWhenFocused(importBtn);
        queuePanel.add(Box.createRigidArea(new Dimension(0,5)));
        queuePanel.add(importPanel);
        add(queuePanel);
    }
    
    public void setProgressInfo(int row, int maxPlanes)
    {
        this.row = row;
        this.maxPlanes = maxPlanes;
    }
 
    public boolean setProgressPending(int row)
    {
        if (table.getValueAt(row, 2).equals("added"))
        {
            table.setValueAt("pending", row, 2); 
            return true;
        }
        return false;
            
    }
    
    public void setProgressInvalid(int row)
    {
        if (table.getValueAt(row, 2).equals("added"))
            table.setValueAt("invalid format", row, 2);    
    }
    
    public void setImportProgress(int count, int series, int step)
    {
        String text;
        if (count > 1)
            text = series + 1 + "/" + count + ": " + step + "/" + maxPlanes;
        else
            text = step + "/" + maxPlanes;
        table.setValueAt(text, row, 2);   
    }

    public void setProgressFailed(int row)
    {
     	table.setValueAt("failed", row, 2);
        failedFiles = true;
        table.fireTableDataChanged();
    }
    
    public void setProgressUnknown(int row)
    {
        table.setValueAt("unreadable", row, 2);
        failedFiles = true;
        table.fireTableDataChanged();
    }    
        
    public void setProgressPrepping(int row)
    {
        table.setValueAt("prepping", row, 2); 
    }

    public void setProgressDone(int row)
    {
        table.setValueAt("done", row, 2);
        doneFiles = true;
        table.fireTableDataChanged();
    }

    public void setProgressSaveToDb(int row)
    {
        table.setValueAt("updating db", row, 2);       
    }
    
    public void setProgressOverlays(int row)
    {
        table.setValueAt("overlays", row, 2);       
    }
    
    public void setProgressArchiving(int row)
    {
        table.setValueAt("archiving", row, 2);       
    }
    
    public void setProgressResettingDefaults(int row)
    {
        table.setValueAt("thumbnailing", row, 2);       
    }

    public void setProgressAnalyzing(int row)
    {
        table.setValueAt("analyzing", row, 2); 
    }
    
    public int getMaximum()
    {
        return maxPlanes;
    }
        
    private JButton addButton(String name, String image, String tooltip)
    {
        JButton button = null;

        if (image == null) 
        {
            button = new JButton(name);
        } else {
            java.net.URL imgURL = GuiImporter.class.getResource(image);
            if (imgURL != null)
            {
                button = new JButton(null, new ImageIcon(imgURL));
            } else {
                button = new JButton(name);
                log.warn("Couldn't find icon: " + image);
            }
        }
        return button;
    }

    /**
     * @return ImportContainer
     */
    public ImportContainer[] getFilesAndObjectTypes() {

        int num = table.getRowCount();     
        ImportContainer[] importContainer = new ImportContainer[num];

        for (int i = 0; i < num; i++)
        {
            importContainer[i] = (ImportContainer) table.getValueAt(i, 3);
        }
        return importContainer;
    }

    public void actionPerformed(ActionEvent e)
    {
        Object src = e.getSource();
        if (src == addBtn)
            firePropertyChange(FileQueueHandler.ADD, false, true);
        if (src == removeBtn)
            firePropertyChange(FileQueueHandler.REMOVE, false, true);
//        if (src == refreshBtn)
//            firePropertyChange(FileQueueHandler.REFRESH, false, true);
        if (src == clearDoneBtn)
            firePropertyChange(FileQueueHandler.CLEARDONE, false, true);
        if (src == clearFailedBtn)
            firePropertyChange(FileQueueHandler.CLEARFAILED, false, true);
        if (src == importBtn)
        {
            queue.clearSelection();
            firePropertyChange(FileQueueHandler.IMPORT, false, true);
        }
    }
    

    public void centerOnRow(int row)
    {
        queue.getSelectionModel().setSelectionInterval(row, row);
        Rectangle visibleRect = queue.getVisibleRect();
        int centerY = visibleRect.y + visibleRect.height/2;
        Rectangle cellRect = queue.getCellRect(row, 0, true);
        if (centerY < cellRect.y)
        {
            // need to scroll up
            cellRect.y = cellRect.y - visibleRect.y + centerY;
        }
        else
        {
            // need to scroll down
            cellRect.y = cellRect.y + visibleRect.y - centerY;                    
        }
        queue.scrollRectToVisible(cellRect);
    }

    class QueueTableModel 
        extends DefaultTableModel 
        implements TableModelListener {
        
        private static final long serialVersionUID = 1L;
        private String[] columnNames = {"Files in Queue", "Project/Dataset or Screen", "Status", "DatasetNum", "Path", "Archive", "ProjectNum", "UserPixels", "UserSpecifiedName"};

        public void tableChanged(TableModelEvent arg0) { }
        
        public int getColumnCount() { return columnNames.length; }

        public String getColumnName(int col) { return columnNames[col]; }
        
        public boolean isCellEditable(int row, int col) { return false; }
        
        public boolean rowSelectionAllowed() { return true; }
    }
 
    public class MyTableHeaderRenderer 
        extends DefaultTableCellRenderer 
    {
        // This method is called each time a column header
        // using this renderer needs to be rendered.

        private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

           // setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setBorder(BorderFactory.createLineBorder(new Color(0xe0e0e0)));
            setForeground(UIManager.getColor("TableHeader.foreground"));
            setBackground(UIManager.getColor("TableHeader.background"));
            setFont(UIManager.getFont("TableHeader.font"));
    
            // Configure the component with the specified value
            setFont(getFont().deriveFont(Font.BOLD));
            setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
            setText(value.toString());
            setOpaque(true);
                
            // Set tool tip if desired
            //setToolTipText((String)value);
            
            setEnabled(table == null || table.isEnabled());
                        
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    
            // Since the renderer is a component, return itself
            return this;
        }
        
        // The following methods override the defaults for performance reasons
        public void validate() {}
        public void revalidate() {}
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
    }
    
    @SuppressWarnings("serial")
    class LeftDotRenderer 
        extends DefaultTableCellRenderer
    {       
        public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column)        
        {
            super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            int availableWidth = table.getColumnModel().getColumn(column).getWidth();
            availableWidth -= table.getIntercellSpacing().getWidth();
            Insets borderInsets = getBorder().getBorderInsets((Component)this);
            availableWidth -= (borderInsets.left + borderInsets.right);
            String cellText = getText();
            FontMetrics fm = getFontMetrics( getFont() );
            // Set tool tip if desired
 
            if (fm.stringWidth(cellText) > availableWidth)
            {
                String dots = "...";
                int textWidth = fm.stringWidth( dots );
                int nChars = cellText.length() - 1;
                for (; nChars > 0; nChars--)
                {
                    textWidth += fm.charWidth(cellText.charAt(nChars));
 
                    if (textWidth > availableWidth)
                    {
                        break;
                    }
                }
 
                setText( dots + cellText.substring(nChars + 1) );
            }

            setFont(UIManager.getFont("TableCell.font"));
            
            if (queue.getValueAt(row, 2).equals("done"))
            { this.setEnabled(false); } 
            else
            { this.setEnabled(true); }

            if (queue.getValueAt(row, 2).equals("failed"))
            { setForeground(Color.red);} 
            else if (queue.getValueAt(row, 2).equals("unreadable"))
            { setForeground(queue.DARK_ORANGE);} 
            else
            { setForeground(null);}
            
            return this;
        }
    }
    
    public class CenterTextRenderer
        extends DefaultTableCellRenderer 
    {
        // This method is called each time a column header
        // using this renderer needs to be rendered.

        private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            Component comp = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            setFont(UIManager.getFont("TableCell.font"));
            setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
            
            // Set tool tip if desired
            //setToolTipText((String)value);
            
            if (queue.getValueAt(row, 2).equals("done"))
            { this.setEnabled(false); } 
            else
            { this.setEnabled(true); }

            if (queue.getValueAt(row, 2).equals("failed"))
            { setForeground(Color.red);} 
            else if (queue.getValueAt(row, 2).equals("unreadable"))
            { setForeground(queue.DARK_ORANGE);} 
            else
            { setForeground(null);}
            
            // Since the renderer is a component, return itself
            return this;
        }
    }

    public class SelectionListener 
        implements ListSelectionListener {
        JTable table;
    
        // It is necessary to keep the table since it is not possible
        // to determine the table from the event's source
        SelectionListener(JTable table) {
            this.table = table;
        }
        public void valueChanged(ListSelectionEvent e) {
            // If cell selection is enabled, both row and column change events are fired
            if (e.getSource() == table.getSelectionModel()
                  && table.getRowSelectionAllowed()) 
            {
                    dselectRows();
            } 
        }
        
        private void dselectRows()
        {
            // Column selection changed
            int rows = queue.getRowCount();

            for (int i = 0; i < rows; i++ )
            {
                try
                {
                    if (!(queue.getValueAt(i, 2).equals("added") ||
                            queue.getValueAt(i, 2).equals("pending")) 
                            && table.getSelectionModel().isSelectedIndex(i))
                    {
                        table.getSelectionModel().removeSelectionInterval(i, i);
                    }
                } catch (ArrayIndexOutOfBoundsException e)
                {
                	log.error("Error deselecting rows in table.", e);
                }
            }
        }
    }
        
    public static void main (String[] args) {

        String laf = UIManager.getSystemLookAndFeelClassName() ;
        //laf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        //laf = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
        //laf = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
        //laf = "javax.swing.plaf.metal.MetalLookAndFeel";
        
        try {
            UIManager.setLookAndFeel(laf);
        } catch (Exception e) 
        { System.err.println(laf + " not supported."); }
        
        FileQueueTable q = new FileQueueTable(null); 
        JFrame f = new JFrame();   
        f.getContentPane().add(q);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
    }

    public void update(IObservable importLibrary, ImportEvent event)
    {
        // TODO : Here we might should check for "cancel" and if so
        // raise some form of exception. This is currently being
        // done in a similar way in ImportHandler with an anonymous
        // inner class.
        
        // TODO: all these setProgress methods could take a base
        // ImportEvent class PROGRESS_EVENT and then we wouldn't
        // need to do the instanceof's here.
        if (event instanceof ImportEvent.LOADING_IMAGE) {
            ImportEvent.LOADING_IMAGE ev = (ImportEvent.LOADING_IMAGE) event;
            setProgressPrepping(ev.index);
        }
        else if (event instanceof ImportEvent.LOADED_IMAGE) {
            ImportEvent.LOADED_IMAGE ev = (ImportEvent.LOADED_IMAGE) event;
            setProgressAnalyzing(ev.index);
        }
        else if (event instanceof ImportEvent.DATASET_STORED) {
            ImportEvent.DATASET_STORED ev = (ImportEvent.DATASET_STORED) event;
            setProgressInfo(ev.index, ev.size.imageCount);
        }
        else if (event instanceof ImportEvent.IMPORT_STEP) {
            ImportEvent.IMPORT_STEP ev = (ImportEvent.IMPORT_STEP) event;
            if (ev.step <= getMaximum()) 
            {   
                setImportProgress(ev.seriesCount, ev.series, ev.step);
            }
        }
        else if (event instanceof ImportEvent.IMPORT_DONE) {
            ImportEvent.IMPORT_DONE ev = (ImportEvent.IMPORT_DONE) event;
            setProgressDone(ev.index);
        }
        else if (event instanceof ImportEvent.IMPORT_ARCHIVING) {
            ImportEvent.IMPORT_ARCHIVING ev = (ImportEvent.IMPORT_ARCHIVING) event;
            setProgressArchiving(ev.index);
        }
        else if (event instanceof ImportEvent.BEGIN_SAVE_TO_DB) {
            ImportEvent.BEGIN_SAVE_TO_DB ev = (ImportEvent.BEGIN_SAVE_TO_DB) event;
        	setProgressSaveToDb(ev.index);
        }
        else if (event instanceof ImportEvent.IMPORT_OVERLAYS) {
            ImportEvent.IMPORT_OVERLAYS ev = (ImportEvent.IMPORT_OVERLAYS) event;
        	setProgressOverlays(ev.index);
        }
        else if (event instanceof ImportEvent.IMPORT_THUMBNAILING) {
            ImportEvent.IMPORT_THUMBNAILING ev = (ImportEvent.IMPORT_THUMBNAILING) event;
        	setProgressResettingDefaults(ev.index);
        }
        else if (event instanceof ImportEvent.IMPORT_QUEUE_STARTED)
        {
            importBtn.setText("Cancel");
            importing = true;
            // addBtn.setEnabled(false);
        }
        else if (event instanceof ImportEvent.IMPORT_QUEUE_DONE)
        {
            // addBtn.setEnabled(true);
            importBtn.setText("Import");
            importBtn.setEnabled(true);
            queue.setRowSelectionAllowed(true);
            removeBtn.setEnabled(true);
            if (failedFiles == true)
                clearFailedBtn.setEnabled(true);
            if (doneFiles == true)
                clearDoneBtn.setEnabled(true);
            importing = false;
            cancel = false;
            abort = false;
        }
        
    }
    
    /**
     * Get the renderer used for rendering header cells
     * @return
     */
    public MyTableHeaderRenderer getHeaderCellRenderer()
    {
        return headerCellRenderer;
    }

    /**
     * Get the renderer used for rendering the file column cells
     * @return
     */
    public LeftDotRenderer getFileCellRenderer()
    {
        return fileCellRenderer;
    }

    /**
     * Get the renderer used for rendering the dataset/project column cells
     * @return
     */
    public CenterTextRenderer getDpCellRenderer()
    {
        return dpCellRenderer;
    }

    /**
     * Get the renderer used for rendering the status line column cells
     * @return
     */
    public CenterTextRenderer getStatusCellRenderer()
    {
        return statusCellRenderer;
    }
}
