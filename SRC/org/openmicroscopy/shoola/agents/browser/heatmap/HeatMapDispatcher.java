/*
 * org.openmicroscopy.shoola.agents.browser.heatmap.HeatMapDispatcher
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

/*------------------------------------------------------------------------------
 *
 * Written by:    Jeff Mellen <jeffm@alum.mit.edu>
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.agents.browser.heatmap;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openmicroscopy.ds.dto.Attribute;
import org.openmicroscopy.ds.dto.SemanticType;
import org.openmicroscopy.shoola.agents.browser.BrowserAgent;
import org.openmicroscopy.shoola.agents.browser.BrowserEnvironment;
import org.openmicroscopy.shoola.agents.browser.BrowserModel;
import org.openmicroscopy.shoola.agents.browser.datamodel.AttributeMap;
import org.openmicroscopy.shoola.agents.browser.datamodel.DataElementType;
import org.openmicroscopy.shoola.agents.browser.images.PaintMethod;
import org.openmicroscopy.shoola.agents.browser.images.Thumbnail;
import org.openmicroscopy.shoola.agents.browser.images.ThumbnailDataModel;
import org.openmicroscopy.shoola.env.data.DSAccessException;
import org.openmicroscopy.shoola.env.data.DSOutOfServiceException;
import org.openmicroscopy.shoola.env.data.SemanticTypesService;

/**
 * @author Jeff Mellen, <a href="mailto:jeffm@alum.mit.edu">jeffm@alum.mit.edu</a><br>
 * <b>Internal version:</b> $Revision$ $Date$
 * @version 2.2
 * @since OME2.2
 */
