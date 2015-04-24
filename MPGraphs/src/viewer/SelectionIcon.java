package viewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import javax.vecmath.Point2d;

import org.openscience.cdk.geometry.GeometryUtil;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.SymbolVisibility;
import org.openscience.cdk.renderer.generators.HighlightGenerator;
import org.openscience.cdk.renderer.generators.standard.SelectionVisibility;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;


public class SelectionIcon implements Icon {
	private AtomContainerRenderer renderer;
	private int width;
	private int height;
	private IAtomContainer mol;
	private RendererModel rm;
	private String fieldStr;
	private String molID;
	private double pot;


	public SelectionIcon(int iconWidth, int iconHeight, IAtomContainer ac,
			Double potency, String fieldStr, String molID, Collection<Integer> hiAtoms) throws Exception {
		StructureDisplay tdp = new StructureDisplay(ac);
		mol = ac;
		renderer = tdp.getRenderer();
		this.width = iconWidth;
		this.height = iconHeight;
		this.fieldStr = fieldStr;
		this.molID = molID;
		pot = potency;
		rm = renderer.getRenderer2DModel();
		rm.set(StandardGenerator.Highlighting.class,
                StandardGenerator.HighlightStyle.OuterGlow);
		rm.set(StandardGenerator.OuterGlowWidth.class,
                5d);
		rm.set(StandardGenerator.Visibility.class, SelectionVisibility.all
				(SymbolVisibility.iupacRecommendationsWithoutTerminalCarbon()));
		Map<IChemObject, Integer> ids = new HashMap<>();
		
		//rm.set(HighlightGenerator.HighlightPalette.class,
	    //          HighlightGenerator.createPalette(new Color(0x88ff0000, true), Color.BLUE, Color.GREEN));
		rm.set(HighlightGenerator.HighlightRadius.class, 15.0);
		for (int i : hiAtoms) {
			ids.put(mol.getAtom(i), 0);
		}
		mol.setProperty(HighlightGenerator.ID_MAP, ids);
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {		
		g.setColor(Color.white);
		g.fillRect(x, y, width, height);
		g.setColor(Color.black);
		FontMetrics metrics = c.getFontMetrics(g.getFont());
		String text = fieldStr + " : " + pot;
		int textW = SwingUtilities.computeStringWidth(metrics, text);
		int textH = metrics.getHeight();
		int idW = SwingUtilities.computeStringWidth(metrics, molID);
		int idH = metrics.getHeight();
		g.setColor(Color.BLACK);
		g.drawString(text, x + Math.max(5, (width - textW)/2), y + height - textH + 4);
		g.drawString(molID, x + Math.max(5, (width - idW)/2), y + idH);		
		Rectangle drawingArea = new Rectangle(x + 2, y + 2, width - 5, height - 5 - textH);
		AWTDrawVisitor visitor = new AWTDrawVisitor((Graphics2D) g);
		renderer.paint(mol, visitor, drawingArea, true);		
	}

	@Override
	public int getIconWidth() {
		return width;
	}

	@Override
	public int getIconHeight() {
		return height;
	}


	public AtomContainerRenderer getRenderer() {
		return renderer;
	}


	public IAtomContainer getMol() {
		return mol;
	}


	public RendererModel getRm() {
		return rm;
	}

}
