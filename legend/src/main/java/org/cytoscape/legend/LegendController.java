package org.cytoscape.legend;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.DiscreteRange;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.annotations.ArrowAnnotation;
import org.cytoscape.view.presentation.annotations.ArrowAnnotation.AnchorType;
import org.cytoscape.view.presentation.annotations.ArrowAnnotation.ArrowEnd;
import org.cytoscape.view.presentation.annotations.GroupAnnotation;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation.ShapeType;
import org.cytoscape.view.presentation.annotations.TextAnnotation;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.LineType;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.ContinuousMappingPoint;
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
	private AnnotationManager annotationMgr;
	private CyNetwork network;
	private CyNetworkView networkView;
	private LegendPanel legendPanel;
	private AnnotationFactory<ShapeAnnotation> shapeFactory;
	private AnnotationFactory<TextAnnotation> textFactory;
	private AnnotationFactory<GroupAnnotation> groupFactory;
	private AnnotationFactory<ArrowAnnotation> arrowFactory;
	
	public void setLegendPanel(LegendPanel p) { legendPanel = p; }
	//----------------------------------------------------------
	private void initialize()
	{
//		if (cyApplicationManager != null) return;
		cyApplicationManager = registrar.getService(CyApplicationManager.class);
		network = cyApplicationManager.getCurrentNetwork();
		networkView = cyApplicationManager.getCurrentNetworkView();

		annotationMgr = registrar.getService(AnnotationManager.class);
		if (annotationMgr == null) 
			System.err.println("AnnotationManager is null");
		shapeFactory =  (AnnotationFactory<ShapeAnnotation>)registrar.getService( AnnotationFactory.class,"(type=ShapeAnnotation.class)");
		if (shapeFactory == null) 
			System.err.println("shapeFactory is null");
		arrowFactory =  (AnnotationFactory<ArrowAnnotation>)registrar.getService( AnnotationFactory.class,"(type=ArrowAnnotation.class)");
		if (arrowFactory == null) 
			System.err.println("arrowFactory is null");
		textFactory =  (AnnotationFactory<TextAnnotation>)registrar.getService( AnnotationFactory.class,"(type=TextAnnotation.class)");
		if (textFactory == null) 
			System.err.println("textFactory is null");
		groupFactory =  (AnnotationFactory<GroupAnnotation>)registrar.getService( AnnotationFactory.class,"(type=GroupAnnotation.class)");
		if (groupFactory == null) 
			System.err.println("groupFactory is null");
		
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
	public void setDrawBorder(boolean show)	{ borderBox = show;	}
	
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
		int SPACER = 30;
		
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
				addContinuousMapLegend((ContinuousMapping<?, ?>) fn, X, Y, size);
			}
			else if (mapType.contains("Discrete"))
			{
				addDiscreteMapLegend((DiscreteMapping<?, ?>) fn, X, Y, size);
					
			}
			if (layoutVertically) { 	dY = 0; dX = size.width + SPACER;  }
			else 				{	dX = 0; dY = size.height + SPACER; }

			X += dX;
			Y += dY;
		}
		int totalWidth = X - startX;
		int totalHeight = Y - startY;
		if (borderBox)
		{
			Object[] boxArgs = { "x", startX, "y", startY , "width", totalWidth, "height", totalHeight,  "shapeType" , "Rectangle"};
			Map<String,String> strs = ezMap(boxArgs);
			ShapeAnnotation lineBox = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, strs);
			annotationMgr.addAnnotation(lineBox);
		}
