package org.cytoscape.legend;

import java.awt.Paint;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.LineType;
import org.cytoscape.view.presentation.property.values.NodeShape;

public class MapBuilder {

	public static Map<NodeShape, CyNode> getUsedShapes(CyNetworkView networkView) {
		Map<NodeShape, CyNode> shapeTypeMap = new HashMap<NodeShape, CyNode>();
		Collection<View<CyNode>> nodeViews = networkView.getNodeViews();
		for (View<CyNode> nodeView : nodeViews) {
			NodeShape shape = (NodeShape) nodeView.getVisualProperty(BasicVisualLexicon.NODE_SHAPE);
			shapeTypeMap.put(shape, nodeView.getModel());
		}
//		System.out.println("shapes " + shapeTypeMap.size() + "  " + shapeTypeMap.keySet());
		return shapeTypeMap;
	}

	public static Map<NodeShape, CyNode> getAllValues(CyNetworkView networkView, String column) {
		Map<NodeShape, CyNode> shapeTypeMap = new HashMap<NodeShape, CyNode>();
		Collection<View<CyNode>> nodeViews = networkView.getNodeViews();
		for (View<CyNode> nodeView : nodeViews) {
			NodeShape shape = (NodeShape) nodeView.getVisualProperty(BasicVisualLexicon.NODE_SHAPE);
			shapeTypeMap.put(shape, nodeView.getModel());
		}

//		System.out.println("shapes " + shapeTypeMap.size() + "  " + shapeTypeMap.keySet());
		return shapeTypeMap;
	}

	public static Map<Paint, CyNode> getUsedFillColors(CyNetworkView networkView) {
		Map<Paint, CyNode> fillColorMap = new HashMap<Paint, CyNode>();
		Collection<View<CyNode>> nodeViews = networkView.getNodeViews();
		for (View<CyNode> nodeView : nodeViews) {
			Paint paint = nodeView.getVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR);
			fillColorMap.put(paint, nodeView.getModel());
		}
//		System.out.println("colors " + fillColorMap.size() + "  " + fillColorMap.keySet());
		return fillColorMap;
	}

	public static Map<Paint, CyNode> getUsedBorderColors(CyNetworkView networkView) {
		Map<Paint, CyNode> borderColorMap = new HashMap<Paint, CyNode>();
		Collection<View<CyNode>> nodeViews = networkView.getNodeViews();
		for (View<CyNode> nodeView : nodeViews) {
			Paint paint = nodeView.getVisualProperty(BasicVisualLexicon.NODE_BORDER_PAINT);
			borderColorMap.put(paint, nodeView.getModel());
		}
		return borderColorMap;
	}

	public static Map<Paint, CyNode> getUsedEdgeColors(CyNetworkView networkView) {
		Map<Paint, CyNode> edgeColorMap = new HashMap<Paint, CyNode>();
		Collection<View<CyNode>> nodeViews = networkView.getNodeViews();
		for (View<CyNode> nodeView : nodeViews) {
			Paint paint = nodeView.getVisualProperty(BasicVisualLexicon.EDGE_PAINT);
			edgeColorMap.put(paint, nodeView.getModel());
		}
		return edgeColorMap;
	}

	public static Map<LineType, CyNode> getUsedLineTypes(CyNetworkView networkView) {
		Map<LineType, CyNode> lineTypeMap = new HashMap<LineType, CyNode>();
		Collection<View<CyNode>> nodeViews = networkView.getNodeViews();
		for (View<CyNode> nodeView : nodeViews) {
			LineType type = nodeView.getVisualProperty(BasicVisualLexicon.EDGE_LINE_TYPE);
			lineTypeMap.put(type, nodeView.getModel());
		}
		return lineTypeMap;
	}

	public static void dump(CyNetworkView networkView) {

		System.out.println("\n\nThere are " + getUsedFillColors(networkView).size() + " fill colors and "
				+ getUsedBorderColors(networkView).size() + " border colors used.");
		System.out.println("\n\nThere are " + getUsedShapes(networkView).size() + " shapes used.");
		System.out.println("There are " + getUsedEdgeColors(networkView).size() + " edge colors and "
				+ getUsedLineTypes(networkView).size() + " line types used.");
	}
}
