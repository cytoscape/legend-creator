package org.cytoscape.enhancer;

import java.awt.Component;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedEvent;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;

/*
 * EnhancerController
 * 
 
 */

public class EnhancerController implements CytoPanelComponentSelectedListener, SetCurrentNetworkListener {

	private CyServiceRegistrar registrar;
	public EnhancerController(CyServiceRegistrar reg)
	{
		registrar = reg;
	}

//----------------------------------------------------------
	private CyApplicationManager cyApplicationManager;
	private CyNetwork network;
	private CyNetworkView networkView;
	private EnhancerPanel enhancerPanel;
	public void setEnhancerPanel(EnhancerPanel p) { enhancerPanel = p; }
	//----------------------------------------------------------
	private void initialize()
	{
		cyApplicationManager = registrar.getService(CyApplicationManager.class);
		network = cyApplicationManager.getCurrentNetwork();
		networkView = cyApplicationManager.getCurrentNetworkView();
}
	//-------------------------------------------------------------------
	@Override
	public void handleEvent(CytoPanelComponentSelectedEvent arg0) 
	{	
		Component comp = arg0.getCytoPanel().getSelectedComponent();
//		if (comp instanceof EnhancerPanel)
//			scanNetwork();
	}
	//-------------------------------------------------------------------
	public CyNetworkView getNetworkView()		{ 	 return networkView;		}
		
		
	public void scanNetwork() {	}
	

	public void setCurrentNetView(CyNetworkView newView)
	{
		if (newView == null) return;		// use ""
//		if (newView.getSUID() == currentNetworkView.getSUID()) return;
		currentNetworkView = newView;
		enhancerPanel.enableControls(currentNetworkView != null);
		
	}
	//-------------------------------------------------------------------------------
	private CyNetworkView currentNetworkView;
	private boolean verbose = true;
	
	public String getCurrentNetworkName() {
		if (currentNetworkView != null)
			return "" + currentNetworkView.getModel().getSUID();		// TODO -- getCurrentNetworkName
		return "";
	}
		//-------------------------------------------------------------------------------
	public void layout()
	{
		networkView = cyApplicationManager.getCurrentNetworkView();

	}

	@Override
	public void handleEvent(SetCurrentNetworkEvent e) {

		network = e.getNetwork();
		if (cyApplicationManager != null)
		{
			networkView = cyApplicationManager.getCurrentNetworkView();
//			scanNetwork();
		}
	}
	
}
