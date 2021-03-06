package viewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;

import main.Molecule;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.BoundsCalculator;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.SymbolVisibility;
import org.openscience.cdk.renderer.color.UniColor;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.HighlightGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;

public class StructureDisplay extends JPanel{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3927318447153631663L;
	private static final int W = 300;
	private static final int H = 300;
	private boolean isEmpty;
	private IAtomContainer mol;
	private Molecule molec;
	private Rectangle drawArea;
	private AtomContainerRenderer renderer;
	private int extra = -1;
//	private String extraName;
//	private double extraVal;
	public StructureDisplay() throws Exception {
		IAtomContainer mol1 = new AtomContainer();
		this.mol = mol1;
		isEmpty = true;
		drawMol();
	}
	public StructureDisplay(Molecule molec, int ind) throws Exception {
		IAtomContainer mol = molec.getMol();
		if (mol == null)
			throw new NullPointerException("No mol");
		this.mol = mol.clone();
		this.molec = molec;
		this.setExtraFieldInd(ind);
		isEmpty = false;
		drawMol();
	}
	private void drawMol() throws Exception {
		drawArea = new Rectangle(W-20, H-20);
		this.setPreferredSize(new Dimension(W,H));
		drawArea = new Rectangle(W-20, H-20);
		Font font = new Font("Verdana", Font.PLAIN, 18);
		//Font font = new Font("Verdana", Font.PLAIN, 26);
		List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
        generators.add(new BasicSceneGenerator());
        generators.add(new StandardGenerator(font));
        generators.add(new HighlightGenerator());
//        for (IAtom atom : mol.atoms()) {	// this is for labelling atoms  //uncomment for atom numbering
//            atom.setProperty(StandardGenerator.ANNOTATION_LABEL,
//                             Integer.toString(0 + mol.getAtomNumber(atom)));
//        }
               
        renderer = new AtomContainerRenderer(generators, 
        		new AWTFontManager());
        RendererModel rm = renderer.getRenderer2DModel();
        rm.set(BasicSceneGenerator.UseAntiAliasing.class, true);
        rm.set(StandardGenerator.AtomColor.class,
        		//new CDK2DAtomColors());
                new UniColor(Color.BLACK));
        rm.set(StandardGenerator.Visibility.class,
                SymbolVisibility.iupacRecommendationsWithoutTerminalCarbon());
        rm.set(StandardGenerator.StrokeRatio.class,
                0.4);
        rm.set(StandardGenerator.SymbolMarginRatio.class,
                4d);
	}
	
	public void clear() throws Exception {
		this.mol = new AtomContainer();
		isEmpty = true;
		drawMol();
	}
	
	public void highlightSelect(Collection<Integer> set) {
		RendererModel rm = renderer.getRenderer2DModel();
		rm.set(StandardGenerator.Highlighting.class,
                StandardGenerator.HighlightStyle.OuterGlow);
		rm.set(StandardGenerator.OuterGlowWidth.class,
                6d);
		HashSet<IAtom> atomSet = new HashSet<IAtom>();
		
		for (int i : set) {
			atomSet.add(mol.getAtom(i));
		}
		
		for (IAtom atom : atomSet) {
			atom.setProperty(StandardGenerator.HIGHLIGHT_COLOR, Color.cyan);
		}
		
		for (IBond bond : mol.bonds()) {
			IAtom a = bond.getAtom(0);
			IAtom b = bond.getAtom(1);
			if (atomSet.contains(a) && atomSet.contains(b)) {
				bond.setProperty(StandardGenerator.HIGHLIGHT_COLOR, Color.cyan);
			}
		}
		
		repaint();
	}
	
	public void highlightFlatSelect(Collection<Integer> set, Color col) {
		RendererModel rm = renderer.getRenderer2DModel();
		rm.set(StandardGenerator.Highlighting.class,
                StandardGenerator.HighlightStyle.Colored);
		rm.set(StandardGenerator.OuterGlowWidth.class,
                3d);
		HashSet<IAtom> atomSet = new HashSet<IAtom>();
		
		for (int i : set) {
			atomSet.add(mol.getAtom(i));
		}
		
		for (IAtom atom : atomSet) {
			atom.setProperty(StandardGenerator.HIGHLIGHT_COLOR, col);
		}
		
		for (IBond bond : mol.bonds()) {
			IAtom a = bond.getAtom(0);
			IAtom b = bond.getAtom(1);
			if (atomSet.contains(a) && atomSet.contains(b)) {
				bond.setProperty(StandardGenerator.HIGHLIGHT_COLOR, col);
			}
		}		
		repaint();
	}
	
