package org.cytoscape.legend;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.DiscreteRange;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.annotations.Annotation;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.annotations.GroupAnnotation;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation.ShapeType;
import org.cytoscape.view.presentation.annotations.TextAnnotation;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.LineType;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;

/*
 * LegendController
 * 
 * there are two major functions:  scanNetwork() and layout()
 * 
 * scanNetwork() gets the style of the current network and
 * extracts the mapping functions.  Passthrough functions are ignored.
 * Discrete and continuous functions become check boxes in the panel.
 * 
 * A class for LegendCandidate is created in scanNetwork with 
 * a visible flag, so that if the user unchecks the box, that
 * legend is skipped
 * 
 * layout() looks at which check boxes are selected and assembles
 * one GroupAnnotation for each attribute that is checked.
 */

public class LegendController {

	private CyServiceRegistrar registrar;
	public LegendController(CyServiceRegistrar reg)
	{
		registrar = reg;
	}

//----------------------------------------------------------
	private CyApplicationManager cyApplicationManager;
	private CyNetwork network;
	private CyNetworkView networkView;
	private AnnotationManager annotationMgr;
	private LegendPanel legendPanel;
	private LegendFactory factory;
	public void setLegendPanel(LegendPanel p) { legendPanel = p; }
	//----------------------------------------------------------
	private void initialize()
	{
//		if (cyApplicationManager != null) return;
		cyApplicationManager = registrar.getService(CyApplicationManager.class);
		annotationMgr = registrar.getService(AnnotationManager.class);
		network = cyApplicationManager.getCurrentNetwork();
		networkView = cyApplicationManager.getCurrentNetworkView();
		factory = new LegendFactory(registrar, networkView);
//		double width = networkView.getVisualProperty(BasicVisualLexicon.NETWORK_WIDTH);
//		double height = networkView.getVisualProperty(BasicVisualLexicon.NETWORK_WIDTH);


	}
	//-------------------------------------------------------------------
		public void scanNetwork() {

		initialize();
		if (network == null) return;
		
		// Now we cruise thru the list of node, then edge attributes looking for mappings.  Each mapping may potentially be a legend entry.
		VisualMappingManager manager = (VisualMappingManager) registrar.getService( VisualMappingManager.class);
		VisualStyle style = manager.getCurrentVisualStyle();
		System.out.println("style: " + style.getTitle());
		findLegendCandidates(style);
	}
	
	private List<LegendCandidate> candidates  = new ArrayList<LegendCandidate>();
	public List<LegendCandidate> getCandidates() { return candidates;	}
	private void findLegendCandidates(VisualStyle style)
	{
		Collection<VisualMappingFunction<?, ?>> vizmapFns =  style.getAllVisualMappingFunctions();
		candidates.clear();
		for (VisualMappingFunction<?,?> fn : vizmapFns)
		{
			String mappingType = fn.toString();
			if (mappingType.contains("Passthrough")) continue;
			candidates.add(new LegendCandidate(fn));
		}
		Collections.sort(candidates);
		
	}
	//-------------------------------------------------------------------------------
//	boolean orientVertically = true;
	boolean layoutVertically = false;
	boolean borderBox = false;

//	boolean showSubtitle = true;
//	boolean showUserTitle = false;
//	boolean showDate = false;

	String title;
	String subtitle;
	
//	public void setOrientation(boolean vert)	{ orientVertically = vert;	}
	public void setLayout(boolean vert)		{ layoutVertically = vert;	}
	public void setDrawBorder(boolean show)	{ borderBox = show;	factory.setDrawBorder(show);}
	
	public void setTitle(String txt) 	{  title = txt;}
	public void setSubtitle(String txt) 		{ subtitle = txt; }
//	public void setShowDate(boolean b) 		{ showDate = b;	}

