package org.cytoscape.legend;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LinearGradientPaint;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.annotations.ArrowAnnotation;
import org.cytoscape.view.presentation.annotations.GroupAnnotation;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;
import org.cytoscape.view.presentation.annotations.ArrowAnnotation.AnchorType;
import org.cytoscape.view.presentation.annotations.ArrowAnnotation.ArrowEnd;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation.ShapeType;
import org.cytoscape.view.presentation.property.values.LineType;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.ContinuousMappingPoint;

public class LegendFactory {

	private AnnotationFactory<ShapeAnnotation> shapeFactory;
	private AnnotationFactory<TextAnnotation> textFactory;
	private AnnotationFactory<GroupAnnotation> groupFactory;
	private AnnotationFactory<ArrowAnnotation> arrowFactory;
	private AnnotationManager annotationMgr;
	private CyServiceRegistrar registrar;
	private FontRenderContext fontRenderContext;
	private CyNetworkView networkView;

		boolean verbose = true;
		
	public LegendFactory(CyServiceRegistrar reg, CyNetworkView view)
	{
		registrar = reg;
		networkView = view;
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
		fontRenderContext = new FontRenderContext(new AffineTransform(),true,true);     
		
	}
	boolean borderBox = false;
	public void setDrawBorder(boolean show)	{ borderBox = show;	}
	int RIGHT_MARGIN = 0;
	public int getRightMargin() {		return RIGHT_MARGIN;		}

	public void addContinuousMapLegend(ContinuousMapping<?, ?> continFn, int X, int Y, Dimension ioSize) {
		
		int width = ioSize.width ;
		int height = ioSize.height;
		Object v = null;
		VisualProperty<?> prop = continFn.getVisualProperty();
		String dispName = prop.getDisplayName();
		BoundaryRangeValues<Double> range = getFunctionRange(continFn);
		double minimum = range.lesserValue;
		double maximum = range.greaterValue;
		v = prop.getDefault();
		String title = dispName + ":  " + continFn.getMappingColumnName();
		if (verbose)
		{
			System.out.print(title + " has range (" + minimum + "  - " + maximum + ") "); 					
			System.out.println(title + " and is of type " + getType(v)); 					
			System.out.println("Draw box: " + (borderBox ? "true" : "false")); 					
		}
		
		RIGHT_MARGIN = 0;
		GroupAnnotation legend = null;
		if (v instanceof Color)
			legend = addGradientLegend(title, X, Y, width, height, continFn);
		else if (isFontSize(v))
			legend = addFontSizeLegend(title, X, Y, ioSize, minimum, maximum, Color.LIGHT_GRAY);
		else if (isNumeric(v))
			legend = addTrapezoidLegend(title, X, Y, width, height, minimum, maximum, Color.LIGHT_GRAY);
		ioSize.width += RIGHT_MARGIN;
		
		if (legend != null)
			annotationMgr.addAnnotation(legend);
	}
	

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

	
	//----------- Legend Subtypes ----------------------------------------
	//------------------------------------------------------------------
	public GroupAnnotation addDiscreteColorLegend(String title, int x, int y, Dimension ioSize, Color[] colors, String[] names)
	{
		boolean orientVertically = true;
	int NCELLS = colors.length;
	float MARGIN = 10;
	float LINE_HEIGHT = 25;
	float SWATCH_HEIGHT = LINE_HEIGHT + MARGIN;;
	float CELL_HEIGHT = SWATCH_HEIGHT + MARGIN;
	float SWATCH_WIDTH = 80;
	float CELL_WIDTH = SWATCH_WIDTH + MARGIN;
	Font labelFont = getLabelFont();  
	int maxLabelWidth = getMaxLabelWidth(names, labelFont);
	int labelHeight = getStringHeight(names[0], labelFont);
	int HALF_DIF = (int)((LINE_HEIGHT - labelHeight) / 2);
	
	float width =  3 * MARGIN + (orientVertically ? (CELL_WIDTH + maxLabelWidth) : (CELL_WIDTH * NCELLS));
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
			Object[] textArgs = { "x", x + dx , "y", y + dy - HALF_DIF, "width", width - xx, "height", LINE_HEIGHT, "text", names[i]};
			strs = ezMap(textArgs);
			strs.put("fontFamily", labelFont.getFontName());
			strs.put("fontStyle", "" + labelFont.getStyle());
			strs.put("fontSize", "" + labelFont.getSize());
			TextAnnotation textBox = textFactory.createAnnotation(TextAnnotation.class, networkView, strs);
			group.addMember(textBox);
		}
		annotationMgr.addAnnotation(group);
		