//		networkView.refresh();
	}

	private void addDiscreteMapLegend(DiscreteMapping<?, ?> fn, int x, int y, Dimension outSize) 
	{
//		boolean orientVertically = false;
//		int width = orientVertically ? SHORT_SIDE : LONG_SIDE;
//		int height = orientVertically ? LONG_SIDE : SHORT_SIDE;
		VisualProperty<?> prop = fn.getVisualProperty();
		String dispName = prop.getDisplayName();
		Color[] discreteColors = { Color.BLACK, Color.BLUE, Color.RED, Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW };
		String[] names= { "Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel" };
		GroupAnnotation discreteLegend = addDiscreteColorLegend(dispName, x, y, outSize, discreteColors, names);
		annotationMgr.addAnnotation(discreteLegend);
		
	}
	int SHORT_SIDE = 100;
	int LONG_SIDE = 400;

	private BoundaryRangeValues<Double> getFunctionRange(ContinuousMapping<?, ?> continFn)
	{
		Object pts = continFn.getAllPoints();
		List<ContinuousMappingPoint<?, ?>> list = (List<ContinuousMappingPoint<?, ?>>) pts;
		double minimum = Double.MAX_VALUE;
		double maximum = Double.MIN_VALUE;
		for (ContinuousMappingPoint<?, ?> pt : list)
		{
			Double v = (Double) pt.getValue();
			if (v instanceof Double)
			{
				minimum = Math.min(minimum, (Double) v);
				maximum = Math.max(maximum, (Double) v);
			}
		}
		BoundaryRangeValues<Double> range = new BoundaryRangeValues<Double>(minimum,maximum,maximum);
		return range;
		
	}
	private void addContinuousMapLegend(ContinuousMapping<?, ?> continFn, int X, int Y, Dimension ioSize) {
		
//		boolean orientVertically = false;
		int width = ioSize.width;
		int height = ioSize.height;
//		ioSize.setSize(width, height);
		Object v = null;
		VisualProperty<?> prop = continFn.getVisualProperty();
		String dispName = prop.getDisplayName();
		BoundaryRangeValues<Double> range = getFunctionRange(continFn);
		double minimum = range.lesserValue;
		double maximum = range.greaterValue;
		v = prop.getDefault();
		String title = continFn.getMappingColumnName() + "  -->  " + dispName;
		System.out.print(title + " has range (" + minimum + "  - " + maximum + ") "); 					
		System.out.println(title + " and is of type " + getType(v)); 					
		
		GroupAnnotation legend = null;
		if (v instanceof Color)
			legend = addGradientLegend(title, X, Y, width, height, continFn);
		else if (isFontSize(v))
			legend = addFontSizeLegend(title, X, Y, ioSize, minimum, maximum, Color.LIGHT_GRAY);
		else if (isNumeric(v))
			legend = addTrapezoidLegend(title, X, Y, width, height, minimum, maximum, Color.LIGHT_GRAY);
		
		if (legend != null)
			annotationMgr.addAnnotation(legend);
	}
	
	
	//----------- Legend Subtypes ----------------------------------------
	//------------------------------------------------------------------
	private GroupAnnotation addDiscreteColorLegend(String title, int x, int y, Dimension ioSize, Color[] colors, String[] names)
	{
		boolean orientVertically = true;
	int NCELLS = colors.length;
	float MARGIN = 10;
	float LINE_HEIGHT = 25;
	float SWATCH_HEIGHT = LINE_HEIGHT + MARGIN;;
	float CELL_HEIGHT = SWATCH_HEIGHT + MARGIN;
	float SWATCH_WIDTH = 80;
	float CELL_WIDTH = SWATCH_WIDTH + MARGIN;
	float LABEL_WIDTH = 100;
	
	float width =  2 * MARGIN + (orientVertically ? (CELL_WIDTH + LABEL_WIDTH) : (CELL_WIDTH * NCELLS));
	float height = 2 * MARGIN +  (orientVertically ? (CELL_HEIGHT * NCELLS) : (MARGIN + CELL_HEIGHT));
	ioSize.setSize(width, height);

	// Construct the annotations to add to the diagram

		int NLINES = colors.length;

		GroupAnnotation group = createGroupWithHeader(title, x, y, (int) width, (int) height);
		addBorderBox(group, x, y, (int) width, (int) height);
		
		for (int i=0; i < NLINES; i++)
		{
			float xx = MARGIN + (orientVertically ?  0 : (i * CELL_WIDTH));
			float yy = MARGIN + (orientVertically ?  (i * CELL_HEIGHT) : 0);
			Object[] swatchArgs = { "x", x + xx, "y", y + yy , "width", SWATCH_WIDTH, "height", SWATCH_HEIGHT,  "shapeType" , "Rectangle"};
			Map<String,String> strs = ezMap(swatchArgs);
			ShapeAnnotation lineBox = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, strs);
			lineBox.setFillColor(colors[i]);
//			lineBox.setCanvas("background");
//			annotationMgr.addAnnotation(lineBox);
			group.addMember(lineBox);
//			lineBox.moveAnnotation(new Point2D.Double(MARGIN,yy));

			float dx = xx + (orientVertically ? CELL_WIDTH + MARGIN :  MARGIN);
			float dy = yy + (orientVertically ? CELL_HEIGHT /2f : CELL_HEIGHT);
			Object[] textArgs = { "x", x + dx , "y", y + dy, "width", width - xx, "height", LINE_HEIGHT, "text", names[i]};
			strs = ezMap(textArgs);
			TextAnnotation textBox = textFactory.createAnnotation(TextAnnotation.class, networkView, strs);
//			textBox.setCanvas("background");
//			annotationMgr.addAnnotation(textBox);
			group.addMember(textBox);
//			textBox.moveAnnotation(new Point2D.Double(COL1WIDTH + 2 * MARGIN,yy + TEXT_OFFSET_Y + (LINE_HEIGHT / 2)));
		}
		group.setCanvas("background");
		annotationMgr.addAnnotation(group);
		
		return group;
	}
	//------------------------------------------------------------------
	// discrete
	
	private GroupAnnotation addShapeLegend(String title, int x, int y, Dimension ioSize, ShapeType[] shapes, String[] names)
	{
		int MARGIN = 10;			
		int NCELLS = shapes.length;
		int CELL_HEIGHT = 80;
		int CELL_WIDTH = 80;
		int LABEL_HEIGHT = 30;
		int LABEL_WIDTH = 100;
		
		boolean orientVertically = true;
		int width =  2 * MARGIN + (orientVertically ? (CELL_WIDTH + LABEL_WIDTH) : (CELL_WIDTH * NCELLS));
		int height = 2 * MARGIN +  (orientVertically ? (CELL_HEIGHT * NCELLS) : (CELL_HEIGHT + LABEL_HEIGHT));
		ioSize.setSize(width, height);


		GroupAnnotation group = createGroupWithHeader(title, x, y, width, height + MARGIN);
		addBorderBox(group, x, y, (int) width, (int) height + MARGIN);
		
		for (int i=0; i < NCELLS; i++)
		{
			float xx = MARGIN + (orientVertically ? 0 : (i * CELL_HEIGHT));
			float yy = MARGIN + (orientVertically ? (i * CELL_HEIGHT) + 20 : 0);
			float siz = CELL_HEIGHT - MARGIN;
			Object[] swatchArgs = { "x", x + xx, "y", y + yy , "width", siz, "height", siz,  "shapeType" , shapes[i].name() };
			Map<String,String> strs = ezMap(swatchArgs);
			ShapeAnnotation lineBox = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, strs);
			lineBox.setFillColor(Color.BLUE);
			lineBox.setCanvas("background");
//			annotationMgr.addAnnotation(lineBox);
			group.addMember(lineBox);

			int halfStringWidth = 6 * names[i].length();
			float HALF_CELL_HEIGHT = CELL_HEIGHT / 2.0f;
			float dx = xx + (orientVertically ? (MARGIN + CELL_HEIGHT) : HALF_CELL_HEIGHT-halfStringWidth);
			float dy = yy + (orientVertically ? HALF_CELL_HEIGHT  : CELL_HEIGHT);
			Object[] textArgs = { "x", x + dx, "y", y + dy, "width", 200, "height", CELL_HEIGHT, "text", names[i]};
			strs = ezMap(textArgs);
			TextAnnotation textBox = textFactory.createAnnotation(TextAnnotation.class, networkView, strs);
//			textBox.setCanvas("background");
//			annotationMgr.addAnnotation(textBox);
			group.addMember(textBox);
//			textBox.moveAnnotation(new Point2D.Double(COL1WIDTH + 2 * MARGIN,yy + TEXT_OFFSET_Y + (LINE_HEIGHT / 2)));
		}
		return group;
	}
	
	//------------------------------------------------------------------
	// discrete
	private GroupAnnotation addLinetypeLegend(String title, int x, int y, Dimension ioSize, LineType[] strokes, String[] names)
	{
		//TODO
		int NCELLS = strokes.length;
		int MARGIN = 10;
		int LINE_HEIGHT = 25;
		int SWATCH_HEIGHT = LINE_HEIGHT + MARGIN;;
		int CELL_HEIGHT = SWATCH_HEIGHT + MARGIN;
		int SWATCH_WIDTH = 120;
		int CELL_WIDTH = SWATCH_WIDTH + MARGIN;
		int LABEL_WIDTH = 100;
		int LABEL_HEIGHT = LINE_HEIGHT + MARGIN;

		boolean orientVertically = true;
		int width =  2 * MARGIN + (orientVertically ? (CELL_WIDTH + LABEL_WIDTH) : (CELL_WIDTH * NCELLS));
		int height = 2 * MARGIN +  (orientVertically ? (CELL_HEIGHT * NCELLS) : (CELL_HEIGHT + LABEL_HEIGHT));
		ioSize.setSize(width, height);
	
		GroupAnnotation group = createGroupWithHeader(title, x, y, width, height);
		addBorderBox(group, x, y,  width, height);
		
		int i = 0;
		for (LineType type : strokes)
		{
			float xx = MARGIN + (orientVertically ? MARGIN : (i * CELL_WIDTH));
			float yy = MARGIN + (orientVertically ? (i * CELL_HEIGHT) + 20 : 0);
			Object[] dotArgs = { "x", x + xx, "y", y + yy,  "width", "2",  "height", "2","shapeType", "Ellipse","fillColor", "0" };
			ShapeAnnotation src = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, ezMap(dotArgs));
			src.moveAnnotation(new Point2D.Double(x + xx,y + yy));
//			src.setCanvas("background");
//			annotationMgr.addAnnotation(src);
			group.addMember(src);
			Object[] targArgs = { "x", x + xx + SWATCH_WIDTH, "y", y + yy,  "width", 2,  "height", 2,"shapeType", "Rectangle","fillColor", 0 };
			ShapeAnnotation targ = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, ezMap(targArgs));
			targ.setCanvas("background");
			annotationMgr.addAnnotation(targ);
			group.addMember(targ);

			float dx = xx + (orientVertically ? CELL_WIDTH : 0);
			float dy = yy + (orientVertically ? 0  : CELL_HEIGHT);
			Object[] textArgs = { "x", x + dx, "y", y + dy,  "width", 40,  "height", 20, "fontSize", 12, "fontFamily", "Courier" };
			TextAnnotation text = textFactory.createAnnotation(TextAnnotation.class, networkView, ezMap(textArgs));