	//-------------------------------------------------------------------------------
	public void layout()
	{
		for (LegendCandidate candidate : candidates)
			candidate.extract();			
		if (legendPanel != null)
			legendPanel.extract();
		
//		System.out.println(orientVertically ? "Tall" : "Wide");
		System.out.println(layoutVertically ? "Stacked" : "Neighbors");
		
		int X = 500;			// starting point
		int Y = 500;
		int startX = X;
		int startY = Y;
		int DEFAULT_WIDTH = 500;			// starting point
		int DEFAULT_HEIGHT = 100;
		int dX = layoutVertically ? 0 : 500;
		int dY = layoutVertically ? 500 : 0;			
		int SPACER = 150;
		int LINE_HEIGHT = 30;
		int HALFSPACE = SPACER / 2;
		
		if (title.length() > 0)
		{
			Object[] textArgs = { "x", X , "y", Y, "width", DEFAULT_WIDTH, "height", LINE_HEIGHT, "text", title,  "fontSize", 24, "fontFamily", "Serif" };
			Map<String,String> strs = LegendFactory.ezMap(textArgs);
			TextAnnotation textBox = factory.createTextAnnotation(TextAnnotation.class, networkView, strs);
			textBox.setCanvas("background");
			annotationMgr.addAnnotation(textBox);
			Y += LINE_HEIGHT;
		
		}
		if (subtitle.length() > 0)
		{
			Object[] textArgs = { "x", X , "y", Y, "width", DEFAULT_WIDTH, "height", LINE_HEIGHT, "text", subtitle,  "fontSize", 14, "fontFamily", "SansSerif" };
			Map<String,String> strs = LegendFactory.ezMap(textArgs);
			TextAnnotation textBox = factory.createTextAnnotation(TextAnnotation.class, networkView, strs);
			textBox.setCanvas("background");
			annotationMgr.addAnnotation(textBox);
			Y += LINE_HEIGHT;
		}
		if (title.length() > 0 || subtitle.length() > 0)  
			Y += LINE_HEIGHT;
		
		for (LegendCandidate candidate : candidates)
		{
			if (!candidate.isVisible())   // check box not selected
				continue;
			VisualMappingFunction<?,?> fn = candidate.getFunction();
			String type = fn.getMappingColumnType().toString();
			int idx = type.lastIndexOf('.');
			if (idx > 0) type = type.substring(idx+1);
			VisualProperty<?> prop = fn.getVisualProperty();
			String dispName = prop.getDisplayName();
			String mapType = fn.toString();
			if (mapType.contains("Passthrough")) continue;
			
			Dimension size= new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
			System.out.println("The attribute " + fn.getMappingColumnName() + " (" + 
					type + ") is shown with " + dispName + " with a " + mapType + " function");
			
			if (mapType.contains("Continuous"))
			{
				factory.addContinuousMapLegend((ContinuousMapping<?, ?>) fn, X, Y, size);
			}
			else if (mapType.contains("Discrete"))
			{
				addDiscreteMapLegend((DiscreteMapping<?, ?>) fn, X, Y, size);		
			}
			if (layoutVertically) 	{  	dX = 0; dY = size.height + SPACER; }		// increment Y
			else 					{	dY = 0; dX = size.width + SPACER; }		// increment X

			X += dX;
			Y += dY;
		}
		int totalWidth = X - startX;
		int totalHeight = Y - startY;
		if (layoutVertically)	totalWidth += DEFAULT_WIDTH + SPACER;
		else 					totalHeight += DEFAULT_HEIGHT + SPACER;
		if (borderBox)
		{
			Object[] boxArgs = { "x", startX - HALFSPACE, "y", startY - HALFSPACE , "width", totalWidth, "height", totalHeight,  "shapeType" , "Rectangle"};
			Map<String,String> strs = LegendFactory.ezMap(boxArgs);
			ShapeAnnotation lineBox = factory.createShapeAnnotation(ShapeAnnotation.class, networkView, strs);
			annotationMgr.addAnnotation(lineBox);
		}
//		networkView.refresh();
	}


	static public ShapeType getShapeType(String shapeName) {
		for (ShapeType type: ShapeType.values()) {
			if (shapeName.equalsIgnoreCase(type.shapeName()))
				return type;
		}
		return null;
	}

