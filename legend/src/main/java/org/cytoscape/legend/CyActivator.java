package org.cytoscape.legend;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CyAction;

import org.osgi.framework.BundleContext;

import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;

import java.util.Properties;


public class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}


	public void start(BundleContext bc) {
		CySwingApplication swingApp = getService(bc,CySwingApplication.class);
		CyServiceRegistrar reg = getService(bc,CyServiceRegistrar.class);
		
		LegendController controller = new LegendController(reg);
		LegendPanel legendPanel = new LegendPanel(controller);
		LegendAction legendAction = new LegendAction(swingApp,legendPanel);
		
		registerService(bc,legendPanel,CytoPanelComponent.class);
		registerService(bc,legendAction,CyAction.class);
		registerAllServices(bc,controller);
//		System.out.println("loading legendCreator!");
	}
}



