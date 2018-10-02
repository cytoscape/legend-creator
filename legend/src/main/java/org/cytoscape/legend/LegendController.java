package org.cytoscape.legend;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedEvent;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;
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

public class LegendController implements CytoPanelComponentSelectedListener {

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
		factory = new LegendFactory(registrar, networkView);

		network = cyApplicationManager.getCurrentNetwork();
		networkView = cyApplicationManager.getCurrentNetworkView();
}
	//-------------------------------------------------------------------
	@Override
	public void handleEvent(CytoPanelComponentSelectedEvent arg0) 
	{	
		Component comp = arg0.getCytoPanel().getSelectedComponent();
		if (comp instanceof LegendPanel)
			scanNetwork();
	}
	//-------------------------------------------------------------------
		public CyNetworkView getNetworkView()		{ 	 return networkView;		}
		
		
		public void scanNetwork() {

		initialize();
		candidates.clear();
		if (network == null) return;
		
		// Now we cruise thru the list of node, then edge attributes looking for mappings.  Each mapping may potentially be a legend entry.
		VisualMappingManager manager = (VisualMappingManager) registrar.getService( VisualMappingManager.class);
		VisualStyle style = manager.getCurrentVisualStyle();
//		System.out.println("style: " + style.getTitle());
		findLegendCandidates(style);
	}
	
	private List<LegendCandidate> candidates  = new ArrayList<LegendCandidate>();
	public List<LegendCandidate> getCandidates() { return candidates;	}
	private void findLegendCandidates(VisualStyle style)
	{
		Collection<VisualMappingFunction<?, ?>> vizmapFns =  style.getAllVisualMappingFunctions();
		for (VisualMappingFunction<?,?> fn : vizmapFns)
		{
			String mappingType = fn.toString();
			if (mappingType.contains("Passthrough")) continue;
			candidates.add(new LegendCandidate(fn));
		}
		Collections.sort(candidates);
		
	}
	
	public void setCurrentNetView(CyNetworkView newView)
	{
		if (newView == null) return;		// use ""
//		if (newView.getSUID() == currentNetworkView.getSUID()) return;
		currentNetworkView = newView;
		legendPanel.enableControls(currentNetworkView != null);
		
	}
	//-------------------------------------------------------------------------------
	private boolean layoutVertically = false;
	private boolean borderBox = false;
	private String title;
	private String subtitle;
	private CyNetworkView currentNetworkView;
	private boolean verbose = false;
	
	public void setLayout(boolean vert)		{ 	layoutVertically = vert;	}
	public void setDrawBorder(boolean show)	{ 	borderBox = show;	factory.setDrawBorder(show);}
	
	public void setTitle(String txt) 		{  	title = txt;}
	public void setSubtitle(String txt) 	{ 	subtitle = txt; }

	public String getCurrentNetworkName() {
		if (currentNetworkView != null)
			return "" + currentNetworkView.getModel().getSUID();		// TODO -- getCurrentNetworkName
		return "";
	}
		//-------------------------------------------------------------------------------
	public Rectangle2D.Double bounds()
	{
		List<CyNode> list = network.getNodeList();
		double minX=Double.MAX_VALUE, minY=Double.MAX_VALUE;
		double maxX=Double.MIN_VALUE, maxY=Double.MIN_VALUE;
		
		for (CyNode node : list)
		{
			View<CyNode> nodeview = networkView.getNodeView(node);
			double x = nodeview.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
			double y = nodeview.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
			double w = nodeview.getVisualProperty(BasicVisualLexicon.NODE_WIDTH);
			double h = nodeview.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT);
	
			minX = Math.min(minX, x-w/2);
			minY = Math.min(minY, y-h/2);
			maxX = Math.max(maxX, x+w/2);
			maxY = Math.max(maxY, y+h/2);
		}
		return new Rectangle2D.Double(minX, minY,maxX-minX, maxY-minY);
	}
	