//			text.setCanvas("background");
			text.setText(type.getDisplayName());
//			annotationMgr.addAnnotation(text);
			group.addMember(text);

			boolean passArgs = true;
			String[] arrowArgs = { "lineThickness", "8", "edgeLineStyle", "Dash Dot" };   //, "edgeLineStyle", type.getSerializableString()
			Map<String, String> argv = passArgs ? ezMap(arrowArgs) : null;
			ArrowAnnotation arrow = arrowFactory.createAnnotation(ArrowAnnotation.class, networkView, argv);  //
			arrow.setSource(src);
			arrow.setTarget(targ);
			arrow.setLineWidth(6);
			arrow.setLineColor(discreteColors[i++]);
			arrow.setAnchorType(ArrowEnd.SOURCE, AnchorType.CENTER);
			arrow.setAnchorType(ArrowEnd.TARGET, AnchorType.CENTER);
//			annotationMgr.addAnnotation(arrow);
//			arrow.setCanvas("background");
			group.addArrow(arrow);
		}
		return group;
	}
	//------------------------------------------------------------------
	// discrete
	private GroupAnnotation addArrowheadLegend(String title, int x, int y, Dimension ioSize, String[] arrows, String[] names)
	{
		//TODO
		int NCELLS = arrows.length;
		int MARGIN = 10;
		int LINE_HEIGHT = 25;
		int SWATCH_HEIGHT = LINE_HEIGHT + MARGIN;;
		int CELL_HEIGHT = SWATCH_HEIGHT + MARGIN;
		int SWATCH_WIDTH = 120;
		int CELL_WIDTH = SWATCH_WIDTH + MARGIN;
		int LABEL_WIDTH = 100;
		int LABEL_HEIGHT = LINE_HEIGHT + MARGIN;

		boolean orientVertically = true;
		int width =  2 * MARGIN + (orientVertically ? (CELL_WIDTH + LABEL_WIDTH) : (CELL_WIDTH * NCELLS));
		int height = 2 * MARGIN +  (orientVertically ? (CELL_HEIGHT * NCELLS) : (CELL_HEIGHT + LABEL_HEIGHT));
		ioSize.setSize(width, height);
		GroupAnnotation group = createGroupWithHeader(title, x, y, width, height);
		addBorderBox(group, x, y, width, height);
		
		for (int i = 0; i < arrows.length; i++)
		{
			float xx = MARGIN + (orientVertically ? 0 : (i * CELL_WIDTH));
			float yy = MARGIN + (orientVertically ? (i * CELL_HEIGHT) + 20 : 0);
			String type = arrows[i];
			String name = names[i];
			
			Object[] dotArgs = { "x", x + xx, "y", y + yy,  "width", 2,  "height", 2, "shapeType", "Ellipse","fillColor", "0" };
			ShapeAnnotation src = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, ezMap(dotArgs));
//			src.setCanvas("background");
//			annotationMgr.addAnnotation(src);
			group.addMember(src);
			
			Object[] targArgs = { "x", x + xx + SWATCH_WIDTH, "y",  y + yy,  "width", 2,  "height", 2,"shapeType", "Rectangle","fillColor", 0 };
			ShapeAnnotation targ = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, ezMap(targArgs));
