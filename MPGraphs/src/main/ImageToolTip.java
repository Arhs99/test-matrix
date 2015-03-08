package main;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.plaf.metal.MetalToolTipUI;

import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;

import viewer.StructureDisplay;


class ImageToolTip extends JToolTip {
	/**
	 * 
	 */
	private static final long serialVersionUID = -20721788964098197L;

	public ImageToolTip(StructureDisplay tdp1, StructureDisplay tdp2) {

		JPanel panel = new JPanel();
		panel.add(tdp1);
		panel.add(tdp2);
		setUI(new ImageToolTipUI(tdp1));
	}

}

class ImageToolTipUI extends MetalToolTipUI {
	private static final int W = 200;
	private static final int H = 200;
	private StructureDisplay panel;
	public ImageToolTipUI(StructureDisplay panel) {
		this.panel = panel;
	}
	public void paint(Graphics g, JComponent c) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(Color.white);
		if (panel.isEmpty()) {
			FontMetrics metrics = c.getFontMetrics(g.getFont());
			String tipText = ((JToolTip) c).getTipText();
			int width = SwingUtilities.computeStringWidth(metrics, tipText);
			int height = metrics.getHeight();
			g.fillRect(0, 0, width + 1,height + 1);
			if (Double.parseDouble(tipText) < 0) {
				g.setColor(Color.red);
			} else {
				g.setColor(Color.black);
			}
			//g.setFont(new Font("Verdana", Font.PLAIN, 12));
			g.drawString(tipText, 1, height-1);
			return;
		}
		AWTDrawVisitor visitor = new AWTDrawVisitor(g2d);
		Rectangle drawArea = new Rectangle(W, H);
		g2d.fillRect(0, 0, W, H);
		panel.getRenderer().paint(panel.getMol(), visitor, drawArea, true);
	}

	public Dimension getPreferredSize(JComponent c) {
		if (!panel.isEmpty()) {
			return new Dimension(W, H);
		}
		FontMetrics metrics = c.getFontMetrics(c.getFont());
		String tipText = ((JToolTip) c).getTipText();
		if (tipText == null) {
			tipText = "";
		}
		int width = SwingUtilities.computeStringWidth(metrics, tipText);
		int height = metrics.getHeight();
		return new Dimension(width + 1, height + 1);
	}
}
