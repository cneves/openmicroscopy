package ome.formats.importer.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import layout.TableLayout;
import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.IObservable;
import ome.formats.importer.IObserver;
import ome.formats.importer.ImportEvent;
import ome.formats.importer.util.ETable;
import omero.model.Dataset;
import omero.model.Screen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.swingx.JXDatePicker;


public class HistoryTable
    extends JPanel
    implements ActionListener, PropertyChangeListener, IObserver, IObservable
{
	/** Logger for this class */
	private static Log log = LogFactory.getLog(HistoryTable.class);
	
    final ArrayList<IObserver> observers = new ArrayList<IObserver>();
    
    private static final long serialVersionUID = 1L;
    public HistoryTableModel table = new HistoryTableModel();
    public ETable eTable = new ETable(table);
    
    private static final String DATE_FORMAT = "yy/MM/dd";
    
    GuiCommonElements gui;
    
 // ----- Variables -----
    // Debug Borders
    Boolean debug = false;
    
    // Size of the add/remove/refresh buttons (which are square).
    int buttonSize = 40;
    
    // width of certain columns
    int statusWidth = 100;
    int dateWidth = 180;

    // Add graphic for add button
    String searchIcon = "gfx/add.png";
    // Remove graphics for remove button
    String clearIcon = "gfx/nuvola_editdelete16.png";
    
    JPanel                  mainPanel;
    JPanel                  topSidePanel;
    JPanel                  bottomSidePanel;
    JPanel                  filterPanel;
    
    JTextPane               sideLabel;

    JLabel                  fromLabel;
    JLabel                  toLabel;
    
    JXDatePicker            fromDate;
    JXDatePicker            toDate;
    
    JTextField              searchField;
    
    JTextPane               filterLabel;
    JCheckBox               doneCheckBox;
    JCheckBox               failedCheckBox;
    JCheckBox               invalidCheckBox;
    JCheckBox               pendingCheckBox;
    
    JButton         searchBtn;
    JButton         reimportBtn;
    JButton         clearBtn;
    
    /**
     * THIS SHOULD NOT BE VISIBLE!
     */
    final HistoryDB db;
    
    private final GuiImporter viewer;
    private final HistoryTaskBar historyTaskBar = new HistoryTaskBar();

    JList todayList = new JList(historyTaskBar.today);
    JList yesterdayList = new JList(historyTaskBar.yesterday);
    JList thisWeekList = new JList(historyTaskBar.thisWeek);
    JList lastWeekList = new JList(historyTaskBar.lastWeek);
    JList thisMonthList = new JList(historyTaskBar.thisMonth);
    private boolean unknownProjectDatasetFlag;

    HistoryTable(GuiImporter viewer)
    {
        this.viewer = viewer;
        try {
            historyTaskBar.addPropertyChangeListener(this);
        } catch (Exception ex) {
        	log.error("Exception adding property change listener.", ex);
        }

        HistoryDB db = null;
        try {
            db = new HistoryDB();
            db.addObserver(this);
        } catch (Exception e) {
            db = null;
            log.error("Could not start history DB.", e);
            if (HistoryDB.alertOnce == false)
            {
                JOptionPane.showMessageDialog(null,
                    "We were not able to connect to the history DB.\n" +
                    "Make sure you do not have a second importer\n" +
                    "running and try again.\n\n" +
                    "In the meantime, you will still be able to use \n" +
                    "the importer, but the history feature will be disable.",
                    "Warning",
                    JOptionPane.ERROR_MESSAGE);
                HistoryDB.alertOnce = true;
            }
        }
        
        this.gui = viewer.gui;
        
        this.db = db;
        
        // set to layout that will maximize on resizing
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        this.setOpaque(false);
        
        // Main Panel containing all elements  
        // Set up the main panel layout
        double mainTable[][] =
                {{170, 10, TableLayout.FILL, 80}, // columns
                { 5, 30, 35, 40, TableLayout.FILL, 35, 5}}; // rows
        
        mainPanel = gui.addMainPanel(this, mainTable, 0,0,0,0, debug); 

        // *****Side Panel****
        double topSideTable[][] = 
                {{TableLayout.FILL}, // columns
                {20, 20, 20, 20}}; // rows      
        
        topSidePanel = gui.addBorderedPanel(mainPanel, topSideTable, " Date Filter ", debug);
        
        String[] dateFormats = new String[1];
        dateFormats[0] = DATE_FORMAT;
        
        fromDate = new JXDatePicker();
        fromDate.setToolTipText("Pick a from date.");
        //fromDate.getEditor().setEditable(false);
        //fromDate.setEditable(false);
        fromDate.setFormats(dateFormats);

        toDate = new JXDatePicker();
        toDate.setToolTipText("Pick a to date.");
        //toDate.getEditor().setEditable(false);
        //toDate.setEditable(false);
        toDate.setFormats(dateFormats);
        
        fromLabel = new JLabel("From (yy/mm/dd):");
        
        topSidePanel.add(fromLabel, "0,0");
        topSidePanel.add(fromDate, "0,1");

        toLabel = new JLabel("To (yy/mm/dd):");
        
        topSidePanel.add(toLabel, "0,2");
        topSidePanel.add(toDate, "0,3");
        
        double bottomSideTable[][] = 
        {{TableLayout.FILL}, // columns
        {TableLayout.FILL}}; // rows 
        
        historyTaskBar.addTaskPane( "Today", historyTaskBar.getList(todayList));
        historyTaskBar.addTaskPane( "Yesterday", historyTaskBar.getList(yesterdayList));
        historyTaskBar.addTaskPane( "This Week", historyTaskBar.getList(thisWeekList));
        historyTaskBar.addTaskPane( "Last Week", historyTaskBar.getList(lastWeekList));
        historyTaskBar.addTaskPane( "This Month", historyTaskBar.getList(thisMonthList));
        
        bottomSidePanel = gui.addBorderedPanel(mainPanel, bottomSideTable, " Quick Date ", debug);

        /*
        JPanel taskPanel = new JPanel( new BorderLayout() );
        JScrollPane taskScrollPane = new JScrollPane();
        taskScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        taskScrollPane.getViewport().add(historyTaskBar);
        taskPanel.add(taskScrollPane);
        
        bottomSidePanel.add(taskPanel, "f,f");
        taskPanel.validate();
        */
        
        bottomSidePanel.add(historyTaskBar, "f,f");  
        
        clearBtn = gui.addIconButton(mainPanel, "Wipe History", clearIcon, 
                130, 32, (int)'S', "Click here to clear your history log.", "0,5,c,c", debug);   
        
        clearBtn.setActionCommand(HistoryHandler.CLEARHISTORY);
        clearBtn.addActionListener(this);
        
        // *****Top right most row containing search field and buttons*****
        searchField = gui.addTextField(mainPanel, "Name Filter: ", "*.*", 'N', 
                "Type in a file name to search for here.", "", 
                TableLayout.PREFERRED, "2,1, 0, 0", debug);

        searchBtn = gui.addButton(mainPanel, "Search", 'S', "Click here to search", "3,1,c,c", debug);
        
        searchBtn.setActionCommand(HistoryHandler.HISTORYSEARCH);
        searchBtn.addActionListener(this);
        
        // *****Middle right row containing the filter options*****
        // Since this panel has a different layout, use a new panel for it

        // Set up the filterTable layout
        double filterTable[][] =
                {{100, 70, 70, 70, 90, TableLayout.FILL}, // columns
                { 30 }}; // rows
        
        filterPanel = gui.addPlanePanel(mainPanel, filterTable, debug);     
        filterLabel = gui.addTextPane(filterPanel, "Status Filters: ", "0,0,r,c", debug);
        
        doneCheckBox = gui.addCheckBox(filterPanel, "Done", "1,0,l,c", debug);
        failedCheckBox = gui.addCheckBox(filterPanel, "Failed", "2,0,l,c", debug);
        invalidCheckBox = gui.addCheckBox(filterPanel, "Invalid", "3,0,l,c", debug);
        pendingCheckBox = gui.addCheckBox(filterPanel, "Pending", "4,0,1,c", debug);
        
        // Default filters to 'on'
        doneCheckBox.setSelected(true);
        failedCheckBox.setSelected(true);
        invalidCheckBox.setSelected(true);
        pendingCheckBox.setSelected(true);
        
        doneCheckBox.addActionListener(this);
        failedCheckBox.addActionListener(this);
        invalidCheckBox.addActionListener(this);
        pendingCheckBox.addActionListener(this);
                
       // *****Bottom right most row containing the history table*****
        TableColumnModel cModel =  eTable.getColumnModel();
        
        // *** remove last 4 rows from display ***
        TableColumn hiddenColumn = cModel.getColumn(6);
        cModel.removeColumn(hiddenColumn);
        hiddenColumn = cModel.getColumn(5);
        cModel.removeColumn(hiddenColumn);
        hiddenColumn = cModel.getColumn(4);
        cModel.removeColumn(hiddenColumn);
        
        MyTableHeaderRenderer myHeader = new MyTableHeaderRenderer();
        
        // Create a custom header for the table
        cModel.getColumn(0).setHeaderRenderer(myHeader);
        cModel.getColumn(1).setHeaderRenderer(myHeader);
        cModel.getColumn(2).setHeaderRenderer(myHeader);  
        cModel.getColumn(3).setHeaderRenderer(myHeader); 

        cModel.getColumn(0).setCellRenderer(new LeftDotRenderer());
        cModel.getColumn(1).setCellRenderer(new TextCellCenter());
        cModel.getColumn(2).setCellRenderer(new TextCellCenter());
        cModel.getColumn(3).setCellRenderer(new TextCellCenter());   
        
        // Set the width of the status column
        TableColumn statusColumn = eTable.getColumnModel().getColumn(3);
        statusColumn.setPreferredWidth(statusWidth);
        statusColumn.setMaxWidth(statusWidth);
        statusColumn.setMinWidth(statusWidth);

        // Set the width of the status column
        TableColumn dateColumn = eTable.getColumnModel().getColumn(2);
        dateColumn.setPreferredWidth(dateWidth);
        dateColumn.setMaxWidth(dateWidth);
        dateColumn.setMinWidth(dateWidth);
                
        // Add the table to the scollpane
        JScrollPane scrollPane = new JScrollPane(eTable);

        reimportBtn = gui.addButton(filterPanel, "Reimport", 'R', "Click here to reimport these images", "5,0,r,c", debug);
        reimportBtn.setEnabled(false);
        
        reimportBtn.setActionCommand(HistoryHandler.HISTORYREIMPORT);
        reimportBtn.addActionListener(this);
        
        mainPanel.add(scrollPane, "2,3,3,5");
        mainPanel.add(bottomSidePanel, "0,4,0,0"); 
        mainPanel.add(topSidePanel, "0,0,0,3");
        mainPanel.add(filterPanel, "2,2,3,1");
        
        this.add(mainPanel);
    }
 
    private void ClearHistory()
    {
        String message = "This will delete your import history. \n" +
                "Are you sure you want to continue?";
        Object[] o = {"Yes", "No"};
        
        int result = JOptionPane.showOptionDialog(this, message, "Warning", -1,
                JOptionPane.WARNING_MESSAGE,null,o,o[1]);
        if (result == 0) //yes clicked
        {
            db.wipeUserHistory(getExperimenterID());
            updateOutlookBar();
            getFileQuery(-1, getExperimenterID(), searchField.getText(), 
                    fromDate.getDate(), toDate.getDate());
        }
    }
    
    /**
     * @param args
     * @return 
     */
    
    public void getImportQuery(long ExperimenterID)
    {   
        try {
            ResultSet rs = db.getImportResults(db, "import_table", ExperimenterID);

            Vector<Object> row = new Vector<Object>();
            
            int count = table.getRowCount();
            for (int r = count - 1; r >= 0; r--)
            {
                table.removeRow(r);
            }
           
            // the result set is a cursor into the data.  You can only
            // point to one row at a time
            // assume we are pointing to BEFORE the first row
            // rs.next() points to next row and returns true
            // or false if there is no next row, which breaks the loop
            for (; rs.next(); ) {
                row.add(rs.getObject("date"));
                row.add(rs.getObject("status"));
                table.addRow(row);
            }
            rs.close();
            db.shutdown();
        } catch (SQLException ex3) {
        	log.error("SQL exeception.", ex3);
        } catch (NullPointerException ex4) {} // results are null
    }
    
    public void getFileQuery(int importID, long experimenterID, String string, Date from, Date to)
    {   
        try {
            ResultSet rs = db.getFileResults(db, "file_table", importID, experimenterID, string, 
                    doneCheckBox.isSelected(), failedCheckBox.isSelected(), invalidCheckBox.isSelected(),
                    pendingCheckBox.isSelected(), from, to);
                       
            // the order of the rows in a cursor
            // are implementation dependent unless you use the SQL ORDER statement
            //ResultSetMetaData meta = rs.getMetaData();
            
            int count = table.getRowCount();
            for (int r = count - 1; r >= 0; r--)
            {
                table.removeRow(r);
            }
           
            // Format the current time.
            String dayString, hourString, objectName= "", projectName = "", pdsString = "";
            long oldObjectID = 0, objectID = 0, oldProjectID = 0, projectID = 0;
            
            // the result set is a cursor into the data.  You can only
            // point to one row at a time
            // assume we are pointing to BEFORE the first row
            // rs.next() points to next row and returns true
            // or false if there is no next row, which breaks the loop
            for (; rs.next() ;) {
                objectID = rs.getLong("datasetID");
                projectID = rs.getLong("projectID");
                
                if (oldObjectID != objectID)
                {
                    oldObjectID = objectID;
                    if (projectID != 0)
                    {
                        try {
                            objectName = store().getTarget(Dataset.class, rs.getLong("datasetID")).getName().getValue();
                        } catch (Exception e)
                        {
                            objectName = "unknown";
                            displayAccessError();
                        } 
                        
                        
                        if (oldProjectID != projectID)
                        {
                            oldProjectID = projectID;
                            try {
                                projectName = store().getProject(rs.getLong("projectID")).getName().getValue();
                            } catch (Exception e)
                            {
                                projectName = "unknown";
                                displayAccessError();
                            }
                        }
                        
                        pdsString = projectName + "/" + objectName;
                        
                    }
                    else
                    {
                        try {
                            objectName = store().getTarget(Screen.class, rs.getLong("datasetID")).getName().getValue();
                        } catch (Exception e)
                        {
                            objectName = "unknown";
                            displayAccessError();
                        }   
                        
                        pdsString = objectName;
                    }
                }
                
                dayString = db.day.format(rs.getObject("date"));
                hourString = db.hour.format(rs.getObject("date"));

                if (db.day.format(new Date()).equals(dayString))
                    dayString = "Today";
                
                if (db.day.format(db.getYesterday()).equals(dayString))
                {
                    dayString = "Yesterday";
                }
                
                Vector<Object> row = new Vector<Object>();
                row.add(rs.getObject("filename"));
                row.add(pdsString);
                row.add(dayString + " " + hourString);
                row.add(rs.getObject("status"));
                row.add(rs.getObject("filepath"));
                row.add(rs.getLong("datasetID"));
                row.add(rs.getLong("projectID"));
                table.addRow(row);
                table.fireTableDataChanged();
                unknownProjectDatasetFlag = false;
            }
            
            if (rs.getFetchSize() > 0)
                reimportBtn.setEnabled(true);
            else
                reimportBtn.setEnabled(false);
            
            rs.close();
            //db.shutdown();
        } catch (SQLException ex3) {
        	log.error("SQL exception.", ex3);
        } catch (NullPointerException ex4) {
        	log.error("Null pointer exception.", ex4);
        } // results are null
    }
    
    public ResultSet getCurrentResultSet()
    {
        return null;
    }
    
    
    private void displayAccessError()
    {
        if (unknownProjectDatasetFlag) return;
        
        unknownProjectDatasetFlag = true;
        JOptionPane.showMessageDialog(null,
                "We were not able to retrieve the project/dataset for\n" +
                "one or more of the imports in this history selection.\n" +
                "The most like cause is that the original project or\n" +
                "dataset was deleted.\n\n" +
                "As a result, the imported items in question cannot be\n" +
                "reimported automatically using the \"reimport\" button.\n\n" +
                "Click OK to continue.",
                "Warning",
                JOptionPane.ERROR_MESSAGE);
    }

    private void updateOutlookBar()
    {
        GregorianCalendar newCal = new GregorianCalendar( );
        int dayOfWeek = newCal.get( Calendar.DAY_OF_WEEK );
        int dayOfMonth = newCal.get( Calendar.DAY_OF_MONTH);
        
        DefaultListModel today = db.getImportListByDate(db.getDaysBefore(new Date(), 1), new Date());
        historyTaskBar.updateList(todayList, historyTaskBar.today, today);

        DefaultListModel yesterday = db.getImportListByDate(new Date(), db.getYesterday());
        historyTaskBar.updateList(yesterdayList, historyTaskBar.yesterday, yesterday);

        DefaultListModel thisWeek = db.getImportListByDate(db.getDaysBefore(new Date(), 1), db.getDaysBefore(new Date(), -(dayOfWeek)));
        historyTaskBar.updateList(thisWeekList, historyTaskBar.thisWeek, thisWeek);

        DefaultListModel lastWeek = db.getImportListByDate(db.getDaysBefore(new Date(), -(dayOfWeek)), 
                db.getDaysBefore(new Date(), -(dayOfWeek+7)));
        historyTaskBar.updateList(lastWeekList, historyTaskBar.lastWeek, lastWeek);
        
        DefaultListModel thisMonth = db.getImportListByDate(db.getDaysBefore(new Date(), 1), db.getDaysBefore(new Date(), -(dayOfMonth)));
        historyTaskBar.updateList(thisMonthList, historyTaskBar.thisMonth, thisMonth);
    }

    private void getQuickHistory(Integer importKey)
    {
       getFileQuery(importKey, getExperimenterID(), null, null, null);
    }

    public void actionPerformed(ActionEvent e)
    {
        Object src = e.getSource();
        if (src == searchBtn || src == doneCheckBox || src == failedCheckBox 
                || src == invalidCheckBox || src == pendingCheckBox)
            getFileQuery(-1, getExperimenterID(), searchField.getText(), 
                    fromDate.getDate(), toDate.getDate());
        if (src == clearBtn)
            ClearHistory();
        if (src == reimportBtn)
        {
            notifyObservers(new ImportEvent.REIMPORT());
        }
    }


    public void propertyChange(PropertyChangeEvent e)
    {
        String prop = e.getPropertyName();
        if (prop.equals("QUICK_HISTORY"))
            getQuickHistory((Integer)e.getNewValue());
        if (prop.equals("date"))
        {
            getFileQuery(-1, getExperimenterID(), searchField.getText(), 
                    fromDate.getDate(), toDate.getDate());
        }
            
    }

    private OMEROMetadataStoreClient store() {
        return viewer.loginHandler.getMetadataStore();
    }
    
    private long getExperimenterID() {
        return store().getExperimenterID();
    }
    
    public void update(IObservable importLibrary, ImportEvent event)
    {
        long experimenterID = getExperimenterID();
        if (experimenterID != -1 && event instanceof ImportEvent.LOGGED_IN
                || event instanceof ImportEvent.QUICKBAR_UPDATE)
            {
                updateOutlookBar();
            }
    }

    // Observable methods


    public boolean addObserver(IObserver object)
    {
        return observers.add(object);
    }
    
    public boolean deleteObserver(IObserver object)
    {
        return observers.remove(object);
        
    }

    public void notifyObservers(ImportEvent event)
    {
        for (IObserver observer:observers)
        {
            observer.update(this, event);
        }
    }

    class HistoryTableModel 
        extends DefaultTableModel 
        implements TableModelListener {
        
        private static final long serialVersionUID = 1L;
        private String[] columnNames = {"File Name", "Project/Dataset or Screen", "Import Date/Time", "Status", "FilePath", "DatasetID", "ProjectID"};
    
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
            setToolTipText((String)value);
            
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

    class LeftDotRenderer 
        extends DefaultTableCellRenderer
    {
        private static final long serialVersionUID = 1L;
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
            /*if (table.getValueAt(row, 2).equals("done"))
            { this.setEnabled(false);} 
            else
            { this.setEnabled(true); }
            */
            return this;
        }
    }

    public class TextCellCenter
        extends DefaultTableCellRenderer 
    {
        // This method is called each time a column header
        // using this renderer needs to be rendered.
    
        private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
    
            super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
    
            setFont(UIManager.getFont("TableCell.font"));
            setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
            // Set tool tip if desired
            //setToolTipText((String)value);
            
            /*if (table.getValueAt(row, 2).equals("done") || 
                    table.getValueAt(row, 2).equals("failed"))
            { this.setEnabled(false); } 
            else
            { this.setEnabled(true); }
            */
            // Since the renderer is a component, return itself
            return this;
        }
    }   
    
}