//			targ.setCanvas("background");
//			annotationMgr.addAnnotation(targ);
			group.addMember(targ);

			float dx = xx + (orientVertically ? CELL_WIDTH : 0);
			float dy = yy + (orientVertically ? 0  : CELL_HEIGHT);
			Object[] textArgs = { "x", x + dx , "y", y + dy,  "width", 40,  "height", 20, "fontSize", 12, "fontFamily", "Courier" };
			TextAnnotation text = textFactory.createAnnotation(TextAnnotation.class, networkView, ezMap(textArgs));
//			text.setCanvas("background");
			text.setText(name);
//			annotationMgr.addAnnotation(text);
			group.addMember(text);

			boolean passArgs = true;
			Object[] arrowArgs = { "lineThickness", "8", "edgeLineStyle", "Dots" };   //, "edgeLineStyle", type.getSerializableString()
			Map<String, String> argv = passArgs ? ezMap(arrowArgs) : null;
			ArrowAnnotation arrow = arrowFactory.createAnnotation(ArrowAnnotation.class, networkView, argv);  //
			arrow.setSource(src);
			arrow.setTarget(targ);
			arrow.setLineWidth(6);
			arrow.setLineColor(discreteColors[i]);
			arrow.setArrowType(ArrowEnd.SOURCE, "none");
			arrow.setArrowType(ArrowEnd.TARGET, type);
