package org.cytoscape.enhancer;

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
		
		EnhancerController controller = new EnhancerController(reg);
		EnhancerPanel enhancerPanel = new EnhancerPanel(controller);
//		EnhancerAction legendAction = new EnhancerAction(swingApp,legendPanel);
		
		registerService(bc,enhancerPanel,CytoPanelComponent.class);
//		registerService(bc,legendAction,CyAction.class);
		registerAllServices(bc,controller);
		registerService(bc, controller, SetCurrentNetworkListener.class);
//		System.out.println("loading legendCreator!");


		EnhancerTaskFactory myTaskFactory = new EnhancerTaskFactory();
		Properties prop = new Properties();  
		prop.setProperty(ServiceProperties.ENABLE_FOR, "true");
		prop.setProperty(ServiceProperties.PREFERRED_MENU, ServiceProperties.NETWORK_ADD_MENU);
		prop.setProperty(ServiceProperties.MENU_GRAVITY,"4");
		prop.setProperty(ServiceProperties.TITLE,"Enhancer");
		registerService(bc,myTaskFactory,  EnhancerTaskFactory.class, prop);
	}
}