public class HeatMapDispatcher implements HeatMapTreeListener,
                                          HeatMapModeListener
{
    private HeatMapMode currentMode;
    private Scale currentScale;
    private String currentScaleName;
    private HeatMapModel model;
    private HeatMapStatus status;
    private HeatMapGradient gradient;
    private String attributeName = "";
    private String elementName = "";
    
    // current method applied to every thumbnnail.
    private PaintMethod currentMethod = null;
    private PaintMethod currentValueMethod = null;
    
    public HeatMapDispatcher(HeatMapModel model)
    {
        this.model = model;    
    }
    
    public HeatMapDispatcher(HeatMapModel model, HeatMapStatus status,
                             HeatMapGradient gradient)
    {
        this.model = model;
        this.status = status;
        this.gradient = gradient;
    }
    
    public HeatMapStatus getStatus()
    {
        return status;
    }
    
    public void setHeatMapStatus(HeatMapStatus status)
    {
        this.status = status;
    }
    
    public HeatMapGradient getGradient()
    {
        return gradient;
    }
    
    public void setHeatMapGradient(HeatMapGradient gradient)
    {
        this.gradient = gradient;
    }
    
    public void setCurrentMode(HeatMapMode mode)
    {
        currentMode = mode;
        displayInformation(attributeName,elementName);
    }
    
    public void setCurrentScale(String scaleName)
    {
        currentScaleName = scaleName;
        recalibrateScale();
        displayInformation(attributeName,elementName);
    }
    
    /**
     * Tells the dispatcher to disable heat map mode for the selected
     * browser.
     */
    public void fireModeCancel()
    {
        BrowserModel browserModel = model.getInfoSource();
        List thumbnailList = browserModel.getThumbnails();
        for(Iterator iter = thumbnailList.iterator(); iter.hasNext();)
        {
            Thumbnail t = (Thumbnail)iter.next();
            t.removeMiddlePaintMethod(currentMethod);
            t.removeForegroundPaintMethod(currentValueMethod);
        }
    }
    
    /**
     * Tells the dispatcher to reenable heat map mode for the selected
     * browser.
     */
    public void fireModeReactivate()
    {
        if(currentMethod != null)
        {
            BrowserModel browserModel = model.getInfoSource();
            browserModel.addPaintMethod(currentMethod,
                                        Thumbnail.MIDDLE_PAINT_METHOD);
            browserModel.addPaintMethod(currentMethod,
                                        Thumbnail.FOREGROUND_PAINT_METHOD);
        }
    }
    
    /**
     * Fills in stuff in the browser model accordingly.
     * @see org.openmicroscopy.shoola.agents.browser.heatmap.HeatMapTreeListener#nodeSelected(org.openmicroscopy.shoola.agents.browser.heatmap.SemanticTypeTree.TreeNode)
     */
    public void nodeSelected(SemanticTypeTree.TreeNode node)
    {
        if(model == null || node == null)
        {
            return;
        }
        LoaderThread thread = new LoaderThread(node);
        thread.start();
    }
    
    /**
     * @see org.openmicroscopy.shoola.agents.browser.heatmap.HeatMapModeListener#modeChanged(org.openmicroscopy.shoola.agents.browser.heatmap.HeatMapMode)
     */
    public void modeChanged(HeatMapMode newMode)
    {
        setCurrentMode(newMode);
    }
    
    /**
     * @see org.openmicroscopy.shoola.agents.browser.heatmap.HeatMapModeListener#scaleChanged(org.openmicroscopy.shoola.agents.browser.heatmap.Scale)
     */
    public void scaleChanged(String newScale)
    {
        setCurrentScale(newScale);
    }
    
    private void recalibrateScale()
    {
        if(currentScaleName.equals(Scale.LINEAR_SCALE))
        {
            currentScale = new LinearScale(gradient.getMin(),gradient.getMax());
        }
        else if(currentScaleName.equals(Scale.LOGARITHMIC_SCALE))
        {
            currentScale = new LogarithmicScale(gradient.getMin(),gradient.getMax());
        }
    }
    
    private void analyzeInformation(String attribute, String elementName)
    {
        BrowserModel browserModel = model.getInfoSource();
        List thumbnails = browserModel.getThumbnails();
        Thumbnail[] ts = new Thumbnail[thumbnails.size()];
        thumbnails.toArray(ts);
        double[] stats = RangeChecker.getGlobalMinMax(ts,attribute,elementName);
        gradient.setMin(stats[0]);
        gradient.setMax(stats[1]);
        recalibrateScale();
    }


    // add all overlays to browser.
    private void displayInformation(String attribute, String elementName)
    {
        if(model == null || attribute == null || elementName == null)
        {
            return;
        }
        BrowserModel browserModel = model.getInfoSource();
        PaintMethod pm =
            HeatMapPMFactory.getPaintMethod(currentMode,attribute,elementName,
                                            currentScale,gradient.getMinColor(),
                                            gradient.getMaxColor());
        PaintMethod vpm =
            HeatMapPMFactory.getShowValueMethod(currentMode,attribute,elementName,
                                                0.75,Color.yellow);
        
        if(currentMethod != null)
        {
            browserModel.removePaintMethod(currentMethod,
                                           Thumbnail.MIDDLE_PAINT_METHOD);
            browserModel.removePaintMethod(currentValueMethod,
                                           Thumbnail.FOREGROUND_PAINT_METHOD);
        }
        browserModel.addPaintMethod(pm,Thumbnail.MIDDLE_PAINT_METHOD);
        browserModel.addPaintMethod(vpm,Thumbnail.FOREGROUND_PAINT_METHOD);
        
        currentMethod = pm;
        currentValueMethod = vpm;
    }
    
    private void displayBooleanInformation(String attribute, String elementName)
    {
        if(model == null || attribute == null || elementName == null)
        {
            return;
        }
        BrowserModel browserModel = model.getInfoSource();
        PaintMethod pm =
            HeatMapPMFactory.getBooleanPaintMethod(attribute,elementName,
                                                   new Color(255,0,0,153),
                                                   new Color(0,255,0,153));
        
        if(currentMethod != null)
        {
            browserModel.removePaintMethod(currentMethod,
                                           Thumbnail.MIDDLE_PAINT_METHOD);
            browserModel.removePaintMethod(currentValueMethod,
                                           Thumbnail.FOREGROUND_PAINT_METHOD);
        }
        browserModel.addPaintMethod(pm,Thumbnail.MIDDLE_PAINT_METHOD);
        currentMethod = pm;
    }
    
    private class LoaderThread extends Thread
    {
        private SemanticTypeTree.TreeNode selectedNode;
        
        public LoaderThread(SemanticTypeTree.TreeNode node)
        {
            selectedNode = node;
        }
        
        public void run()
        {
            if(model == null) return;
            if(selectedNode == null) return;

            if(!(selectedNode instanceof SemanticTypeTree.ElementNode) ||
               selectedNode.getFQName() == null)
            {
                fireModeCancel();
                currentMethod = null;
                currentValueMethod = null;
                return;
            }
            
            DataElementType dataType =
                ((SemanticTypeTree.ElementNode)selectedNode).getType();
            
            if(dataType == DataElementType.BOOLEAN)
            {
                gradient.setDiscrete(true);
            }
            else
            {
                gradient.setDiscrete(false);
            }
            
            if(selectedNode.isLazilyInitialized())
            {
                // get attribute
                elementName = selectedNode.getFQName();
                SemanticTypeTree.TreeNode pathNode = selectedNode;
                SemanticTypeTree.TypeNode parentNode = null;
                while(pathNode.getParent() instanceof SemanticTypeTree.TypeNode &&
                      pathNode.getParent() != null)
                {
                    parentNode = (SemanticTypeTree.TypeNode)pathNode.getParent();
                    pathNode = parentNode;
                }
                SemanticType type = 
                    ((SemanticTypeTree.TypeNode)pathNode).getType();
                attributeName = type.getName();
                
                if(dataType == DataElementType.BOOLEAN)
                {
                    displayBooleanInformation(attributeName,elementName);
                }
                else
                {
                    analyzeInformation(attributeName,elementName);
                    displayInformation(attributeName,elementName);
                }
                
            }
            else
            {
                selectedNode.markAsInitialized(true);
                BrowserEnvironment env = BrowserEnvironment.getInstance();
                BrowserAgent agent = env.getBrowserAgent();
                SemanticTypesService sts = agent.getSemanticTypesService();
                
                SemanticTypeTree.TreeNode pathNode = selectedNode;
                SemanticTypeTree.TypeNode parentNode = null;
                while(pathNode.getParent() instanceof SemanticTypeTree.TypeNode &&
                      pathNode.getParent() != null)
                {
                    parentNode = (SemanticTypeTree.TypeNode)pathNode.getParent();
                    parentNode.markAsInitialized(true);
                    Set children = parentNode.getChildren();
                    for(Iterator iter = children.iterator(); iter.hasNext();)
                    {
                        Object o = iter.next();
                        // NOTE: this depends on the contract that the STS
                        // retrieves the attribute as a whole, which is correct
                        // but hard to explicitly formalize and check
                        if(o instanceof SemanticTypeTree.ElementNode)
                        {
                            ((SemanticTypeTree.ElementNode)o).
                                markAsInitialized(true);
                        }
                    }
                    pathNode = parentNode;
                }
                
                SemanticType parentType = parentNode.getType();
                String name = parentType.getName();
                attributeName = name;
                elementName = selectedNode.getFQName();
                BrowserModel source = model.getInfoSource();
                
                Map imageIDMap = source.getImageDataMap();
                List imageIDList = new ArrayList(imageIDMap.keySet());
                Collections.sort(imageIDList);
                
                if(status != null)
                {
                    status.showMessage("Loading "+name+" attributes...");
                }
                try
                {
                    List attributeList =
                        sts.retrieveImageAttributes(name,selectedNode.getDBName(),
                                                    imageIDList);
                    
                    for(Iterator iter = attributeList.iterator(); iter.hasNext();)
                    {
                        Attribute attribute = (Attribute)iter.next();
                        int imageID = attribute.getImage().getID();
                        ThumbnailDataModel tdm =
                            (ThumbnailDataModel)imageIDMap.get(new Integer(imageID));
                        AttributeMap attrMap = tdm.getAttributeMap();
                        attrMap.putAttribute(attribute);
                    }
                    
                    if(status != null)
                    {
                        status.showMessage("Loaded "+name+" attributes.");
                    }
                    
                    if(dataType == DataElementType.BOOLEAN)
                    {
                        displayBooleanInformation(attributeName,elementName);
                    }
                    else
                    {
                        analyzeInformation(attributeName,elementName);
                        displayInformation(attributeName,elementName);
                    }
                }
                catch(DSOutOfServiceException dso)
                {
                    if(status != null)
                    {
                        status.showMessage("could not retrieve "+name+" data.");
                    }
                }
                catch(DSAccessException dsa)
                {
                    if(status != null)
                    {
                        status.showMessage("could not retrieve "+name+" data.");
                    }
                }
            }
        }
    }
}