//-------------------------------------------------------------------------------
	public void layout()
	{
		for (LegendCandidate candidate : candidates)
			candidate.extract();			
		if (legendPanel != null)
			legendPanel.extract();
				
//		int X = 500;			// starting point
//		int Y = 500;
		
		Rectangle2D.Double bounds= bounds();	
		boolean showBounds = false;
		if (showBounds)
		{
			Object[] args = { "x", bounds.getX(), "y", bounds.getY() , "width", bounds.getWidth(), "height", bounds.getHeight(),  "shapeType" , "Rectangle"};
			Map<String,String> sstrs = LegendFactory.ezMap(args);
			ShapeAnnotation abox = factory.createShapeAnnotation(ShapeAnnotation.class, networkView, sstrs);
			abox.setCanvas("background");
			abox.setName("Bounding Box");
			abox.setBorderColor(Color.GREEN);
			abox.setBorderWidth(3);
			annotationMgr.addAnnotation(abox);
		}
		
		int DEFAULT_WIDTH = 500;	 
		int DEFAULT_HEIGHT = 100;
		int SPACER = 150;
		int HALFSPACE = SPACER / 2;
		int LINE_HEIGHT = 30;

		final int left = (int) bounds.getX() ;
		final int top = (int) bounds.getY();
		final int right = left + (int) bounds.getWidth();
		final int bottom = top + (int) bounds.getHeight();
		final int startX = layoutVertically ? (right + HALFSPACE + 12) : left;
		final int startY = layoutVertically ? top : (bottom + HALFSPACE + 12);
		int x = startX;
		int y = startY;
		int dX = layoutVertically ? 0 : 500;
		int dY = layoutVertically ? 500 : 0;			
		
		if (title.length() > 0)
		{
			Object[] textArgs = { "x", x , "y", y, "width", DEFAULT_WIDTH, "height", LINE_HEIGHT, "text", title,  "fontSize", 24, "fontFamily", "Serif" };
			Map<String,String> strs = LegendFactory.ezMap(textArgs);
			TextAnnotation textBox = factory.createTextAnnotation(TextAnnotation.class, networkView, strs);
			textBox.setCanvas("background");
			textBox.setName(title);
			annotationMgr.addAnnotation(textBox);
			y += LINE_HEIGHT;
		
		}
		if (subtitle.length() > 0)
		{
			Object[] textArgs = { "x", x , "y", y, "width", DEFAULT_WIDTH, "height", LINE_HEIGHT, "text", subtitle,  "fontSize", 14, "fontFamily", "SansSerif" };
			Map<String,String> strs = LegendFactory.ezMap(textArgs);
			TextAnnotation textBox = factory.createTextAnnotation(TextAnnotation.class, networkView, strs);
			textBox.setCanvas("background");
			textBox.setName(subtitle);
			annotationMgr.addAnnotation(textBox);
			y += LINE_HEIGHT;
		}
		if (title.length() > 0 || subtitle.length() > 0)  
			y += LINE_HEIGHT;
		
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
			if (verbose) 
				System.out.println("The attribute " + fn.getMappingColumnName() + " (" + 
					type + ") is shown with " + dispName + " with a " + mapType + " function");
			
			if (mapType.contains("Continuous"))
				factory.addContinuousMapLegend((ContinuousMapping<?, ?>) fn, x, y, size);
			else if (mapType.contains("Discrete"))
				addDiscreteMapLegend((DiscreteMapping<?, ?>) fn, x, y, size);		

			if (layoutVertically) 	{  	dX = 0; dY = size.height + SPACER; }		// increment Y
			else 					{	dY = 0; dX = size.width + SPACER; }		// increment X

			x += dX;
			y += dY;
		}
		int totalWidth = x - startX;
		int totalHeight = y  - startY;
		if (layoutVertically)	totalWidth += DEFAULT_WIDTH + SPACER;
		else 					totalHeight += DEFAULT_HEIGHT + SPACER;
		
		
		if (borderBox)
		{
			Object[] boxArgs = { "x", startX-HALFSPACE, "y", startY-HALFSPACE , "width", totalWidth, "height", totalHeight,  "shapeType" , "Rectangle"};
			Map<String,String> strs = LegendFactory.ezMap(boxArgs);
			ShapeAnnotation lineBox = factory.createShapeAnnotation(ShapeAnnotation.class, networkView, strs);
			lineBox.setCanvas("background");
			lineBox.setName("Bounding Box");
			annotationMgr.addAnnotation(lineBox);
		}
//		networkView.refresh();
//		networkView.fitContent();
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
		VisualProperty<?> prop = fn.getVisualProperty();
		String dispName = prop.getDisplayName();
		GroupAnnotation legend = null;
		String columnName = fn.getMappingColumnName();
		Map<NodeShape, CyNode> used = ModelUtil.getUsedShapes(networkView);
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
				
				if (shape != null && shapeIsUsed(shape, used))			// && typeIsUsed(s, types)
				{
					shapeList.add(shape);
					objectNames.add(s);
					if (verbose ) System.out.println("using shape: " + shape + " for " + s);
				}
			}
			
			ShapeType[] shapes = shapeList.toArray(new ShapeType[0]);
			String[] oNames = objectNames.toArray(new String[0]);
//			System.out.println("using shapes: " + shapes);
//			System.out.println("with names: " + oNames);
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
	/* 
	 * A data structure to map functions that the user might want in a legend.
	 * It has a custom compareTo function so that Node properties will be sorted ahead of Edge properties
	 */
	
	
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

  //	temporary actions to clear out the annotations during development
	public void selectAllAnnotations() {
		List<Annotation> annos = annotationMgr.getAnnotations(networkView);
		for (Annotation an : annos)
			an.setSelected(true);
	}

	public void clearAnnotations() {
		if (annotationMgr == null || networkView == null)
			System.err.println("annotationMgr:  " + annotationMgr + "  networkView: " + networkView);
		else for (Annotation a: annotationMgr.getAnnotations(networkView)) {
		    if (a == null) continue;
		    annotationMgr.removeAnnotation(a);
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

		if (borderBox)
		{
			int DEFAULT_WIDTH = 550;	
			int DEFAULT_HEIGHT = 450;
			int totalWidth = X - startX;
			int totalHeight = Y - startY;
			if (layoutVertically)	totalWidth += DEFAULT_WIDTH + SPACER;
			else 					totalHeight += DEFAULT_HEIGHT + SPACER ;

			Object[] boxArgs = { "x", startX - HALFSPACE, "y", startY - HALFSPACE , 
					"width", totalWidth, "height", totalHeight,  "shapeType" , "Rectangle",  "name" , "Border"};
			Map<String,String> strs = LegendFactory.ezMap(boxArgs);
			ShapeAnnotation lineBox = factory.createShapeAnnotation(ShapeAnnotation.class, networkView, strs);
			lineBox.setCanvas("BACKGROUND");
			annotationMgr.addAnnotation(lineBox);
		};
		networkView.updateView();
		
		ModelUtil.dump(networkView);
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
	
}
