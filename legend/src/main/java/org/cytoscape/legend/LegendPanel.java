package org.cytoscape.legend;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.legend.LegendController.LegendCandidate;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunction;

public class LegendPanel extends JPanel implements CytoPanelComponent {
	
	private static final long serialVersionUID = 8292806967891823933L;

	private LegendController controller;
	private JPanel optionsPanel;
	
	public Component getComponent() 				{		return this;	}
	public CytoPanelName getCytoPanelName() 		{		return CytoPanelName.WEST;	}
	public String getTitle() 					{		return "Legend Panel";	}
	public Icon getIcon() 						{		return null;	}

	JCheckBox 		layoutVertical = new JCheckBox("Lay out vertically");
	JCheckBox  		drawBorder = new JCheckBox("Draw bounding box");
	JLabel curNetNameLabel = new JLabel("No network selected");
	JTextField title  = new JTextField();
	JTextField subtitle  = new JTextField();
	
	JButton adder = new JButton("Add Legend");
	JButton selectAll = new JButton("Select All Annotations");			// DEBUG
	JButton clearAll = new JButton("Remove All Annotations");
	JButton tester = new JButton("Test");

	//--------------------------------------------------------------------
	public LegendPanel(CyServiceRegistrar reg, LegendController ctrl) {
		controller = ctrl;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		controller.setLegendPanel(this);
		buildUI(reg);
		title.setText(controller.getCurrentNetworkName());
		setVisible(true);
	}
	
	//--------------------------------------------------------------------
	static Font smallFont = new Font("Serif", 0, 8);
	private void buildUI(CyServiceRegistrar reg) {
		
		JPanel intro = new JPanel();
		intro.setLayout(new BoxLayout(intro, BoxLayout.PAGE_AXIS));
		JLabel label1 = new JLabel("Legends are drawn as annotations in the background canvas.");
		JLabel label2 = new JLabel("Use these controls to customize your legend.");
		intro.add(line(label1));
		intro.add(line(label2));
		intro.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		add(intro);
		LookAndFeelUtil.makeSmall(label1);
		LookAndFeelUtil.makeSmall(label2);

		optionsPanel = new JPanel();
//		optionsPanel.setAlignmentX(0f);
		optionsPanel.setBorder(BorderFactory.createEtchedBorder());
		optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
		optionsPanel.add(Box.createRigidArea(new Dimension(10,10)));
		JButton scanner = new JButton("Scan Network");
		ActionListener scan = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { 
				setCurrentNetwork();
				controller.scanNetwork();
				optionsPanel.removeAll();
				for (LegendCandidate candidate : controller.getCandidates())
				{
					VisualMappingFunction<?,?> fn = candidate.getFunction();
					String type = fn.getMappingColumnType().toString();
					int idx = type.lastIndexOf('.');
					if (idx > 0) type = type.substring(idx+1);
					VisualProperty<?> prop = fn.getVisualProperty();
					String dispName = prop.getDisplayName();
					String name = "Show " + fn.getMappingColumnName() + " as " + dispName;
					JCheckBox checkbox = new JCheckBox(name, true);
					optionsPanel.add(line(checkbox));
					candidate.setCheckBox(checkbox);
				}
				optionsPanel.repaint();
			}
		};
		scanner.addActionListener(scan);
		add(line(scanner,curNetNameLabel));
		
		JLabel titlePrompt = new JLabel("Title");
		titlePrompt.setPreferredSize(new Dimension(50, 28));
		title.setMaximumSize(new Dimension(640, 28));
		title.setPreferredSize(new Dimension(640, 28));
		title.setMinimumSize(new Dimension(240, 28));
		LookAndFeelUtil.makeSmall(titlePrompt);
		JLabel subtitlePrompt = new JLabel("Subtitle");
		subtitlePrompt.setPreferredSize(new Dimension(50, 28));
		subtitle.setMaximumSize(new Dimension(640, 28));
		subtitle.setPreferredSize(new Dimension(640, 28));
		subtitle.setMinimumSize(new Dimension(240, 28));
		LookAndFeelUtil.makeSmall(subtitlePrompt);

		add(line(titlePrompt, title));
		add(line(subtitlePrompt, subtitle));
		add(optionsPanel);
		optionsPanel.add(Box.createRigidArea(new Dimension(20, 20)));
		optionsPanel.add(Box.createGlue());

		add(line(layoutVertical));
		add(line(drawBorder));

		ActionListener layout = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { controller.layout(); }
		};
		adder.addActionListener(layout);
		add(line(adder));

		add(Box.createVerticalGlue());		//-------

		ActionListener test = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { controller.testAnnotations();  }
		};
		tester.addActionListener(test);
		add(line(tester));

		ActionListener selAll = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { controller.selectAllAnnotations();  }
		};
		selectAll.addActionListener(selAll);
		add(line(selectAll));

		ActionListener clrAll = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { controller.clearAnnotations();  }
		};
		clearAll.addActionListener(clrAll);
		add(line(clearAll));

		
	}

	//--------------------------------------------------------------------
	JComponent[] ctrls = {adder, selectAll, clearAll, title, subtitle};
	public void enableControls(boolean on)
	{
		for (JComponent component : ctrls)
			component.setEnabled(on);
	}
	
	//--------------------------------------------------------------------
	public void setCurrentNetwork()
	{
		CyNetworkView view = controller.getNetworkView();
		curNetNameLabel.setText(view == null ? "no current network" : "" + view.getModel());
		title.setText(view == null ? "" : "" + view.getModel());
		if (subtitle.getText().isEmpty())
			subtitle.setText("Legend");
		title.setText(view == null ? "" : "" + view.getModel());
		enableControls(view != null);
	}
	//--------------------------------------------------------------------
	public void extract()
	{
		controller.setLayout(layoutVertical.isSelected());
		controller.setDrawBorder(drawBorder.isSelected());
		controller.setTitle(title.getText());
		controller.setSubtitle(subtitle.getText());
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

