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
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
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

public class LegendController implements CytoPanelComponentSelectedListener, SetCurrentNetworkListener {

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
		factory = new LegendFactory(registrar, this);

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
		network = cyApplicationManager.getCurrentNetwork();
		networkView = cyApplicationManager.getCurrentNetworkView();
		legendPanel.setCurrentNetwork();
		
		// Now we cruise thru the list of node, then edge attributes looking for mappings.  Each mapping may potentially be a legend entry.
		VisualMappingManager manager = (VisualMappingManager) registrar.getService( VisualMappingManager.class);
		VisualStyle style = manager.getCurrentVisualStyle();
//		System.out.println("style: " + style.getTitle());
		findLegendCandidates(style);
		legendPanel.resetOptionsPanel();
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
			VisualProperty<?> vp = fn.getVisualProperty();
			if (vp.toString().contains("EDGE")) continue;
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
		networkView = cyApplicationManager.getCurrentNetworkView();
		for (LegendCandidate candidate : candidates)
			candidate.extract();			
		if (legendPanel != null)
			legendPanel.extract();
				
//		int X = 500;			// starting point
//		int Y = 500;
		
		 Map<NodeShape, CyNode> allShapes = MapBuilder.getUsedShapes(networkView);
		 String shapeName = "Ellipse";
		 if (!allShapes.isEmpty())
		 {
			 NodeShape sh = allShapes.keySet().iterator().next(); 
			 shapeName = sh.getDisplayName();
		 }
		Rectangle2D.Double bounds= bounds();	
//		boolean showBounds = false;
//		if (showBounds)
//		{
//			Object[] args = { "x", bounds.getX(), "y", bounds.getY() , "width", bounds.getWidth(), "height", bounds.getHeight(),  "shapeType" , "Rectangle"};
//			Map<String,String> sstrs = LegendFactory.ezMap(args);
//			ShapeAnnotation abox = factory.createShapeAnnotation(ShapeAnnotation.class, networkView, sstrs);
//			abox.setCanvas("background");
//			abox.setName("Bounding Box");
//			abox.setBorderColor(Color.GREEN);
//			abox.setBorderWidth(3);
//			annotationMgr.addAnnotation(abox);
//		}
		