	private void addDiscreteMapLegend(DiscreteMapping<?, ?> fn, int x, int y, Dimension outSize) 
	{
//		boolean orientVertically = false;
//		int width = orientVertically ? SHORT_SIDE : LONG_SIDE;
//		int height = orientVertically ? LONG_SIDE : SHORT_SIDE;
		VisualProperty<?> prop = fn.getVisualProperty();
		String dispName = prop.getDisplayName();
		GroupAnnotation legend = null;
		Map<NodeShape, CyNode> used = getUsedShapes();
		if ("Node Shape".equals(dispName))
		{
			List<String> objectNames = new ArrayList<String>();
			List<ShapeType> shapeList = new ArrayList<ShapeType>();
			Map<?,?> set = fn.getAll();
			for (Object key : set.keySet())
			{
				String s = key.toString();
				Object val = set.get(s);
				ShapeType shape = getShapeType(val.toString());
				
				if (shape != null && shapeIsUsed(shape, used))
				{
					shapeList.add(shape);
					objectNames.add(s);
				}
			}
			
			ShapeType[] shapes = shapeList.toArray(new ShapeType[0]);
			String[] oNames = objectNames.toArray(new String[0]);
			legend = factory.addShapeLegend(title, x, y, outSize, shapes, oNames);
		}
		else if ("Node Fill Color".equals(dispName))
		{
			List<String> objectNames = new ArrayList<String>();
			List<Color> colorList = new ArrayList<Color>();
			Map<?,?> set = fn.getAll();
			for (Object key : set.keySet())
			{
				String s = key.toString();
				Object val = set.get(s);
				if (val instanceof Color)
				{
					Color c = (Color) val;
					if (c != null)
					{
						colorList.add(c);
						objectNames.add(s);
					}
				}
			}
			Color[] colors = colorList.toArray(new Color[0]);
			String[] oNames = objectNames.toArray(new String[0]);
			legend = factory.addDiscreteColorLegend(dispName, x, y, outSize, colors, oNames);
		}
		else if ("Edge Line Type".equals(dispName))
		{
			List<String> objectNames = new ArrayList<String>();
			List<LineType> types = new ArrayList<LineType>();
			Map<?,?> set = fn.getAll();
			for (Object key : set.keySet())
			{
				String s = key.toString();
				Object val = set.get(s);
				if (val instanceof LineType)
				{
					LineType type = (LineType) val;
					if (type != null)
					{
						types.add(type);
						objectNames.add(s);
					}
				}
			}
			LineType[] linetypes = types.toArray(new LineType[0]);
			String[] oNames = objectNames.toArray(new String[0]);
			legend = factory.addLinetypeLegend(dispName, x, y, outSize, linetypes, oNames);
		}
		else if ("Target Arrow".equals(dispName))
		{
			legend = factory.addArrowheadLegend(dispName, x, y, outSize, arrowheads, arrowNames);
		}
		outSize.width += factory.getRightMargin();
		
		if (legend != null)
			annotationMgr.addAnnotation(legend);
		
	}
	private boolean shapeIsUsed(ShapeType shape, Map<NodeShape, CyNode >used) {
		String shapeName = shape.shapeName();
		if (shapeName == null) return false;
		for (NodeShape nodeShape : used.keySet())
		{
			String displayName = nodeShape.getDisplayName();
			if (shapeName.equals(displayName))
				return true;
		}

		return false;
	}

	int SHORT_SIDE = 100;
	int LONG_SIDE = 400;
	//------------------  LegendCandidate -----------------------------------------------
  class LegendCandidate implements Comparable<LegendCandidate>
  {
	  VisualMappingFunction<?,?> func;
	  boolean visible = true;
	  JCheckBox checkBox = null;

	  public LegendCandidate(VisualMappingFunction<?,?> f)
	  {
		  func = f;
	  }

	  public String toString()	  {	  return func.getMappingColumnName();  }
	  public VisualMappingFunction<?,?> getFunction()	  {	  return func;  }
	  public void setCheckBox(JCheckBox ck)	  	{	checkBox = ck;  }
	  public JCheckBox getCheckBox()	  			{	return checkBox;  }
	  public void extract()	  					{	if (checkBox != null) 	visible = checkBox.isSelected();  }
	  boolean isVisible()						{ 	return visible;	}
	  
	@Override public int compareTo(LegendCandidate o) {
	
		VisualProperty<?> prop = func.getVisualProperty();
		String dispName = prop.getDisplayName();
		VisualProperty<?> otherProp = o.getFunction().getVisualProperty();
		String otherPropName = otherProp.getDisplayName();

		boolean imaNode = dispName.contains("Node");
		boolean uraNode = otherPropName.contains("Node");
		String colName = func.getMappingColumnName() + dispName;
		String otherName = o.getFunction().getMappingColumnName() + otherPropName;
		
		if (imaNode && !uraNode) return -1;
		if (uraNode && !imaNode) return 1;
		return colName.compareTo(otherName);
	}
  }
//-----------------------  TEST  -------------------------------------------
	String[] names= { "Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel" };
	Color[] discreteColors = { Color.BLACK, Color.BLUE, Color.RED, Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW };
	float[] stops = {0.0f, 0.5f, 1.0f};
	Color[] colors = {Color.BLUE, Color.WHITE, Color.YELLOW};