//			annotationMgr.addAnnotation(arrow);
//			arrow.setCanvas("background");
			group.addArrow(arrow);
		}
		return group;
	}
	//------------------------------------------------------------------
	// continuous
	
	private GroupAnnotation addTrapezoidLegend(String title, int x, int y, int w, int h, double min, double max, Color color)
	{
		GroupAnnotation group = createGroupWithHeader( title,  x,  y,  w,  h);
		addBorderBox(group, x, y, w, h);

		GeneralPath path = new GeneralPath();
		path.moveTo(x, y);
		path.lineTo(x + w * 0.2,y);
		path.lineTo(x + w, y + h);
		path.lineTo(x, y + h);
		path.lineTo(x, y);
        path.closePath();
		
        Map<String,String> trapezoidArgs = new HashMap<String,String>();
		trapezoidArgs.put("x", "" + x);
		trapezoidArgs.put("y", "" + y);
		trapezoidArgs.put("width", "" + w);
		trapezoidArgs.put("height", "" + h);
		ShapeAnnotation trapezoid = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, trapezoidArgs);
		trapezoid.setShapeType(ShapeType.CUSTOM.name());
		trapezoid.setCustomShape(path);
		trapezoid.setFillColor(color);
		trapezoid.setBorderColor(Color.DARK_GRAY);
		trapezoid.setBorderWidth(1);
	
