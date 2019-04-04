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
	
	class ColumnMapPane extends JPanel
	{
		JComboBox<String> column;
		ColorMenuButton colorButton;
		JTextField minVal;
		JTextField maxVal;
		String[] colNames = { "Name", "Age", "JurkatScore", "HEKScore" } ;
			
		private ColumnMapPane()
		{
			int lineHeight = 40;
			Dimension dim = new Dimension(400, lineHeight);
			setSize(dim);
			setMaximumSize(dim);
			Dimension numDimension = new Dimension(40, lineHeight);
			Dimension columnDimension = new Dimension(140, lineHeight);
			Dimension colorDimension = new Dimension(30, lineHeight);
			column = new JComboBox<String>(colNames); 	column.setSize(columnDimension);
			colorButton = new ColorMenuButton(); 		
			colorButton.setMinimumSize(numDimension);
			colorButton.setMaximumSize(numDimension);
			colorButton.setPreferredSize(numDimension);
			JPanel around = new JPanel();
			around.add(colorButton);
			around.setBorder(BorderFactory.createDashedBorder(Color.blue));
			//			colorButton.setSize(colorDimension);
			minVal = new JTextField("0.0"); 				
			minVal.setSize(numDimension);
			minVal.setMinimumSize(numDimension);
			minVal.setMaximumSize(numDimension);
			maxVal = new JTextField("1.0");				
			maxVal.setSize(numDimension);
			maxVal.setMinimumSize(numDimension);
			maxVal.setMaximumSize(numDimension);
//			colorButton.setBorder(BorderFactory.createLineBorder(Color.red));
			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			add(column);		add(Box.createHorizontalStrut(20));
//			JButton b = new JButton("Push");
//			add(b);
			add(around);	add(Box.createHorizontalStrut(20));
			add(new JLabel("Min:")); add(minVal); add(Box.createHorizontalStrut(20));
			add(new JLabel("Max:")); add(maxVal);
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
		JPanel intro = new JPanel();
		intro.setLayout(new BoxLayout(intro, BoxLayout.PAGE_AXIS));
		JLabel label1 = new JLabel("Select the columns and colors to assign to the pies.");
		JLabel label2 = new JLabel("Range can be optionally set to get normalized colors.");
		intro.add(line(label1));
		intro.add(line(label2));
		intro.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		add(intro);
		LookAndFeelUtil.makeSmall(label1);
		LookAndFeelUtil.makeSmall(label2);

		categoryParentPanel = new JPanel();
//		optionsPanel.setAlignmentX(0f);
		categoryParentPanel.setBorder(BorderFactory.createEtchedBorder());
		categoryParentPanel.setLayout(new BoxLayout(categoryParentPanel, BoxLayout.PAGE_AXIS));
		categoryParentPanel.add(Box.createRigidArea(new Dimension(10,10)));
	
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