	String[] LINETYPE_NAMES = { "Solid", "Parallel Lines", "Equal Dash", "Dots", "Dash Dot"} ;
	LineType[] LINETYPES =  {
		  getCyNodeLineType("Solid"), getCyNodeLineType("Parallel Lines"),  getCyNodeLineType("Equal Dash"), getCyNodeLineType("Dots"), getCyNodeLineType("Dash Dot") };
	String[] arrowNames = { "A", "B", "C", "D", "E"};
	String[] arrowheads = {"Open-Circle", "Circle", "Open-Square", "Square", "Delta"};
	ShapeType[] shapes = { ShapeType.RECTANGLE, ShapeType.ELLIPSE, ShapeType.TRIANGLE, ShapeType.PENTAGON, ShapeType.HEXAGON };
	String[] shapenames= { "Ligand", "Kinase", "Promoter", "RNA", "Antigen" };

	public void testAnnotations() 
	{	
		initialize();
		if (legendPanel != null)
			legendPanel.extract();
		
		int X = 200;			// starting point
		int Y = layoutVertically ? 500 : 0;
		int SPACER = 120;
		int HALFSPACE = SPACER / 2;
		int startX = X - SPACER;
		int startY = Y - SPACER;
		int W = 500;
		int H = 100;
//		int dX = layoutVertically ? 0 : W + SPACER;
//		int dY = layoutVertically ? H + SPACER : 0;			

		GroupAnnotation gradientLegend = factory.addGradientLegend("Fill Color: layout.degree ",  X, Y, W, H, -2, 3, colors, stops);
		annotationMgr.addAnnotation(gradientLegend);
		X += layoutVertically ? 0 : W + SPACER;  	
		Y += layoutVertically ? H + SPACER : 0;	
		
		Dimension legendSize = new Dimension( 200, 500);
		GroupAnnotation discreteLegend = factory.addDiscreteColorLegend("Fill Color: Type", X, Y, legendSize, discreteColors, names);
		annotationMgr.addAnnotation(discreteLegend);
		X += layoutVertically ? 0 : legendSize.width + SPACER;  	
		Y += layoutVertically ? legendSize.height + SPACER : 0;	

		W = 500;
		H = 200;
		GroupAnnotation continuousLegend = factory.addTrapezoidLegend("Node Size: Expression", X, Y, W, H, null, Color.LIGHT_GRAY);
		annotationMgr.addAnnotation(continuousLegend);
		X += layoutVertically ? 0 : W + SPACER;  	
		Y += layoutVertically ? H + SPACER : 0;	
	
		W = 300;
		H = 300;
		legendSize = new Dimension( 300, 300);
		GroupAnnotation linetypeLegend = factory.addLinetypeLegend("Line Type: Function", X, Y, legendSize, LINETYPES, LINETYPE_NAMES);
		annotationMgr.addAnnotation(linetypeLegend);
		X += layoutVertically ? 0 : legendSize.width + SPACER;  	
		Y += layoutVertically ? legendSize.height + SPACER : 0;	

		legendSize = new Dimension( 300, 500);
		GroupAnnotation arrowLegend =  factory.addArrowheadLegend("Arrowhead: Interaction Type", X, Y, legendSize, arrowNames, arrowheads);
		annotationMgr.addAnnotation(arrowLegend);
		X += layoutVertically ? 0 : legendSize.width + SPACER;  	
		Y += layoutVertically ? legendSize.height + SPACER : 0;	

		legendSize = new Dimension( 200, 500);
		GroupAnnotation shapeLegend = factory.addShapeLegend("Node Shape: Protein Type ", X, Y, legendSize, shapes, shapenames);
		annotationMgr.addAnnotation(shapeLegend);
		X += layoutVertically ? 0 : legendSize.width + SPACER;  	
		Y += layoutVertically ? legendSize.height + SPACER : 0;	

		GroupAnnotation fontSizeLegend =  factory.addFontSizeLegend("Font Size: degree", X, Y, legendSize, 10, 50, Color.cyan);
		annotationMgr.addAnnotation(fontSizeLegend);
		X += layoutVertically ? 0 : legendSize.width + SPACER;  	
		Y += layoutVertically ? legendSize.height + SPACER : 0;	

		int DEFAULT_WIDTH = 550;	
		int DEFAULT_HEIGHT = 150;
		int totalWidth = X - startX;
		int totalHeight = Y - startY;
		if (layoutVertically)	totalWidth += DEFAULT_WIDTH + SPACER;
		else 					totalHeight += DEFAULT_HEIGHT + SPACER;
		if (borderBox)
		{
			Object[] boxArgs = { "x", startX - HALFSPACE, "y", startY - HALFSPACE , "width", totalWidth, "height", totalHeight,  "shapeType" , "Rectangle"};
			Map<String,String> strs = LegendFactory.ezMap(boxArgs);
			ShapeAnnotation lineBox = factory.createShapeAnnotation(ShapeAnnotation.class, networkView, strs);
			annotationMgr.addAnnotation(lineBox);
		};
		
		dump();
	}	