//		trapezoid.setCanvas("background");
//		annotationMgr.addAnnotation(trapezoid);
		group.addMember(trapezoid);		
		group.setCanvas("background");
		addTicks(x,y,w,h,min, max, group, false);
		return group;

	}	
	//------------------------------------------------------------------
	// continuous

	private GroupAnnotation addFontSizeLegend(String title, int x, int y, Dimension inSize, double min, double max, Color color)
	{
		int w = inSize.width;  
		int h = inSize.height; 
		GroupAnnotation group = createGroupWithHeader( title,  x,  y,  w,  h);
		addBorderBox(group, x, y, w, h);
		
		String[] text = { "smallest", "small", "median", "large", "largest"};
		int[] sizes = {10, 20, 30, 40, 50 };
		int margin = 18;
		int lineHeight = h / text.length;
		for (int i=0; i < text.length; i++)
		{
			int xx = x + margin;
			int yy = y + i * lineHeight + margin;
			Object[] rawArgs = { "x", xx, "y", yy, "width", w, "fontSize", sizes[i], "text", text[i]};
			Map<String,String> args = ezMap(rawArgs);
			TextAnnotation sample = textFactory.createAnnotation(TextAnnotation.class, networkView, args);
//			sample.setCanvas("background");
//			annotationMgr.addAnnotation(sample);
			group.addMember(sample);		
		}
		
		addTicks(x,y,w,h,10, 50, group, true);
		return group;

	}
	//------------------------------------------------------------------
	// continuous

	private GroupAnnotation addGradientLegend(String title, int x, int y, int w, int h, ContinuousMapping<?, ?> fn)
	{
		Object pts = fn.getAllPoints();
		VisualProperty<?> prop = fn.getVisualProperty();
		String dispName = prop.getDisplayName();
		List<ContinuousMappingPoint<?, ?>> list = (List<ContinuousMappingPoint<?, ?>>) pts;
		int listsize = list.size();
		

		Object v = null;
		double minimum = 1000000;
		double maximum = -1000000;
		for (ContinuousMappingPoint<?, ?> pt : list)
		{
			v = pt.getValue();
			if (v instanceof Double)
			{
				minimum = Math.min(minimum, (Double) v);
				maximum = Math.max(maximum, (Double) v);
			}
		}
		float range = (float) maximum - (float) minimum;
		Color[] colors = new Color[listsize];
		float[] stops = new float[listsize];
		int i = 0;
		for (ContinuousMappingPoint<?, ?> pt : list)
		{
			BoundaryRangeValues<?> vals = pt.getRange();
			colors[i] =  (Color) vals.lesserValue;
			v = pt.getValue();
			if (v instanceof Double)
			{
				float value = ((Double) pt.getValue()).floatValue();
				stops[i] = (value - (float) minimum) / range;
				System.out.println(String.format("value = %3.2f stop = %3.2f", value, stops[i]));
			}
			i++;
		}
		return addGradientLegend(title, x, y, w, h, minimum, maximum, colors, stops);
	}
	
	private GroupAnnotation addGradientLegend(String title, int x, int y, int w, int h, double min, double max, Color[] colors, float[] stops)
	{
		GroupAnnotation group = createGroupWithHeader( title,  x,  y,  w,  h);
		ShapeAnnotation gradientBox = addBorderBox(group, x, y, w, h);
//		boolean orientVertically = false;

		Point2D start = new Point2D.Float(0, 0);
		Point2D end = new Point2D.Float(1f,0);
//		if (orientVertically)
//		{
//			start = new Point2D.Float(0, 0);
//			end = new Point2D.Float(0, 1);
//		}
		LinearGradientPaint p = new LinearGradientPaint(start, end, stops, colors);		
		gradientBox.setFillColor(p);
		addTicks(x,y,w,h,min, max, group, false);
		return group;
	}
	
	//--------SUBCOMPONENTS-----------------------------------------------------

	private GroupAnnotation createGroupWithHeader(String title, int x, int y, int w, int h)
	{
		String[] groupArgs = { "x", "" + x, "y", "" + y,  "width", "" + w,  "height", "" + h};
		GroupAnnotation group = groupFactory.createAnnotation(GroupAnnotation.class, networkView, ezMap(groupArgs));
		group.setCanvas("background");
		float LINE_HEIGHT = 25;
		Object[] titleArgs = { "x", x, "y", y-LINE_HEIGHT, "width", w, "height", LINE_HEIGHT, "text", title};
		Map<String,String> strs = ezMap(titleArgs);
		TextAnnotation titleBox = textFactory.createAnnotation(TextAnnotation.class, networkView, strs);
//		titleBox.setCanvas("background");
//		annotationMgr.addAnnotation(titleBox);
		group.addMember(titleBox);
		return group;
			
	}
	//------------------------------------------------------------------
	private ShapeAnnotation addBorderBox(GroupAnnotation group, int x, int y, int w, int h) {
		Map<String,String> borderArgs = new HashMap<String,String>();
		borderArgs.put("x", "" + x);
		borderArgs.put("y", "" + y);
		borderArgs.put("width", "" + w);
		borderArgs.put("height", "" + h);
		borderArgs.put("shapeType", "Rectangle");
		ShapeAnnotation borderBox = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, borderArgs);
//		borderBox.setCanvas("background");
		borderBox.moveAnnotation(new Point2D.Double(x,y));
