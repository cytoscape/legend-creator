package org.cytoscape.legend;

import java.util.Properties;

import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.ServiceProperties;
import org.osgi.framework.BundleContext;


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
		registerService(bc, controller, SetCurrentNetworkListener.class);
//		System.out.println("loading legendCreator!");


		LegendTaskFactory myTaskFactory = new LegendTaskFactory();
		Properties prop = new Properties();  
		prop.setProperty(ServiceProperties.ENABLE_FOR, "true");
		prop.setProperty(ServiceProperties.PREFERRED_MENU, ServiceProperties.NETWORK_ADD_MENU);
		prop.setProperty(ServiceProperties.MENU_GRAVITY,"4");
		prop.setProperty(ServiceProperties.TITLE,"Legend...");
		registerService(bc,myTaskFactory,  LegendTaskFactory.class, prop);
//		System.out.println("registerService " + "Add Legend...");
	}
}



