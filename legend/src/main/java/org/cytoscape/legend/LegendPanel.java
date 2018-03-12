package org.cytoscape.legend;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

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
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunction;

public class LegendPanel extends JPanel implements CytoPanelComponent {
	
	private static final long serialVersionUID = 8292806967891823933L;

	private LegendController controller;
	private JPanel optionsPanel;
	
	public LegendPanel(CyServiceRegistrar reg, LegendController ctrl) {
		controller = ctrl;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		JPanel intro = new JPanel();
		intro.setLayout(new BoxLayout(intro, BoxLayout.PAGE_AXIS));
		intro.add(line(new JLabel("Legend")));
		intro.add(line(new JLabel("Use these controls to customize your legend")));
		add(intro);

		optionsPanel = new JPanel();
//		optionsPanel.setAlignmentX(0f);
		optionsPanel.setBorder(BorderFactory.createEtchedBorder());
		optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
		optionsPanel.add(Box.createRigidArea(new Dimension(10,10)));
		JButton scanner = new JButton("Scan Network");
		ActionListener scan = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { 
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
		add(line(scanner, new JLabel("(automate me)")));
		
		JLabel titlePrompt = new JLabel("Title");
		title = new JTextField();
<<<<<<< HEAD
		title.setMaximumSize(new Dimension(400, 28));
		title.setPreferredSize(new Dimension(300, 28));
		title.setMinimumSize(new Dimension(200, 28));
		JLabel subtitlePrompt = new JLabel("Subtitle");
//		showDate = new JCheckBox("Include Date");
		subtitle = new JTextField();
		subtitle.setMaximumSize(new Dimension(400, 28));
		subtitle.setPreferredSize(new Dimension(300, 28));
=======
		title.setMaximumSize(new Dimension(300, 28));
		title.setPreferredSize(new Dimension(200, 28));
		title.setMinimumSize(new Dimension(200, 28));
		JLabel subtitlePrompt = new JLabel("Include Subtitle");
//		showDate = new JCheckBox("Include Date");
		subtitle = new JTextField();
		subtitle.setMaximumSize(new Dimension(300, 28));
		subtitle.setPreferredSize(new Dimension(200, 28));
>>>>>>> 5e04d5cf23e1c74418c64cd3e7fc8f71ad9f8c96
		subtitle.setMinimumSize(new Dimension(200, 28));
		ActionListener echo = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { System.out.println(e.getActionCommand()); }
		};
//		showUserTitle.addActionListener(echo);
//		showSubtitle.addActionListener(echo);
//		showDate.addActionListener(echo);
		
		add(line(titlePrompt, title));
		add(line(subtitlePrompt, subtitle));
//		add(line(showDate));
		add(optionsPanel);
		optionsPanel.add(Box.createRigidArea(new Dimension(20, 20)));

		optionsPanel.add(Box.createGlue());

//		orientVertical = new JCheckBox("Orient Vertically", true);
//		orientVertical.addActionListener(echo);
//		add(line(orientVertical));

		layoutVertical = new JCheckBox("Lay out Vertically");
		layoutVertical.addActionListener(echo);
		add(line(layoutVertical));

		drawBorder = new JCheckBox("Draw Border Box");
		drawBorder.addActionListener(echo);
		add(line(drawBorder));

		JButton adder = new JButton("Add Legend");
		ActionListener layout = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { controller.layout(); }
		};
		adder.addActionListener(layout);
		add(line(adder));
		
		
<<<<<<< HEAD
		JButton selectAll = new JButton("Select All Annotations");
		ActionListener selAll = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { controller.selectAllAnnotations();  }
		};
		selectAll.addActionListener(selAll);
		add(line(selectAll));

		
		JButton clearAll = new JButton("Remove All Annotations");
		ActionListener clr = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { controller.clearAnnotations();  }
		};
		clearAll.addActionListener(clr);
		add(line(clearAll));

		
=======
>>>>>>> 5e04d5cf23e1c74418c64cd3e7fc8f71ad9f8c96
		JButton tester = new JButton("Test");
		ActionListener test = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) { controller.testAnnotations();  }
		};
		tester.addActionListener(test);
		add(line(tester));

		setVisible(true);
	}
//	JCheckBox orientVertical;
	JCheckBox layoutVertical;
	JCheckBox drawBorder;
	
//	JCheckBox showDate;
	JTextField title;
	JTextField subtitle;
	
	public void extract()
	{
//		controller.setOrientation(orientVertical.isSelected());
		controller.setLayout(layoutVertical.isSelected());
		controller.setDrawBorder(drawBorder.isSelected());

		controller.setTitle(title.getText());
		controller.setSubtitle(subtitle.getText());
//		controller.setShowDate(showDate.isSelected());
	}
	
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

	List<JCheckBox> options;
	
	public Component getComponent() 				{		return this;	}
	public CytoPanelName getCytoPanelName() 		{		return CytoPanelName.WEST;	}
	public String getTitle() 					{		return "Legend Panel";	}
	public Icon getIcon() 						{		return null;	}

}