//		annotationMgr.addAnnotation(borderBox);
		group.addMember(borderBox);				// TODO Auto-generated method stub
		return borderBox;
	}
	//------------------------------------------------------------------
	private void addTicks(int x, int y, int w, int h, double min, double max,GroupAnnotation group, boolean orientVertically)
	{
		int TICKS = 5;
		int TICKWIDTH = 10;
		int LABELWIDTH = 35;
		int LABELHEIGHT = 15;
		int FONTSIZE = 18;
		double BORDERWIDTH = 1.5;
		ShapeAnnotation[] ticks = new ShapeAnnotation[TICKS];
		TextAnnotation[] vals = new TextAnnotation[TICKS];
	
		for (int t = 0; t < TICKS; t++)
		{
			double xt = orientVertically ? (x + w) : (x + (t * w / 4));
			double yt = orientVertically ? (y + (t * h / 4.0)) : (y + h);
			if (t > TICKS / 2) yt-= BORDERWIDTH;
			
			Map<String,String> tickArgs = new HashMap<String,String>();
			tickArgs.put("x", "" + xt);
			tickArgs.put("y", "" + yt);
			tickArgs.put("width", orientVertically ? "" + TICKWIDTH : "1");
			tickArgs.put("height", orientVertically ? "1" : "" + TICKWIDTH);
			tickArgs.put("shapeType", "Rectangle");
			ticks[t] = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, tickArgs);
			ticks[t].setCanvas("background");
			ticks[t].setFillColor(Color.BLACK);
			ticks[t].setBorderWidth(BORDERWIDTH);
//			annotationMgr.addAnnotation(ticks[t]);
			group.addMember(ticks[t]);
			ticks[t].setCanvas("background");
//			textBox.moveAnnotation(new Point2D.Double(COL1WIDTH + 2 * MARGIN,yy+TEXT_OFFSET_Y));
	
			Map<String,String> tickLabels = new HashMap<String,String>();
			double val = min + ((float) t / (TICKS-1)) * (max - min);
			String formattedStr = String.format("%3.1f", val);
			tickLabels.put("text", "" + formattedStr);
			int halfStringWidth = 6 * (formattedStr.length()-1);  // TODO
			tickLabels.put("x", orientVertically ? "" + (xt  + 5 + TICKWIDTH) : ("" + (xt - halfStringWidth)));
			tickLabels.put("y", orientVertically ? "" + (yt-FONTSIZE/2.0) : ("" + (int) (yt  + 5 + TICKWIDTH)));
			tickLabels.put("width", "" + LABELWIDTH);
			tickLabels.put("height", "" + LABELHEIGHT);
			tickLabels.put("fontSize", "" + FONTSIZE);
			
			vals[t] = textFactory.createAnnotation(TextAnnotation.class, networkView, tickLabels);
//			vals[t].setCanvas("background");
//			annotationMgr.addAnnotation(vals[t]);
			group.addMember(vals[t]);
			
		}
	}
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
		
		String colName = func.getMappingColumnName();
		String otherName = o.getFunction().getMappingColumnName();

		VisualProperty<?> prop = func.getVisualProperty();
		String dispName = prop.getDisplayName();
		VisualProperty<?> otherProp = o.getFunction().getVisualProperty();
		String otherPropName = otherProp.getDisplayName();

		boolean imaNode = dispName.contains("Node");
		boolean uraNode = otherPropName.contains("Node");
		
		if (imaNode && !uraNode) return -1;
		if (uraNode && !imaNode) return 1;
		return colName.compareTo(otherName);
	}
  }
//------------------  UTILITY -----------------------------------------------
	static Map<String,String> ezMap(Object[] elems) {
	    final Map<String,String> map = new HashMap<String,String>();
	    for (int i = 0; i < elems.length-1; i += 2) 
	      map.put(elems[i].toString(), elems[i+1].toString());
	    return map;
	  }

	private String getType(Object v) {
		if (v == null) return "";
		 String s = v.getClass().toString();
		 int idx = s.lastIndexOf(".");
		 String sub = idx < 0 ? s : s.substring(idx+1);
		 return sub;
	}

	private boolean isNumeric(Object v) { return v instanceof Double || v instanceof Float || v instanceof Integer; }
	private boolean isFontSize(Object v) {  		return v instanceof Font;	}
	private boolean isStroke(Object v) { 		return v instanceof LineType;	}
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
		int SPACER = 50;
		int startX = X - SPACER;
		int startY = Y - SPACER;
		int W = 500;
		int H = 100;