		return group;
	}
	//------------------------------------------------------------------
	// discrete
	
	int getMaxLabelWidth(String[] labels, Font font)
	{
		int maxLabelWidth = 0;
		for (int i=0; i < labels.length; i++)
		{
			int w =  getStringWidth(labels[i], font);
			maxLabelWidth = Math.max(maxLabelWidth, w);
		}
		return maxLabelWidth;
		
		
	}
	int getLabelHeight(String text, Font font)
	{
		return getStringHeight(text, font);
	}

	public GroupAnnotation addShapeLegend(String title, int x, int y, Dimension ioSize, ShapeType[] shapes, String[] names)
	{
		int MARGIN = 10;			
		int NCELLS = shapes.length;
		int CELL_HEIGHT = 80;
		int CELL_WIDTH = 80;
		int LABEL_HEIGHT = 30;
		
		if (NCELLS == 0) return null;
		Font labelFont = getLabelFont();  
		int maxLabelWidth = getMaxLabelWidth(names, labelFont);
		int labelHeight = getStringHeight(names[0], labelFont);
		int HALF_DIF = (int)((CELL_HEIGHT - labelHeight) / 2);
		
		boolean orientVertically = true;
		int width =  3 * MARGIN + (orientVertically ? (CELL_WIDTH + maxLabelWidth) : (CELL_WIDTH * NCELLS));
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

			int halfStringWidth = maxLabelWidth / 2;
			float HALF_CELL_HEIGHT = CELL_HEIGHT / 2.0f;
			float dx = xx + (orientVertically ? (MARGIN + CELL_HEIGHT) : HALF_CELL_HEIGHT-halfStringWidth);
			float dy = yy + (orientVertically ? HALF_CELL_HEIGHT  : CELL_HEIGHT);
			Object[] textArgs = { "x", x + dx, "y", y + dy - HALF_DIF, "width", 200, "height", CELL_HEIGHT, "text", names[i]};
			strs = ezMap(textArgs);
			strs.put("fontFamily", labelFont.getFontName());
			strs.put("fontStyle", "" + labelFont.getStyle());
			strs.put("fontSize", "" + labelFont.getSize()
			);
			TextAnnotation textBox = textFactory.createAnnotation(TextAnnotation.class, networkView, strs);
			group.addMember(textBox);
		}
		return group;
	}
	
	private Font getLabelFont() {
		return new Font(Font.SANS_SERIF, 0, 20);
	}
	//------------------------------------------------------------------
	// discrete
	public GroupAnnotation addLinetypeLegend(String title, int x, int y, Dimension ioSize, LineType[] strokes, String[] names)
	{
		//TODO
		int NCELLS = strokes.length;
		int MARGIN = 10;
		int LINE_HEIGHT = 25;
		int SWATCH_HEIGHT = LINE_HEIGHT + MARGIN;
		int CELL_HEIGHT = SWATCH_HEIGHT + MARGIN;
		int SWATCH_WIDTH = 120;
		int CELL_WIDTH = SWATCH_WIDTH + MARGIN;
		int LABEL_HEIGHT = LINE_HEIGHT + MARGIN;

		boolean orientVertically = true;
		Font font = getLabelFont();
		int maxLabelWidth = getMaxLabelWidth(names, font);
		int labelHeight = getStringHeight(names[0], font);
		int half_dif = (CELL_HEIGHT - labelHeight) / 2;
		int width =  3 * MARGIN + (orientVertically ? (CELL_WIDTH + maxLabelWidth) : (CELL_WIDTH * NCELLS));
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
			Object[] targArgs = { "x", x + xx + SWATCH_WIDTH, "y", y + yy ,  "width", 2,  "height", 2,"shapeType", "Rectangle","fillColor", 0 };
			ShapeAnnotation targ = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, ezMap(targArgs));
			targ.setCanvas("background");
			annotationMgr.addAnnotation(targ);
			group.addMember(targ);

			float dx = xx + (orientVertically ? CELL_WIDTH : 0);
			float dy = yy + (orientVertically ? 0  : CELL_HEIGHT);
			Object[] textArgs = { "x", x + dx, "y", y + dy - half_dif,  "width", 40,  "height", 20, "fontSize", font.getSize(), "fontFamily", font.getFamily() };
			
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
	Color[] discreteColors = { Color.BLACK, Color.BLUE, Color.RED, Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW };
	//------------------------------------------------------------------
	// discrete
	public GroupAnnotation addArrowheadLegend(String title, int x, int y, Dimension ioSize, String[] arrows, String[] names)
	{
		//TODO
		int NCELLS = arrows.length;
		int MARGIN = 10;
		int LINE_HEIGHT = 25;
		int SWATCH_HEIGHT = LINE_HEIGHT + MARGIN;;
		int CELL_HEIGHT = SWATCH_HEIGHT + MARGIN;
		int SWATCH_WIDTH = 120;
		int CELL_WIDTH = SWATCH_WIDTH + MARGIN;
		int LABEL_HEIGHT = LINE_HEIGHT + MARGIN;
		Font font = getLabelFont();
		int maxLabelWidth = getMaxLabelWidth(names, font);
		int labelHeight = getStringHeight(names[0], font);
		int half_dif = (CELL_HEIGHT - labelHeight) / 2;

		boolean orientVertically = true;
		int width =  3 * MARGIN + (orientVertically ? (CELL_WIDTH + maxLabelWidth) : (CELL_WIDTH * NCELLS));
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
			group.addMember(src);
			
			Object[] targArgs = { "x", x + xx + SWATCH_WIDTH, "y",  y + yy,  "width", 2,  "height", 2,"shapeType", "Rectangle","fillColor", 0 };
			ShapeAnnotation targ = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, ezMap(targArgs));
			group.addMember(targ);

			float dx = xx + (orientVertically ? CELL_WIDTH : 0);
			float dy = yy + (orientVertically ? 0  : CELL_HEIGHT);
			Object[] textArgs = { "x", x + dx , "y", y + dy - half_dif,  "width", 40,  "height", 20, "fontSize","" + font.getSize() , "fontFamily", font.getFamily() };
			TextAnnotation text = textFactory.createAnnotation(TextAnnotation.class, networkView, ezMap(textArgs));
			text.setText(name);
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
	
	public GroupAnnotation addTrapezoidLegend(String title, int x, int y, int w, int h, double min, double max, Color color)
	{
		GroupAnnotation group = createGroupWithHeader( title,  x,  y,  w,  h);
		addBorderBox(group, x, y, w, h);

		GeneralPath path = new GeneralPath();
		path.moveTo(x, y + h);
		path.lineTo(x, y + (0.8 * h));
		path.lineTo(x + w, y);
		path.lineTo(x + w, y + h);
		path.lineTo(x, y + h);
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
		addTicks(x,y,w,h,min, max, group, false);
		addYTicks(x,y,w,h,min, max, group);
		return group;

	}	
	//------------------------------------------------------------------
	// continuous

	public GroupAnnotation addFontSizeLegend(String title, int x, int y, Dimension inSize, double min, double max, Color color)
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
		int FUDGE = 25;			// offset the ticks to match the text
		addTicks(x,y+FUDGE,w,h-(2 * FUDGE),min, max, group, true);
		return group;

	}
	//------------------------------------------------------------------
	// continuous

	public GroupAnnotation addGradientLegend(String title, int x, int y, int w, int h, ContinuousMapping<?, ?> fn)
	{
		Object pts = fn.getAllPoints();
		VisualProperty<?> prop = fn.getVisualProperty();
//		String dispName = prop.getDisplayName();
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
//				System.out.println(String.format("value = %3.2f stop = %3.2f", value, stops[i]));
			}
			i++;
		}
		return addGradientLegend(title, x, y, w, h, minimum, maximum, colors, stops);
	}
	
	public GroupAnnotation addGradientLegend(String title, int x, int y, int w, int h, double min, double max, Color[] colors, float[] stops)
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
		Font labelFont = getLabelFont();
		int FONTSIZE = labelFont.getSize();
		String fontFamily = labelFont.getFamily();
		String[] groupArgs = { "x", "" + x, "y", "" + y,  "width", "" + w,  "height", "" + h};
		GroupAnnotation group = groupFactory.createAnnotation(GroupAnnotation.class, networkView, ezMap(groupArgs));
		group.setCanvas("background");
		float LINE_HEIGHT = 28;
		Object[] titleArgs = { "x", x, "y", y-LINE_HEIGHT, "width", w, "height", LINE_HEIGHT, "text", title};
		Map<String,String> strs = ezMap(titleArgs);
		strs.put("fontFamily", fontFamily);
		strs.put("fontStyle", "" + labelFont.getStyle());
		strs.put("fontSize", "" + FONTSIZE);
	
		TextAnnotation titleBox = textFactory.createAnnotation(TextAnnotation.class, networkView, strs);
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
		borderBox.moveAnnotation(new Point2D.Double(x,y));
		group.addMember(borderBox);
		return borderBox;
	}
	//------------------------------------------------------------------

	private void addTicks(int x, int y, int w, int h, double min, double max,GroupAnnotation group, boolean orientVertically)
	{
		int TICKS = 5;
		int TICKWIDTH = 10;
		int LABELWIDTH = 35;
		int LABELHEIGHT = 15;
		Font labelFont = getLabelFont();
		int FONTSIZE = labelFont.getSize();
		String fontFamily = labelFont.getFamily();
		double BORDERWIDTH = 1.5;
		RIGHT_MARGIN = orientVertically ? TICKWIDTH + LABELWIDTH : 0;
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
			ticks[t].setFillColor(Color.BLACK);
			ticks[t].setBorderWidth(BORDERWIDTH);
			group.addMember(ticks[t]);
	
			Map<String,String> tickLabels = new HashMap<String,String>();
			double val = min + ((float) t / (TICKS-1)) * (max - min);
			String formattedStr = String.format("%3.1f", val);
			tickLabels.put("text", "" + formattedStr);
			int strWidth = getStringWidth(formattedStr, labelFont);
			int halfStringWidth = strWidth / 2;
			tickLabels.put("x", orientVertically ? "" + (xt  + 5 + TICKWIDTH) : ("" + (xt - halfStringWidth)));
			tickLabels.put("y", orientVertically ? "" + (yt-FONTSIZE/2.0) : ("" + (int) (yt  + 5 + TICKWIDTH)));
			tickLabels.put("width", "" + LABELWIDTH);
			tickLabels.put("height", "" + LABELHEIGHT);
			tickLabels.put("fontSize", "" + FONTSIZE);
			tickLabels.put("fontFamily", fontFamily);
			tickLabels.put("fontStyle", "" + Font.PLAIN);

			vals[t] = textFactory.createAnnotation(TextAnnotation.class, networkView, tickLabels);
			group.addMember(vals[t]);
			
		}
	}

	private void addYTicks(int x, int y, int w, int h, double min, double max,GroupAnnotation group)
	{
		int TICKS = 2;
		int TICKWIDTH = 10;
		int LABELWIDTH = 35;
		int LABELHEIGHT = 15;
		Font labelFont = getLabelFont();
		int FONTSIZE = labelFont.getSize();
		String fontFamily = labelFont.getFamily();
		double BORDERWIDTH = 1.5;
		RIGHT_MARGIN = TICKWIDTH + LABELWIDTH;
		ShapeAnnotation[] ticks = new ShapeAnnotation[TICKS];
		TextAnnotation[] vals = new TextAnnotation[TICKS];
		
		for (int t = 0; t < TICKS; t++)
		{
			double xt = x - TICKWIDTH;
			double yt = y + h - (t * h);
			
			Map<String,String> tickArgs = new HashMap<String,String>();
			tickArgs.put("x", "" + xt);
			tickArgs.put("y", "" + yt);
			tickArgs.put("width", "" + TICKWIDTH);
			tickArgs.put("height", "1");
			tickArgs.put("shapeType", "Rectangle");
			ticks[t] = shapeFactory.createAnnotation(ShapeAnnotation.class, networkView, tickArgs);
			ticks[t].setFillColor(Color.BLACK);
			ticks[t].setBorderWidth(BORDERWIDTH);
			group.addMember(ticks[t]);
	
			Map<String,String> tickLabels = new HashMap<String,String>();
			double val = min + t * (max - min);
			String formattedStr = String.format("%3.0f", val);
			tickLabels.put("text", "" + formattedStr);
			int strWidth = getStringWidth(formattedStr, labelFont);
//			int halfStringWidth = strWidth / 2;
			tickLabels.put("x", "" + (xt - strWidth - 5));
			tickLabels.put("y", "" + (yt -FONTSIZE/2.0));
			tickLabels.put("width", "" + LABELWIDTH);
			tickLabels.put("height", "" + LABELHEIGHT);
			tickLabels.put("fontSize", "" + FONTSIZE);
			tickLabels.put("fontFamily", fontFamily);
			tickLabels.put("fontStyle", "" + Font.PLAIN);

			vals[t] = textFactory.createAnnotation(TextAnnotation.class, networkView, tickLabels);
			group.addMember(vals[t]);
			
		}
	}
	
	int getStringWidth(String text, Font font)
	{
		return (int) textBox(text, font).getWidth();
	}

	int getStringHeight(String text, Font font)
	{
		return (int) textBox(text, font).getHeight();
	}

	Dimension textBox(String text, Font font)
	{
		Rectangle2D bounds = (font.getStringBounds(text, fontRenderContext));
		int w =  (int) bounds.getWidth();
		int h = (int) bounds.getHeight();
		return new Dimension(w,h);
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

		public TextAnnotation createTextAnnotation(Class<TextAnnotation> type, CyNetworkView view, Map<String, String> argMap) {
			return textFactory.createAnnotation(type, view, argMap);
		}

		public ShapeAnnotation createShapeAnnotation(Class<ShapeAnnotation> type, CyNetworkView view, Map<String, String> argMap) {
			return shapeFactory.createAnnotation(type, view, argMap);
		}


}