	  static LineType getCyNodeLineType(final String displayName) {
	    for (final LineType lineType : ((DiscreteRange<LineType>) BasicVisualLexicon.NODE_BORDER_LINE_TYPE.getRange()).values()) {
	      final String lineTypeName = lineType.getDisplayName();
	      if (displayName.equals(lineTypeName)) {
	        return lineType;
	      }
	    }
	    return null;
	  }
	
	//-------------------------------------------------------------------
	  
	  boolean verbose = true;
	private Map<NodeShape, CyNode> getUsedShapes()
	{
		Map<NodeShape, CyNode> shapeTypeMap = new HashMap<NodeShape, CyNode>();
		Collection<View<CyNode>> nodeViews = networkView.getNodeViews();
		for (View<CyNode> nodeView : nodeViews)
		{
			 NodeShape shape = (NodeShape) nodeView.getVisualProperty(BasicVisualLexicon.NODE_SHAPE);
			 shapeTypeMap.put(shape, nodeView.getModel());
		}
		return shapeTypeMap;
	}
	
	private Map<Paint, CyNode> getUsedFillColors()
	{
		Map<Paint, CyNode> fillColorMap = new HashMap<Paint, CyNode>();
		Collection<View<CyNode>> nodeViews = networkView.getNodeViews();
		for (View<CyNode> nodeView : nodeViews)
		{
			 Paint paint = nodeView.getVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR);
			 fillColorMap.put(paint, nodeView.getModel());
		}
		return fillColorMap;
	}
	
	private Map<Paint, CyNode> getUsedBorderColors()
	{
		Map<Paint, CyNode> borderColorMap = new HashMap<Paint, CyNode>();
		Collection<View<CyNode>> nodeViews = networkView.getNodeViews();
		for (View<CyNode> nodeView : nodeViews)
		{
			 Paint paint = nodeView.getVisualProperty(BasicVisualLexicon.NODE_BORDER_PAINT);
			 borderColorMap.put(paint, nodeView.getModel());
		}
		return borderColorMap;
	}
	private Map<Paint, CyNode> getUsedEdgeColors()
	{
		Map<Paint, CyNode> edgeColorMap = new HashMap<Paint, CyNode>();
		List<CyNode> nodes = network.getNodeList();
		for (CyNode node : nodes)
		{
			 View<CyNode> nodeView = networkView.getNodeView(node);
			 Paint paint = nodeView.getVisualProperty(BasicVisualLexicon.EDGE_PAINT);
			 edgeColorMap.put(paint, node);
		}
		return edgeColorMap;
	}
	
	private Map<LineType, CyNode> getUsedLineTypes()
	{
		Map<LineType, CyNode> lineTypeMap = new HashMap<LineType, CyNode>();
		List<CyNode> nodes = network.getNodeList();
		for (CyNode node : nodes)
		{
			 View<CyNode> nodeView = networkView.getNodeView(node);
			 LineType type = nodeView.getVisualProperty(BasicVisualLexicon.EDGE_LINE_TYPE);
			 lineTypeMap.put(type, node);
		}
		return lineTypeMap;
	}
	
	
	private void dump() {
		if (!verbose) return;

		System.out.println("\n\nThere are " + getUsedFillColors().size() + " fill colors and " + getUsedBorderColors().size() + " border colors used.");
		System.out.println("\n\nThere are " + getUsedShapes().size() + " shapes used.");
		System.out.println("There are " + getUsedEdgeColors().size() + " edge colors and " + getUsedLineTypes().size() + " line types used.");
	}
	
	public void selectAllAnnotations() {
		List<Annotation> annos = annotationMgr.getAnnotations(networkView);
		for (Annotation an : annos)
			an.setSelected(true);
	}

	public void clearAnnotations() {
		if (annotationMgr == null || networkView == null)
		{
			System.out.println("annotationMgr:  " + annotationMgr + "  networkView: " + networkView);
			return;
		}
	
		for (Annotation a: annotationMgr.getAnnotations(networkView)) {
		    if (a == null) return;
			System.out.println("a:  " + a);
		    annotationMgr.removeAnnotation(a);
		}
		
	}
	
}