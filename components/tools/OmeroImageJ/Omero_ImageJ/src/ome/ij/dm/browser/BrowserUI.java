/*
 * ome.ij.dm.browser.BrowserUI 
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
package ome.ij.dm.browser;


//Java imports
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;


//Third-party libraries

//Application-internal dependencies
import ome.ij.dm.util.TreeCellRenderer;
import pojos.DataObject;
import pojos.DatasetData;
import pojos.ExperimenterData;
import pojos.ImageData;
import pojos.PlateData;
import pojos.ProjectData;
import pojos.ScreenData;
import pojos.TagAnnotationData;

/** 
 * The Browser's View.
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
class BrowserUI     
	extends JPanel
{
    
	/** The text of the dummy default node. */
    private static final String     LOADING_MSG = "Loading...";
	
    /** 
     * The text of the node added to a {@link TreeImageSet} node
     * containing no element.
     */
    private static final String     EMPTY_MSG = "Empty";
    
    /** The tree hosting the display. */
    private JTree           		treeDisplay;
    
    /** The tool bar hosting the controls. */
    private JToolBar				menuBar;
    
    /** The Controller. */
    private BrowserControl  		controller;
    
    /** The model. */
    private BrowserModel    		model;
    
    /** Reference to the listener. */
    private TreeExpansionListener	listener;
    
    /** Reference to the selection listener. */
    private TreeSelectionListener	selectionListener;
    
    /** The component hosting the tree. */
    private JScrollPane             scrollPane;

    /** Collections of nodes whose <code>enabled</code> flag has to be reset. */
    private Set<TreeImageDisplay>	nodesToReset;
    
    /** Button indicating if the partial name is displayed or not. */
    private JToggleButton			partialButton;
    
    /**
     * Handles the mouse pressed and released.
     * 
     * @param loc			The location of the mouse click.
     * @param popupTrigger	Pass <code>true</code> if the mouse event is the 
     * 						popup menu trigger event for the platform,
     * 						<code>false</code> otherwise.
     */
    private void handleMouseClick(Point loc, boolean popupTrigger)
    {
    	if (treeDisplay.getRowForLocation(loc.x, loc.y) == -1 && popupTrigger) {
    		//model.setClickPoint(loc);
    		//controller.showPopupMenu(TreeViewer.PARTIAL_POP_UP_MENU);
		}
    }
    
    /** Builds and lays out the UI. */
    private void buildGUI()
    {
        setLayout(new BorderLayout(0, 0));
        JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        p.setBorder(null);
        p.add(menuBar);
        p.setPreferredSize(menuBar.getPreferredSize());
        add(p, BorderLayout.NORTH);
        scrollPane = new JScrollPane(treeDisplay);
        add(scrollPane, BorderLayout.CENTER);
        treeDisplay.addMouseListener(new MouseAdapter() {
    		
        	/**
        	 * Pops up a menu if the mouse click occurs on the tree
        	 * but not on the a node composing the tree
        	 * @see MouseAdapter#mousePressed(MouseEvent)
        	 */
			public void mousePressed(MouseEvent e)
			{
				handleMouseClick(e.getPoint(), e.isPopupTrigger());
			}
		
			/**
        	 * Pops up a menu if the mouse click occurs on the tree
        	 * but not on the a node composing the tree
        	 * @see MouseAdapter#mouseReleased(MouseEvent)
        	 */
			public void mouseReleased(MouseEvent e)
			{
				handleMouseClick(e.getPoint(), e.isPopupTrigger());
			}
		});
    }
    
    /** Helper method to create the menu bar. */
    private void createMenuBar()
    {
        menuBar = new JToolBar();
        menuBar.setBorder(null);
        menuBar.setRollover(true);
        menuBar.setFloatable(false);
       
        ButtonGroup group = new ButtonGroup();
        JToggleButton b = new JToggleButton();
        group.add(b);
        b.setBorderPainted(true);
        b.setSelected(true);
        b.setAction(controller.getAction(BrowserControl.SORT));
        
        menuBar.add(b);
        b = new JToggleButton(controller.getAction(BrowserControl.SORT_DATE));
        
        b.setBorderPainted(true);
        group.add(b);
        menuBar.add(b);
       
        partialButton = new JToggleButton(
        				controller.getAction(BrowserControl.PARTIAL_NAME));
        partialButton.setBorderPainted(true);
        menuBar.add(partialButton);
        menuBar.add(new JSeparator(JSeparator.VERTICAL));
        JButton button = new JButton(
        			controller.getAction(BrowserControl.COLLAPSE));
        button.setBorderPainted(false);
        menuBar.add(button);
    }

    /** 
     * Reacts to node expansion event.
     * 
     * @param tee       The event to handle.
     * @param expanded 	Pass <code>true</code> is the node is expanded,
     * 					<code>false</code> otherwise.
     */
    private void onNodeNavigation(TreeExpansionEvent tee, boolean expanded)
    {
        TreeImageDisplay node = (TreeImageDisplay) 
        							tee.getPath().getLastPathComponent();
        node.setExpanded(expanded);
        controller.onNodeNavigation(node, expanded);
    }
    
    /**
     * Reacts to mouse pressed and mouse release event.
     * 
     * @param me        The event to handle.
     * @param released  Pass <code>true</code> if the method is invoked when
     *                  the mouse is released, <code>false</code> otherwise.
     */
    private void onClick(MouseEvent me, boolean released)
    {
        Point p = me.getPoint();
        int row = treeDisplay.getRowForLocation(p.x, p.y);
        
        if (row != -1) {
            if (me.getClickCount() == 2 && released) {
            	//controller.cancel();
                //model.viewDataObject();
            	TreeImageDisplay d  = model.getLastSelectedDisplay();
                if (d == null) return;
                Object o = d.getUserObject();
                if (o instanceof ImageData) {
                	controller.viewImage(d);
                } 
            }
        }
    }

    /**
     * Creates an experimenter node hosting the passed experimenter.
     * 
     * @param exp	The experimenter to add.
     * @return See above.
     */
    private TreeImageSet createExperimenterNode(ExperimenterData exp)
    {
    	DefaultTreeModel tm = (DefaultTreeModel) treeDisplay.getModel();
    	TreeImageSet node = new TreeImageSet(exp);
    	buildEmptyNode(node);
    	TreeImageDisplay root = getTreeRoot();
    	root.addChildDisplay(node);
    	tm.insertNodeInto(node, root, root.getChildCount());
    	return node;
    }
    
    /** 
     * Helper method to create the trees hosting the display. 
     * 
     * @param exp The logged in experimenter.
     */
    private void createTrees(ExperimenterData exp)
    {
        treeDisplay = new JTree();
        treeDisplay.setVisible(true);
        treeDisplay.setRootVisible(false);
        ToolTipManager.sharedInstance().registerComponent(treeDisplay);
        treeDisplay.setCellRenderer(new TreeCellRenderer());
        treeDisplay.setShowsRootHandles(true);
        //treeDisplay.putClientProperty("JTree.lineStyle", "Angled");
        treeDisplay.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        TreeImageSet root = new TreeImageSet("");
        treeDisplay.setModel(new DefaultTreeModel(root));
        TreeImageSet node = createExperimenterNode(exp);
        treeDisplay.collapsePath(new TreePath(node.getPath()));
        //Add Listeners
        //treeDisplay.requestFocus();
        treeDisplay.addMouseListener(new MouseAdapter() {
           public void mousePressed(MouseEvent e) { onClick(e, false); }
           public void mouseReleased(MouseEvent e) { onClick(e, true); }
        });
        
        treeDisplay.addTreeExpansionListener(listener);
        selectionListener = new TreeSelectionListener() {
        
            public void valueChanged(TreeSelectionEvent e)
            {
            	TreePath[] paths = e.getPaths();
            	List<TreePath> added = new ArrayList<TreePath>();
            	for (int i = 0; i < paths.length; i++) {
            		if (e.isAddedPath(paths[i])) added.add(paths[i]);
				}
                controller.onClick(added);
            }
        };
        treeDisplay.addTreeSelectionListener(selectionListener);
        treeDisplay.addKeyListener(new KeyAdapter() {
	
			public void keyPressed(KeyEvent e)
			{
				switch (e.getKeyCode()) {
					case KeyEvent.VK_ENTER:
						TreeImageDisplay d  = model.getLastSelectedDisplay();
		                if (d == null) return;
		                Object o = d.getUserObject();
		                if (o instanceof ImageData) {
		                	controller.viewImage(d);
		                }
				}
			}
		});
    }
    
    /**
     * Adds the nodes to the specified parent.
     * 
     * @param parent    The parent node.
     * @param nodes     The list of nodes to add.
     * @param tm        The  tree model.
     */
    private void buildTreeNode(TreeImageDisplay parent, 
                                Collection nodes, DefaultTreeModel tm)
    {
        if (nodes.size() == 0) {
            tm.insertNodeInto(new DefaultMutableTreeNode(EMPTY_MSG), 
                    parent, parent.getChildCount());
            return;
        }
        Iterator i = nodes.iterator();
        TreeImageDisplay display;
        List children;
        parent.removeAllChildren();
        while (i.hasNext()) {
            display = (TreeImageDisplay) i.next();
            tm.insertNodeInto(display, parent, parent.getChildCount());
            if (display instanceof TreeImageSet) {
                children = display.getChildrenDisplay();
                if (children.size() != 0) {
                    if (display.containsImages()) {
                    	display.setExpanded(true);
                    	setExpandedParent(display, false);
                    	nodesToReset.add(display);
                    	buildTreeNode(display, model.sort(children), tm);
                        expandNode(display);
                        tm.reload(display);
                    } else {
                    	if (display.isExpanded()) {
                    		setExpandedParent(display, true);
                        	nodesToReset.add(display);
                    	}
                    	buildTreeNode(display, model.sort(children), tm);
                    }
                } else {
                	Object uo = display.getUserObject();
                	if (uo instanceof DatasetData) {
                		tm.insertNodeInto(new DefaultMutableTreeNode(EMPTY_MSG), 
                				display, display.getChildCount());
                	}
                }  
            }
        } 
        if (parent.isExpanded()) {
            expandNode(parent);
            tm.reload(parent);
        }
    }
    
    /**
     * Sets the value of the <code>expanded</code> flag for the parent of 
     * the specified node.
     * 
     * @param n	The node to handle.
     * @param b	The value to set.
     */
    private void setExpandedParent(TreeImageDisplay n, boolean b)
    {
    	TreeImageDisplay p = n.getParentDisplay();
    	if (p != null) {
    		p.setExpanded(b);
    		setExpandedParent(p, b);
    	}
    }
    
    /**
     * Adds a dummy node to the specified node.
     * 
     * @param node The parent node.
     */
    private void buildEmptyNode(DefaultMutableTreeNode node)
    {
        DefaultTreeModel tm = (DefaultTreeModel) treeDisplay.getModel();
        tm.insertNodeInto(new DefaultMutableTreeNode(EMPTY_MSG), node,
                            node.getChildCount());
    }
    
    /**
     * Expands the specified node. To avoid loop, we first need to 
     * remove the <code>TreeExpansionListener</code>.
     * 
     * @param node The node to expand.
     */
    private void expandNode(TreeImageDisplay node)
    {
        //First remove listener otherwise an event is fired.
    	node.setExpanded(true);
        treeDisplay.removeTreeExpansionListener(listener);
        treeDisplay.expandPath(new TreePath(node.getPath()));
        treeDisplay.addTreeExpansionListener(listener);
    }

    
    
    /**
     * Refreshes the passed folder node.
     * 
     * @param node		The node to refresh.
     * @param elements	The elements to add.
     */
    private void refreshFolderNode(TreeImageSet node, Set elements)
	{
		node.removeAllChildren();
		node.removeAllChildrenDisplay();
		Iterator k = elements.iterator();
		TreeImageDisplay child;
		while (k.hasNext()) {
			child = (TreeImageDisplay) k.next();
			node.addChildDisplay(child);
		}

		buildTreeNode(node, model.sort(elements), 
				(DefaultTreeModel) treeDisplay.getModel());
		node.setExpanded(true);
		expandNode(node);
	}
    
    /**
     * Organizes the sorted list so that the Project/Screen/Tag Set 
     * are displayed first.
     * 
     * @param sorted The collection to organize.
     * @return See above.
     */
    private List prepareSortedList(Collection sorted)
    {
    	List top = new ArrayList();
		List bottom = new ArrayList();
		Iterator j = sorted.iterator();
		TreeImageDisplay object;
		Object uo;
		while (j.hasNext()) {
			object = (TreeImageDisplay) j.next();
			uo = object.getUserObject();
			if ((uo instanceof ProjectData) ||
					(uo instanceof ScreenData))
				top.add(object);
			else if ((uo instanceof DatasetData) ||
					(uo instanceof PlateData))
				bottom.add(object);
			else if (uo instanceof TagAnnotationData) {
				if (TagAnnotationData.INSIGHT_TAGSET_NS.equals(
					((TagAnnotationData) uo).getNameSpace()))
					top.add(object);
				else bottom.add(object);
			}
		}
		List all = new ArrayList();
		
		if (top.size() > 0) all.addAll(top);
		if (bottom.size() > 0) all.addAll(bottom);
		return all;
    }
    
    /**
     * Creates a new instance.
     * The {@link #initialize(BrowserControl, BrowserModel) initialize} method
     * should be called straight after to link this View to the Controller.
     */
    BrowserUI()
    {
        nodesToReset = new HashSet<TreeImageDisplay>();
        listener = new TreeExpansionListener() {
            public void treeCollapsed(TreeExpansionEvent e) {
                onNodeNavigation(e, false);
            }
            public void treeExpanded(TreeExpansionEvent e) {
                onNodeNavigation(e, true);  
            }   
        };
    }
    
    /**
     * Links this View to its Controller and its Model.
     * 
     * @param controller    The Controller.
     * @param model         The Model.
     * @param exp			The experimenter the tree view is for.
     */
    void initialize(BrowserControl controller, BrowserModel model, 
    						ExperimenterData exp)
    {
    	if (controller == null)
    		throw new IllegalArgumentException("Controller cannot be null");
    	if (model == null)
    		throw new IllegalArgumentException("Model cannot be null");
        this.controller = controller;
        this.model = model;
        createMenuBar();
        createTrees(exp);
        buildGUI();
    }

    /**
     * Creates a dummy loading node whose parent is the specified node.
     * 
     * @param parent The parent node.
     */
    void loadAction(TreeImageDisplay parent)
    {
        DefaultTreeModel tm = (DefaultTreeModel) treeDisplay.getModel();
        parent.removeAllChildren();
        tm.insertNodeInto(new DefaultMutableTreeNode(LOADING_MSG), parent,
                			parent.getChildCount());
        tm.reload(parent);
    }
    
    /**
     * Returns <code>true</code> if the first child of the passed node
     * is one of the added element.
     * 
     * @param parent The node to handle.
     * @return See above.
     */
    boolean isFirstChildMessage(TreeImageDisplay parent)
    {
    	int n = parent.getChildCount();
    	if (n == 0) return true;
    	DefaultMutableTreeNode node = 
			 (DefaultMutableTreeNode) parent.getChildAt(0);
    	Object uo = node.getUserObject();
    	if (LOADING_MSG.equals(uo) || EMPTY_MSG.equals(uo))
    		return true;
    	return false;
    }
    
    /**
     * Returns the tree hosting the display.
     * 
     * @return See above.
     */
    JTree getTreeDisplay() { return treeDisplay; }
    
    /**
     * Returns the root node of the tree.
     * 
     * @return See above.
     */
    TreeImageDisplay getTreeRoot()
    {
        if (treeDisplay == null) return null;
        DefaultTreeModel dtm = (DefaultTreeModel) treeDisplay.getModel();
        if (dtm == null) return null;
        return (TreeImageDisplay) dtm.getRoot();
    }
    
    /**
     * Returns the title of the Browser according to the type.
     * 
     * @return See above.
     */
    String getBrowserTitle()
    {
        return "Projects";
    }
    
    /**
     * Selects the specified node.
     * 
     * @param node The node to select.
     */
    void selectFoundNode(TreeImageDisplay node)
    {
        TreePath path = new TreePath(node.getPath());
        treeDisplay.setSelectionPath(path);
        TreeCellRenderer renderer = (TreeCellRenderer) 
        			treeDisplay.getCellRenderer();
        //treeDisplay.requestFocus();
        renderer.getTreeCellRendererComponent(treeDisplay, node, 
                					treeDisplay.isPathSelected(path),
                					false, true, 0, false);
    }
    
    /**
     * Collapses the specified node. To avoid loop, we first need to 
     * remove the <code>TreeExpansionListener</code>.
     * 
     * @param node The node to collapse.
     */
    void collapsePath(DefaultMutableTreeNode node)
    {
        //First remove listener otherwise an event is fired.
        treeDisplay.removeTreeExpansionListener(listener);
        treeDisplay.collapsePath(new TreePath(node.getPath()));
        treeDisplay.addTreeExpansionListener(listener);
    }
    
    /** 
     * Collapses the node when an on-going data loading is cancelled.
     * 
     * @param node The node to collapse.
     */
    void cancel(DefaultMutableTreeNode node)
    {
        if (node == null) return;
        if (node.getChildCount() <= 1) {
            if (node.getUserObject() instanceof String) {
                node.removeAllChildren(); 
                buildEmptyNode(node);
            }
        }
        //in this order otherwise the node is not collapsed.
        ((DefaultTreeModel) treeDisplay.getModel()).reload(node);
        collapsePath(node);
    }
    
    /**
     * Update the specified set of nodes.
     * 
     * @param nodes The collection of nodes to update.
     * @param object The <code>DataObject</code> to update.
     */
    void updateNodes(List nodes, DataObject object)
    {
        DefaultTreeModel dtm = (DefaultTreeModel) treeDisplay.getModel();
        Iterator i = nodes.iterator(); 
        TreeImageDisplay node;
        while (i.hasNext()) {
            node = (TreeImageDisplay) i.next();
            node.setUserObject(object);
            dtm.nodeChanged(node);
        }
    }
    
    /**
     * Removes the specified set of nodes from the tree.
     * 
     * @param nodes         The collection of nodes to remove.
     * @param parentDisplay The selected parent.
     */
    void removeNodes(List nodes, TreeImageDisplay parentDisplay)
    {
        if (parentDisplay == null) parentDisplay = getTreeRoot();
        Iterator i = nodes.iterator(); 
        TreeImageDisplay node;
        TreeImageDisplay parent;
        DefaultTreeModel dtm = (DefaultTreeModel) treeDisplay.getModel();
        while (i.hasNext()) {
            node = (TreeImageDisplay) i.next();
            parent = node.getParentDisplay();
            if (parent.isChildrenLoaded()) {
                parent.removeChildDisplay(node);
                parent.remove(node);
                dtm.reload(parent);
                if (parent.equals(parentDisplay))
                    treeDisplay.setSelectionPath(
                            new TreePath(parent.getPath()));
            }
        }
    }
    
    /**
     * Adds the newly created node to the tree.
     * 
     * @param nodes         The collection of the parent nodes.
     * @param newNode       The node to add to the parent.
     * @param parentDisplay The selected parent.
     */
    void createNodes(List nodes, TreeImageDisplay newNode, 
                    TreeImageDisplay parentDisplay)
    {
        if (parentDisplay == null) parentDisplay = getTreeRoot();
        Iterator i = nodes.iterator();
        TreeImageDisplay parent;
        List list;
        Iterator j;
        DefaultTreeModel dtm = (DefaultTreeModel) treeDisplay.getModel();
        //buildEmptyNode(newNode);
        boolean toLoad = false;
        TreeImageDisplay n;
        while (i.hasNext()) {
            parent = (TreeImageDisplay) i.next();
            //problem will come when we have images
            if (parent.isChildrenLoaded()) {
                parent.addChildDisplay(newNode); 
                list = prepareSortedList(
                		model.sort(parent.getChildrenDisplay()));
                parent.removeAllChildren();
                j = list.iterator();
                while (j.hasNext()) {
                	n = (TreeImageDisplay) j.next();
                	if (!n.isChildrenLoaded()) {
                		n.removeAllChildren();
                		buildEmptyNode(n);
                	}
                	dtm.insertNodeInto(n, parent, parent.getChildCount());
                }
                dtm.reload(parent);
                expandNode(parent);
                if (parent.equals(parentDisplay))
                    treeDisplay.setSelectionPath(
                            new TreePath(newNode.getPath()));
            } else { //Only the currently selected one will be loaded.
                if (parent.equals(parentDisplay)) toLoad = true;
            }
        }
        //should be leaves. Need to review that code.
        if (toLoad) { //TO BE MODIFIED
            //if (parentDisplay.getParentDisplay() == null) //root
            //    controller.loadData();
            //else controller.loadLeaves();
        }
    }
    
    /**
     * Sorts the nodes in the tree view  according to the specified index.
     * 
     * @param type 	One out of the following constants: 
     * 				{@link  Browser#SORT_NODES_BY_DATE} or 
     * 				{@link  Browser#SORT_NODES_BY_NAME}.
     */
    void sortNodes()
    {
        DefaultTreeModel dtm = (DefaultTreeModel) treeDisplay.getModel();
        TreeImageDisplay root = (TreeImageDisplay) dtm.getRoot();
    	int n = root.getChildCount();
    	TreeImageDisplay node;
    	List children;
    	Iterator j;
    	List all;
    	for (int i = 0; i < n; i++) {
			node = (TreeImageDisplay) root.getChildAt(i);
			children = node.getChildrenDisplay();
			node.removeAllChildren();
			dtm.reload(node);
			if (children.size() != 0) {
				if (node.getUserObject() instanceof ExperimenterData) {
					all = prepareSortedList(model.sort(children));
					buildTreeNode(node, all, dtm);
				} else {
					buildTreeNode(node, model.sort(children), dtm);
				}
			} else buildEmptyNode(node);
			j = nodesToReset.iterator();
			while (j.hasNext()) {
				setExpandedParent((TreeImageDisplay) j.next(), true);
			}
		}	        	
    }
    
    /** Loads the children of the root node. */
    void loadRoot()
    {
        treeDisplay.expandPath(new TreePath(getTreeRoot().getPath()));
    }
    
    /** Loads the children of the currently logged in experimenter. */
    void loadExperimenterData()
    {
    	TreeImageDisplay root = getTreeRoot();
    	TreeImageDisplay child = (TreeImageDisplay) root.getFirstChild();
        treeDisplay.expandPath(new TreePath(child.getPath()));
    }

    /** 
     * Reacts to state change.
     * 
     * @param b Pass <code>true</code> to enable the trees, <code>false</code>
     *          otherwise.
     */
    void onStateChanged(boolean b)
    {
       //model.getParentModel().onComponentStateChange(b);
    }

    /**
     * Enables the components composing the display depending on the specified
     * parameter.
     * 
     * @param b Pass <code>true</code> to enable the component, 
     *          <code>false</code> otherwise.
     */
    void onComponentStateChange(boolean b)
    {
        treeDisplay.setEnabled(b);
    }

    /** Resets the UI so that we have no node selected in trees. */
    void setNullSelectedNode()
    {
        if (getTreeRoot() != null) {
            treeDisplay.setSelectionRow(-1);
        }
    }
    
    /** 
     * Returns <code>true</code> if the partial name is displayed, 
     * <code>false</code> otherwise.
     * 
     * @return See above.
     */
    boolean isPartialName() { return !partialButton.isSelected(); }
    
    /**
     * Removes the collection of <code>TreePath</code>s from the main tree.
     * We first need to remove the <code>TreeSelectionListener</code> to avoid 
     * loop.
     * 
     * @param paths Collection of paths to be removed.
     */
    void removeTreePaths(List paths)
    {
    	treeDisplay.removeTreeSelectionListener(selectionListener);
    	Iterator j = paths.iterator();
        while (j.hasNext()) 
        	treeDisplay.removeSelectionPath((TreePath) j.next());

        treeDisplay.addTreeSelectionListener(selectionListener);
    }

    /**
     * Adds the experimenter's data to the passed node.
     * 
     * @param nodes		The data to add.
     * @param expNode	The selected experimenter node.
     */
	void setExperimenterData(Set nodes, TreeImageDisplay expNode)
	{
		DefaultTreeModel dtm = (DefaultTreeModel) treeDisplay.getModel();
		expNode.removeAllChildren();
		expNode.removeAllChildrenDisplay();
		expNode.setChildrenLoaded(Boolean.TRUE);
		expNode.setExpanded(true);
        dtm.reload();
        if (nodes.size() != 0) {
            Iterator i = nodes.iterator();
            while (i.hasNext()) {
            	expNode.addChildDisplay((TreeImageDisplay) i.next());
            } 
            buildTreeNode(expNode, prepareSortedList(model.sort(nodes)), 
                        (DefaultTreeModel) treeDisplay.getModel());
        } else buildEmptyNode(expNode);
        Iterator j = nodesToReset.iterator();
        while (j.hasNext()) 
			setExpandedParent((TreeImageDisplay) j.next(), true);
	}
	
	/**
     * Adds the specifies nodes to the currently selected
     * {@link TreeImageDisplay}.
     * 
     * @param nodes     The collection of nodes to add.
     * @param parent    The parent of the nodes.
     */
    void setLeavesViews(Collection nodes, TreeImageSet parent)
    {
        DefaultTreeModel dtm = (DefaultTreeModel) treeDisplay.getModel();
        parent.removeAllChildren();
        parent.setChildrenLoaded(Boolean.TRUE);
        if (nodes.size() != 0) {
            Iterator i = nodes.iterator();
            while (i.hasNext())
                parent.addChildDisplay((TreeImageDisplay) i.next()) ;
            buildTreeNode(parent, model.sort(nodes), dtm);
        } else buildEmptyNode(parent);
        dtm.reload(parent);
        if (!isPartialName()) {
    		//model.component.accept(new PartialNameVisitor(isPartialName()), 
    		//		TreeImageDisplayVisitor.TREEIMAGE_NODE_ONLY);
        }
    }

	/**
	 * Removes the specified experimenter from the tree.
	 * 
	 * @param exp The experimenter data to remove.
	 */
	void removeExperimenter(ExperimenterData exp)
	{
		TreeImageDisplay root = getTreeRoot();
		List<TreeImageDisplay> nodesToKeep = new ArrayList<TreeImageDisplay>();
		TreeImageDisplay element, node = null;
		Object ho;
		ExperimenterData expElement;
		for (int i = 0; i < root.getChildCount(); i++) {
			element = (TreeImageDisplay) root.getChildAt(i);
			ho = element.getUserObject();
			if (ho instanceof ExperimenterData) {
				expElement = (ExperimenterData) ho;
				if (expElement.getId() == exp.getId())
					node = element;
				else nodesToKeep.add(element);
			}
		}
		if (node != null) root.removeChildDisplay(node);
		Iterator i = nodesToKeep.iterator();
		DefaultTreeModel tm = (DefaultTreeModel) treeDisplay.getModel();
		root.removeAllChildren();
		while (i.hasNext()) {
			tm.insertNodeInto((TreeImageSet) i.next(), root, 
							root.getChildCount());
		}
		tm.reload();
	}

	/**
	 * Returns the node hosting the logged in user.
	 * 
	 * @return See above.
	 */
	TreeImageDisplay getLoggedExperimenterNode()
	{
		TreeImageDisplay root = getTreeRoot();
		return (TreeImageDisplay) root.getChildAt(0);
	}

	/**
	 * Sets the nodes selecting via other views.
	 * 
	 * @param newSelection	The collection of nodes to select.
	 */
	void setFoundNode(TreeImageDisplay[] newSelection)
	{
		treeDisplay.removeTreeSelectionListener(selectionListener);
		treeDisplay.clearSelection();
		if (newSelection != null) {
			TreePath[] paths = new TreePath[newSelection.length];
			for (int i = 0; i < newSelection.length; i++) 
				paths[i] = new TreePath(newSelection[i].getPath());

			treeDisplay.setSelectionPaths(paths);
		}
		
		treeDisplay.repaint();
		treeDisplay.addTreeSelectionListener(selectionListener);
	}
    
}
