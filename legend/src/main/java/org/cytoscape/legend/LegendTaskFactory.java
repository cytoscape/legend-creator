package org.cytoscape.legend;

import java.awt.geom.Point2D;

import org.cytoscape.task.NetworkViewLocationTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class LegendTaskFactory extends AbstractTaskFactory implements NetworkViewLocationTaskFactory, TaskFactory{

	@Override
<<<<<<< HEAD
	public TaskIterator createTaskIterator() {
		return null;
	}
=======
	public TaskIterator createTaskIterator() {		return null;	}
>>>>>>> 488dd4a8f7ef97482ffbbea100596c7d623156be

	@Override
	public TaskIterator createTaskIterator(CyNetworkView networkView, Point2D javaPt, Point2D xformPt) {
		return null;
	}

	@Override
	public boolean isReady(CyNetworkView networkView, Point2D javaPt, Point2D xformPt) {		return true;	}

}
