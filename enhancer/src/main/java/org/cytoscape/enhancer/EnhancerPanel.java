package org.cytoscape.enhancer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.view.model.CyNetworkView;

public class EnhancerPanel extends JPanel implements CytoPanelComponent {
	
	//--------------------------------------------------------------------
	public EnhancerPanel(EnhancerController ctrl) {
		controller = ctrl;
		controller.setEnhancerPanel(this);
		buildUI();
		setVisible(true);
	}

	//--------------------------------------------------------------------
	private static final long serialVersionUID = 12L;

	private EnhancerController controller;
	private JPanel categoryParentPanel;
	
	public Component getComponent() 			{		return this;	}
	public CytoPanelName getCytoPanelName() 	{		return CytoPanelName.WEST;	}
	public String getTitle() 					{		return "Enhancer Panel";	}
	public Icon getIcon() 						{		return null;	}

	
	JButton adder = new JButton("Add");
	JButton doIt = new JButton("Enhance");		
	JButton clearAll = new JButton("Clear");

	List<ColumnMapPane> categories = new ArrayList<ColumnMapPane>();
	int lineHeight = 40;
	Dimension dim = new Dimension(400, lineHeight);
	Dimension numDimension = new Dimension(40, 30);
	Dimension columnDimension = new Dimension(140, lineHeight);
	Dimension colorDimension = new Dimension(24, 24);
	Dimension colorLabDimension = new Dimension(64, lineHeight);
	Dimension rangeDimension = new Dimension(100, lineHeight);

JPanel makeIntro()
{
	JPanel intro = new JPanel();
	intro.setLayout(new BoxLayout(intro, BoxLayout.PAGE_AXIS));
	JLabel label1 = new JLabel("Select the columns and colors to assign to the pies.");
	JLabel label2 = new JLabel("Range can be optionally set to get normalized colors.");
	intro.add(line(label1));
	intro.add(line(label2));
	intro.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
	LookAndFeelUtil.makeSmall(label1);
	LookAndFeelUtil.makeSmall(label2);
	return intro;

}
	JPanel makeHeader()
	{
		JLabel lab1 = new JLabel("Column");
		JLabel lab2 = new JLabel("Color");
		JLabel lab3 = new JLabel("Range");
		LookAndFeelUtil.makeSmall(lab1,lab2, lab3);
		setSizes(lab1, columnDimension);
		setSizes(lab2, colorLabDimension);
		setSizes(lab3, rangeDimension);
		JPanel line = new JPanel();
		setSizes(line, dim);
		line.setLayout(new BoxLayout(line, BoxLayout.LINE_AXIS));
		line.add(lab1);
		line.add(lab2);
		line.add(lab3);
		return line;
		
	}

	static void setSizes(Component p, Dimension d)
	{
		p.setMinimumSize(d);
		p.setMaximumSize(d);
		p.setPreferredSize(d);
		
	}
	class ColumnMapPane extends JPanel
	{
		JComboBox<String> column;
		ColorMenuButton colorButton;
		JTextField minVal;
		JTextField maxVal;
		String[] colNames = { "Name", "Age", "JurkatScore", "HEKScore" } ;
			
		private ColumnMapPane()
		{
			setSizes(this, dim);
			column = new JComboBox<String>(colNames); 	
			column.setSize(columnDimension);
			colorButton = new ColorMenuButton();
			setSizes(colorButton,colorDimension); 
			colorButton.setMinimumSize(colorDimension);
			colorButton.setMaximumSize(colorDimension);
			colorButton.setPreferredSize(colorDimension);
			JPanel around = new JPanel();
			around.add(colorButton);
//			around.setBorder(BorderFactory.createDashedBorder(Color.blue));
			//			colorButton.setSize(colorDimension);
			minVal = new JTextField("0.0"); 				
			setSizes(minVal,numDimension);
			maxVal = new JTextField("1.0");				
			maxVal.setSize(numDimension);
			setSizes(maxVal,numDimension);
//			colorButton.setBorder(BorderFactory.createLineBorder(Color.red));
			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			add(column);		add(Box.createHorizontalStrut(20));
//			JButton b = new JButton("Push");
//			add(b);
			add(around);	add(Box.createHorizontalStrut(20));
			LookAndFeelUtil.makeSmall(minVal);
			LookAndFeelUtil.makeSmall(maxVal);
			JLabel lab1 = new JLabel("Min:");
			JLabel lab2 = new JLabel("Max:");
			LookAndFeelUtil.makeSmall(lab1);
			LookAndFeelUtil.makeSmall(lab2);
			add(lab1); add(minVal); add(Box.createHorizontalStrut(20));
			add(lab2); add(maxVal);
			setBorder(BorderFactory.createLineBorder(Color.ORANGE));
		}
		
