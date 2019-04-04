package org.cytoscape.enhancer;

import java.awt.geom.Point2D;

import org.cytoscape.task.NetworkViewLocationTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class EnhancerTaskFactory extends AbstractTaskFactory implements NetworkViewLocationTaskFactory, TaskFactory{

	@Override
	public TaskIterator createTaskIterator() {		return null;	}

	@Override
	public TaskIterator createTaskIterator(CyNetworkView networkView, Point2D javaPt, Point2D xformPt) {
		return null;
	}

	@Override
	public boolean isReady(CyNetworkView networkView, Point2D javaPt, Point2D xformPt) {		return true;	}

}