	public void setMol(IAtomContainer mol) throws Exception {
		this.mol = mol.clone();
		drawMol();
		isEmpty = mol.isEmpty();
	}

	public IAtomContainer getMol() {return this.mol;}
	public boolean isEmpty() {return isEmpty;} 
	
	public Rectangle getDrawArea() {
		return drawArea;
	}
	public AtomContainerRenderer getRenderer() {
		return renderer;
	}
	
	public Icon getIcon(int w, int h, Collection<Integer> highL, final double pot,
			final Color col, final String fieldStr, final String id) {
		final int width = w;
		final int height = h;
		if (highL != null) {
			this.highlightSelect(highL);
			//this.highlightFlatSelect(highL, Color.cyan);//col);
		}
		return new Icon() {
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				g.setColor(Color.white);
				g.fillRoundRect(x, y, width, height, 10, 10);
				g.setColor(col);//(Color.black);
				g.drawRoundRect(x, y, width, height, 10, 10);
				FontMetrics metrics = c.getFontMetrics(g.getFont());
				String text = fieldStr.substring(0, 10) + " : " + pot;
				int textW = SwingUtilities.computeStringWidth(metrics, text);
				int textH = metrics.getHeight();
				int idW = SwingUtilities.computeStringWidth(metrics, id);
				int idH = metrics.getHeight();

				
				if (extra != -1) {
					String text2 = molec.getFieldStr()[extra].substring(0, 10)
							       + " : " + molec.getValues()[extra];
					g.setColor(Color.BLACK);
					g.drawString(text, x + 4, y + height - textH - 2);
					g.drawString(id, x + Math.max(4, (width - idW)/2), y + idH);
					g.setColor(Color.BLUE);
					g.drawString(text2, x + 4, y + height - 2);
					textH = 2*textH + 6; 
				} else {
					g.setColor(Color.BLACK);
					g.drawString(text, x + 4, y + height - 2);
					g.drawString(id, x + Math.max(4, (width - idW)/2), y + idH);
				}
				
				Rectangle drawingArea = new Rectangle(x + 4, y + 4, width - 3, height - 4 - textH);
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
			
		};
	}
	
	public Icon getFlatIcon(int w, int h, Collection<Integer> highL, final double pot,
			final Color col, final String fieldStr, final String id) {
		final int width = w;
		final int height = h;
		if (highL != null) {
			this.highlightSelect(highL);
		}
		return new Icon() {
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				g.setColor(Color.white);
				g.fillRect(x, y, width-2, height-2);
				g.setColor(col);//(Color.black);
				g.drawRect(x, y, width-2, height-2);
				FontMetrics metrics = c.getFontMetrics(g.getFont());
				String text = fieldStr + " : " + pot;
				int textW = SwingUtilities.computeStringWidth(metrics, text);
				int textH = metrics.getHeight();
				int idW = SwingUtilities.computeStringWidth(metrics, id);
				int idH = metrics.getHeight();
				g.setColor(Color.BLACK);
				g.drawString(text, x + Math.max(5, (width - textW)/2), y + height - textH + 4);
				g.drawString(id, x + Math.max(5, (width - idW)/2), y + idH);
				
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
		};
	}
	
	public int getExtraFieldInd() {
		return extra;
	}
	public void setExtraFieldInd(int extra) {
		this.extra = extra;
		repaint();
	}
	public void paintComponent(Graphics g) {
		this.setBackground(Color.white);
		g.fillRect(0, 0, W, H);
        super.paintComponent(g);
        g.setColor(Color.WHITE);
   
        AWTDrawVisitor visitor = new AWTDrawVisitor((Graphics2D) g);
        renderer.paint(mol, visitor, drawArea, true);
	}
}

