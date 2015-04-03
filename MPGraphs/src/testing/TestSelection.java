package testing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.media.j3d.Geometry;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import javax.vecmath.Point2d;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.geometry.GeometryUtil;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.SymbolVisibility;
import org.openscience.cdk.renderer.generators.standard.SelectionVisibility;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.smiles.SmilesParser;

import viewer.StructureDisplay;

public class TestSelection extends JPanel {
	private AtomContainerRenderer renderer;
	private static final int W = 500;
	private static final int H = 500;
	private Rectangle drawArea;
	private IAtomContainer mol;
	private IAtom atomSel = null;
	private IBond bondSel = null;
	private static final Color HIL = Color.MAGENTA;
	private RendererModel rm;
	
	public TestSelection(IAtomContainer ac) throws Exception {
		StructureDisplay tdp = new StructureDisplay(ac);
		mol = ac;
		renderer = tdp.getRenderer();
		drawArea = new Rectangle(W-20, H-20);
		MouseListener listener = new MouseListener();
		this.addMouseListener(listener);
		this.addMouseMotionListener(listener);
		rm = renderer.getRenderer2DModel();
		rm.set(StandardGenerator.Visibility.class, SelectionVisibility.all
				(SymbolVisibility.iupacRecommendationsWithoutTerminalCarbon()));
//		for (IAtom atom : mol.atoms()) {	// this is for labelling atoms
//			atom.setProperty(StandardGenerator.ANNOTATION_LABEL,
//					Integer.toString(1 + mol.getAtomNumber(atom)));
//		}
      
	}
	
	public void paintComponent(Graphics g) {
		this.setBackground(Color.white);
		g.fillRect(0, 0, W, H);
        super.paintComponent(g);
        g.setColor(Color.WHITE);
   
        AWTDrawVisitor visitor = new AWTDrawVisitor((Graphics2D) g);
        renderer.paint(mol, visitor, drawArea, true);
	}
	
	public class MouseListener extends MouseInputAdapter {
		public void mouseClicked(MouseEvent e) {
			Point p = e.getPoint();
			Point2d modelPoint = renderer.toModelCoordinates(p.getX(), p.getY());
			IAtom atom = GeometryUtil.getClosestAtom(modelPoint.getX(), modelPoint.getY(), mol);
			System.out.println(atom);
			System.out.println(atom.getPoint2d() + " " + modelPoint);
			System.out.println(atom.getPoint2d().distance(modelPoint));
		}
		
		public void mouseMoved(MouseEvent e) {
			Point p = e.getPoint();
			Point2d modelPoint = renderer.toModelCoordinates(p.getX(), p.getY());
			IAtom atom = GeometryUtil.getClosestAtom(modelPoint.getX(), modelPoint.getY(), mol);
			IBond bond = GeometryUtil.getClosestBond(modelPoint.getX(), modelPoint.getY(), mol);
			double da = atom.getPoint2d().distanceSquared(modelPoint);
			double db = bond.get2DCenter().distanceSquared(modelPoint);
			if (da > 0.5 && db > 0.5) {
				if (atomSel == null && bondSel ==  null) {
					return;
				}
				if (atomSel != null) {
					//atomSel.setProperty(StandardGenerator.HIGHLIGHT_COLOR, NO_HIL);
					atomSel.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
					atomSel = null;
				}
				if (bondSel != null) {
					//bondSel.setProperty(StandardGenerator.HIGHLIGHT_COLOR, NO_HIL);
					bondSel.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
					bondSel = null;
				}
			} else if (da < db) {
				if (bondSel != null) {
					bondSel.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
					bondSel = null;
				}
				
				if (atom.equals(atomSel)) {
					return;
				} else {
					if (atomSel != null) {
						atomSel.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
					}
					atomSel = atom;
					atomSel.setProperty(StandardGenerator.HIGHLIGHT_COLOR, HIL);
				}
			} else {
				if (atomSel != null) {
					atomSel.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
					atomSel = null;
				}
				if (bond.equals(bondSel)) {
					return;
				} else {
					if (bondSel != null) {
						bondSel.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
					}
					bondSel = bond;
					bondSel.setProperty(StandardGenerator.HIGHLIGHT_COLOR, HIL);
				}
 			}
			repaint();
		}
	}

	public static void showGUI(String smi) throws Exception {
		final JFrame f = new JFrame("Test");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().setPreferredSize(new Dimension(600, 600));
		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		try {
			IAtomContainer mol1 = sp.parseSmiles(smi);
			StructureDiagramGenerator sdg = new StructureDiagramGenerator();
	        sdg.setMolecule(mol1.clone());
			sdg.generateCoordinates();
	        mol1 = sdg.getMolecule();
			TestSelection ts =  new TestSelection(mol1);
			
			f.add(ts);
			f.pack();
			f.setVisible(true);
		} catch (InvalidSmilesException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String smi = "CN1C2=CC=C(C=C2N=C1C1=CC2=CC=CN=C2N1CC1CC1)C(=O)N1CC[C@H](O)[C@H](N)C1"; //args[0]; 
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					showGUI(smi);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
}