//		int dX = layoutVertically ? 0 : W + SPACER;
//		int dY = layoutVertically ? H + SPACER : 0;			

		GroupAnnotation gradientLegend = addGradientLegend("Fill Color: layout.degree ",  X, Y, W, H, -2, 3, colors, stops);
		annotationMgr.addAnnotation(gradientLegend);
		X += layoutVertically ? 0 : W + SPACER;  	
		Y += layoutVertically ? H + SPACER : 0;	
		
		Dimension legendSize = new Dimension( 200, 500);
		GroupAnnotation discreteLegend = addDiscreteColorLegend("Fill Color: Type", X, Y, legendSize, discreteColors, names);
		annotationMgr.addAnnotation(discreteLegend);
		X += layoutVertically ? 0 : legendSize.width;  	
		Y += layoutVertically ? legendSize.height : 0;	

		W = 500;
		H = 200;
		GroupAnnotation continuousLegend = addTrapezoidLegend("Node Size: Expression", X, Y, W, H, 12, 45, Color.LIGHT_GRAY);
		annotationMgr.addAnnotation(continuousLegend);
		X += layoutVertically ? 0 : W + SPACER;  	
		Y += layoutVertically ? H + SPACER : 0;	
	
		W = 300;
		H = 300;
		legendSize = new Dimension( 300, 300);
		GroupAnnotation linetypeLegend = addLinetypeLegend("Line Type: Function", X, Y, legendSize, LINETYPES, LINETYPE_NAMES);
		annotationMgr.addAnnotation(linetypeLegend);
		X += layoutVertically ? 0 : legendSize.width;  	
		Y += layoutVertically ? legendSize.height : 0;	

		legendSize = new Dimension( 300, 500);
		GroupAnnotation arrowLegend =  addArrowheadLegend("Arrowhead: Interaction Type", X, Y, legendSize, arrowNames, arrowheads);
		annotationMgr.addAnnotation(arrowLegend);
		X += layoutVertically ? 0 : legendSize.width;  	
		Y += layoutVertically ? legendSize.height : 0;	

		legendSize = new Dimension( 200, 500);
		GroupAnnotation shapeLegend = addShapeLegend("Node Shape: Protein Type ", X, Y, legendSize, shapes, shapenames);
		annotationMgr.addAnnotation(shapeLegend);
		X += layoutVertically ? 0 : legendSize.width;  	
		Y += layoutVertically ? legendSize.height : 0;	

		GroupAnnotation fontSizeLegend =  addFontSizeLegend("Font Size: degree", X, Y, legendSize, 10, 50, Color.cyan);
		annotationMgr.addAnnotation(fontSizeLegend);
		X += layoutVertically ? 0 : legendSize.width;  	
		Y += layoutVertically ? legendSize.height : 0;	

		if (borderBox)
		{
			Object[] boxArgs = { "x", startX, "y", startY , "width", X-startX, "height", Y-startY,  "shapeType" , "Rectangle"};
			Map<String,String> strs = ezMap(boxArgs);
			ShapeAnnotation lineBox = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, strs);
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
	private void dump() {
		Map<Paint, CyNode> fillColorMap = new HashMap<Paint, CyNode>();
		Map<Paint, CyNode> borderColorMap = new HashMap<Paint, CyNode>();

		List<CyNode> nodes = network.getNodeList();
		for (CyNode node : nodes)
		{
			 View<CyNode> nodeView = networkView.getNodeView(node);
			 Paint fillColor = nodeView.getVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR);
			 fillColorMap.put(fillColor, node);
			 Paint borderColor = nodeView.getVisualProperty(BasicVisualLexicon.NODE_BORDER_PAINT);
			 borderColorMap.put(borderColor, node);
		}
		System.out.println("\n\nThere are " + fillColorMap.size() + " fill colors and " + borderColorMap.size() + " border colors used.");
			
		Map<Paint, CyEdge> edgeColorMap = new HashMap<Paint, CyEdge>();
		Map<LineType, CyEdge> lineTypeMap = new HashMap<LineType, CyEdge>();
		
		List<CyEdge> edges = network.getEdgeList();
		for (CyEdge edge : edges)
		{
			 View<CyEdge> edgeView = networkView.getEdgeView(edge);
			 Paint edgeColor = edgeView.getVisualProperty(BasicVisualLexicon.EDGE_PAINT);
			 edgeColorMap.put(edgeColor, edge);
			 LineType lineType = edgeView.getVisualProperty(BasicVisualLexicon.EDGE_LINE_TYPE);
			 lineTypeMap.put(lineType, edge);
		}
		System.out.println("There are " + edgeColorMap.size() + " edge colors and " + lineTypeMap.size() + " line types used.");
	}
	
}