		public String getColumn()	{ return "" + column.getSelectedItem(); }
		public Color getCatColor()	{ return colorButton.getColor(); }
		public double getMin()		{	return Double.parseDouble(minVal.getText());			}
		public double getMax()		{	return Double.parseDouble(maxVal.getText());			}
	}
	
	private void addCategory()	
	{ 	
		ColumnMapPane pane = new ColumnMapPane();
		categories.add(pane);
		categoryParentPanel.add(pane);
	}
	//--------------------------------------------------------------------
	private void buildUI() {
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(makeIntro());
		categoryParentPanel = new JPanel();
//		optionsPanel.setAlignmentX(0f);
		categoryParentPanel.setBorder(BorderFactory.createEtchedBorder());
		categoryParentPanel.setLayout(new BoxLayout(categoryParentPanel, BoxLayout.PAGE_AXIS));
		categoryParentPanel.add(Box.createRigidArea(new Dimension(10,10)));
		categoryParentPanel.add(makeHeader());
		add(categoryParentPanel);
		add(Box.createRigidArea(new Dimension(20, 20)));
		add(Box.createGlue());

		//a row of buttons and their actions
		ActionListener addCategory = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) 
			{
				addCategory(); 
				setVisible(false);
				setVisible(true);
			}
		};
		adder.addActionListener(addCategory);


		ActionListener clrAll = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { resetOptionsPanel();  }
		};
		add(line(clearAll, adder));
		clearAll.addActionListener(clrAll);

		addCategory();
		add(Box.createVerticalGlue());		//-------
	}

	//--------------------------------------------------------------------
	JComponent[] ctrls = {adder, clearAll, doIt};
	public void enableControls(boolean on)
	{
		for (JComponent component : ctrls)
			component.setEnabled(on);

		for (ColumnMapPane category : categories)
			category.setEnabled(on);
	}
	
	public void resetOptionsPanel()
	{
		categoryParentPanel.removeAll();
		categoryParentPanel.setVisible(false);
		categoryParentPanel.setVisible(true);

	}
	//--------------------------------------------------------------------
	public void setCurrentNetwork()
	{
		CyNetworkView view = controller.getNetworkView();
		enableControls(view != null);
	}
	//--------------------------------------------------------------------
	public String extract()
	{
		// pull the data out of the GUI components
		StringBuilder builder = new StringBuilder();
		for (ColumnMapPane pane : categories)
		{
			builder.append(pane.getColumn());
			Color c = pane.getCatColor();
			builder.append("\t").append(c.toString()).append("\t");
			builder.append ("[").append(pane.getMin());
			builder.append (",").append(pane.getMax()).append ("]\n");
		}
		builder.append ("\n");
		return builder.toString();
	}
	
	//--------------------------------------------------------------------
	// util
	
	private JPanel line(JComponent sub)
	{
		JPanel box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.LINE_AXIS));
		box.add(sub);
		box.add(Box.createHorizontalGlue());
//		box.setBorder(BorderFactory.createLineBorder(Color.blue));
		return box;
	}
	
	private JPanel line(JComponent subA, JComponent subB)
	{
		JPanel box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.LINE_AXIS));
		box.add(subA);
		box.add(Box.createRigidArea(new Dimension(12,12)));
		box.add(subB);
		box.add(Box.createHorizontalGlue());
//		box.setBorder(BorderFactory.createLineBorder(Color.blue));
		return box;
	}


}