		int DEFAULT_WIDTH = 500;	 
		int DEFAULT_HEIGHT = 100;
		int SPACER = 150;
		int HALFSPACE = SPACER / 2;
		int LINE_HEIGHT = 50;

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
			Object[] textArgs = { "x", x , "y", y, "width", DEFAULT_WIDTH, "height", LINE_HEIGHT, "text", title,  "fontSize", 48, "fontFamily", "Serif" };
			Map<String,String> strs = LegendFactory.ezMap(textArgs);
			TextAnnotation textBox = factory.createTextAnnotation(TextAnnotation.class, networkView, strs);
			textBox.setCanvas("background");
			textBox.setName("legend");
			annotationMgr.addAnnotation(textBox);
			y += LINE_HEIGHT;
		
		}
		if (subtitle.length() > 0)
		{
			Object[] textArgs = { "x", x , "y", y, "width", DEFAULT_WIDTH, "height", LINE_HEIGHT, "text", subtitle,  "fontSize", 24, "fontFamily", "SansSerif" };
			Map<String,String> strs = LegendFactory.ezMap(textArgs);
			TextAnnotation textBox = factory.createTextAnnotation(TextAnnotation.class, networkView, strs);
			textBox.setCanvas("background");
			textBox.setName("legend");
			annotationMgr.addAnnotation(textBox);
			y += LINE_HEIGHT;
		}
		if (title.length() > 0 || subtitle.length() > 0)  
			y += LINE_HEIGHT;
		
		int maxWidth = 0; 
		int maxHeight = 0;
		for (LegendCandidate candidate : candidates)
		{
			if (!candidate.isVisible())   // check box not selected
			{
				if (verbose) System.out.println("hidden");
				continue;
			}
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
			
			boolean added = false;
			if (mapType.contains("Continuous"))
				added = factory.addContinuousMapLegend((ContinuousMapping<?, ?>) fn, x, y, size, shapeName);
			else if (mapType.contains("Discrete"))
				added = addDiscreteMapLegend((DiscreteMapping<?, ?>) fn, x, y, size);		

			if (added)
			{
				if (layoutVertically) 	{  	dX = 0; dY = size.height + SPACER; }	// increment Y
				else 					{	dY = 0; dX = size.width + SPACER; }		// increment X

				x += dX;
				y += dY;
				maxWidth = Math.max(maxWidth, size.width);
				maxHeight = Math.max(maxHeight, size.height);
				if (verbose) 	System.out.println("xy: " + x + ", " + y + " = [" + size.width + " x " + size.height + "] " +
						mapType + " " + (x - startX) + ", " + (y  - startY) + " {" + maxWidth + ", " + maxHeight + "}");		
			}
		}
		int totalWidth = x - startX;
		int totalHeight = y  - startY;
		if (layoutVertically)	totalWidth += Math.max(DEFAULT_WIDTH + SPACER, maxWidth);
		else 					totalHeight += Math.max(DEFAULT_HEIGHT + SPACER, maxHeight);
		if (verbose) 	System.out.println("Legend Dim: " + totalWidth + " X " + totalHeight );		

		
		if (borderBox)
		{
			if (verbose) 	System.out.println("borderBox: " + totalWidth + " x " + totalHeight );		
			Object[] boxArgs = { "x", startX-HALFSPACE, "y", 20 + startY-HALFSPACE , "width", totalWidth, "height", totalHeight * 1.1 ,  "shapeType" , "Rectangle"};
			Map<String,String> strs = LegendFactory.ezMap(boxArgs);
			ShapeAnnotation lineBox = factory.createShapeAnnotation(ShapeAnnotation.class, networkView, strs);
			lineBox.setCanvas("background");
			lineBox.setName("legend");
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

	private boolean addDiscreteMapLegend(DiscreteMapping<?, ?> fn, int x, int y, Dimension outSize) 
	{
		VisualProperty<?> prop = fn.getVisualProperty();
		String dispName = prop.getDisplayName();
		GroupAnnotation legend = null;
		String columnName = fn.getMappingColumnName();
		Map<NodeShape, CyNode> used = MapBuilder.getUsedShapes(networkView);
		if ("Node Shape".equals(dispName))
		{
			List<String> objectNames = new ArrayList<String>();
			List<ShapeType> shapeList = new ArrayList<ShapeType>();
			Map<?,?> set = fn.getAll();
			for (Object key : set.keySet())
			{
				String s = key.toString();
				Object val = set.get(key);
				String name = val.toString();
				ShapeType shape = getShapeType(name);
				
				if (shape != null && shapeIsUsed(shape, used))			// && typeIsUsed(s, types)
				{
					shapeList.add(shape);
					objectNames.add(s);
					if (verbose ) System.out.println("using shape: " + shape + " for " + s + " with name " + name);
				}
			}
			String title = dispName + ":  " + columnName;			
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
				Object val = set.get(key);
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
//		else if ("Edge Line Type".equals(dispName))
//		{
//			List<String> objectNames = new ArrayList<String>();
//			List<LineType> types = new ArrayList<LineType>();
//			Map<?,?> set = fn.getAll();
//			for (Object key : set.keySet())
//			{
//				String s = key.toString();
//				Object val = set.get(s);
//				if (val instanceof LineType)
//				{
//					LineType type = (LineType) val;
//					if (type != null)
//					{
//						types.add(type);
//						objectNames.add(s);
//					}
//				}
//			}
//			LineType[] linetypes = types.toArray(new LineType[0]);
//			String[] oNames = objectNames.toArray(new String[0]);
//			legend = factory.addLinetypeLegend(dispName, x, y, outSize, linetypes, oNames);
//		}
//		else if ("Target Arrow".equals(dispName))
//		{
//			legend = factory.addArrowheadLegend(dispName, x, y, outSize, arrowheads, arrowNames);
//		}
		outSize.width += factory.getRightMargin();
		
		if (legend != null)
			annotationMgr.addAnnotation(legend);
		return legend != null;
		
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
	 * Each entry is mapping function (eg: map degree -> size) and a checkbox to determine if its included
	 * 
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
	  boolean isVisible()						{ 	return checkBox != null && checkBox.isSelected() ;	}
	  
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
		    if ("legend".equals(a.getName()))
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

	int X = 200;			// starting point
	int Y = layoutVertically ? 500 : 0;
	int SPACER = 120;
	int HALFSPACE = SPACER / 2;

	public void testAnnotations() 
	{	
		initialize();
		X = 200;			// starting point
		Y = layoutVertically ? 500 : 0;
		if (legendPanel != null)
			legendPanel.extract();
		
		int startX = X - SPACER;
		int startY = Y - SPACER;
		int W = 500;
		int H = 100;
//		int dX = layoutVertically ? 0 : W + SPACER;
//		int dY = layoutVertically ? H + SPACER : 0;			

		GroupAnnotation gradientLegend = factory.addGradientLegend("Fill Color: layout.degree ",  X, Y, W, H, -2, 3, colors, stops);
		annotationMgr.addAnnotation(gradientLegend);
		advance(W,H);
		
		Dimension legendSize = new Dimension( 200, 500);
		GroupAnnotation discreteLegend = factory.addDiscreteColorLegend("Fill Color: Type", X, Y, legendSize, discreteColors, names);
		annotationMgr.addAnnotation(discreteLegend);
		advance(legendSize);
	
		W = 500;
		H = 200;
		GroupAnnotation continuousLegend = factory.addRampLegend("Node Size: Expression", X, Y, W, H, null, Color.LIGHT_GRAY);
		annotationMgr.addAnnotation(continuousLegend);
		advance(W,H);
	
//		legendSize = new Dimension( 300, 300);
//		GroupAnnotation linetypeLegend = factory.addLinetypeLegend("Line Type: Function", X, Y, legendSize, LINETYPES, LINETYPE_NAMES);
//		annotationMgr.addAnnotation(linetypeLegend);
//		advance(legendSize);
//
//		legendSize = new Dimension( 300, 500);
//		GroupAnnotation arrowLegend =  factory.addArrowheadLegend("Arrowhead: Interaction Type", X, Y, legendSize, arrowNames, arrowheads);
//		annotationMgr.addAnnotation(arrowLegend);
//		advance(legendSize);

		legendSize = new Dimension( 200, 500);
		GroupAnnotation shapeLegend = factory.addShapeLegend("Node Shape: Protein Type ", X, Y, legendSize, shapes, shapenames);
		annotationMgr.addAnnotation(shapeLegend);
		advance(legendSize);

		GroupAnnotation fontSizeLegend =  factory.addFontSizeLegend("Font Size: degree", X, Y, legendSize, 10, 50, Color.cyan);
		annotationMgr.addAnnotation(fontSizeLegend);
		advance(legendSize);

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
		
		MapBuilder.dump(networkView);
	}	

	  private void advance(Dimension dim) {		  advance(dim.width, dim.height);		}
	  private void advance(int w, int h) {
			X += layoutVertically ? 0 : w + SPACER;  	
			Y += layoutVertically ? h + SPACER : 0;	
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
	@Override
	public void handleEvent(SetCurrentNetworkEvent e) {

		network = e.getNetwork();
		if (cyApplicationManager != null)
		{
			networkView = cyApplicationManager.getCurrentNetworkView();
			scanNetwork();
		}
	}
	
}
