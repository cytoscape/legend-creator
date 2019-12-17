package org.cytoscape.legend;


import java.awt.*;
import java.awt.print.*;
import javax.swing.*;

public class tester {

    public static class Gradient extends JComponent {

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        g2d.setColor(Color.RED);
        
        Rectangle r = new Rectangle(75, 75, 500, 150);
        g2d.fill(r); //paint a red opaque rectangle

        LinearGradientPaint p = new LinearGradientPaint(100, 100, 400, 100, new float[]{0.0f, 1.0f}, new Color[]{Color.GREEN, Color.BLUE}, MultipleGradientPaint.CycleMethod.NO_CYCLE);
        g2d.setPaint(p);
        g2d.fill(r); //paint a green to blue gradient
    }
}

public static final Gradient rect = new Gradient();

public static void main(String[] args) throws PrinterException {

    JFrame f = new JFrame("Test Gradient");
    f.setLayout(new BorderLayout());
    f.add(rect);
    f.setSize(600, 300);
    f.setVisible(true);
  }
 }

