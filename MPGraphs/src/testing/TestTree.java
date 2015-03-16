package testing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import main.AdjMatrix;
import main.Molecule;
import main.SDFreader;
import main.SMSDpair;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;

import viewer.StructureDisplay;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.samples.ShortestPathDemo.MyEdgeStrokeFunction;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.DefaultVertexIconTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;

@SuppressWarnings("serial")
public class TestTree extends JPanel {
	Factory<Integer> edgeFactory = new Factory<Integer>() {
		int i=0;
		public Integer create() {
			return i++;
		}};
		
		
	private DelegateTree<Integer, Integer> graph;
	private VisualizationViewer<Integer, Integer> vv;
	private static final int ICON_WIDTH = 220;
	private static final int ICON_HEIGHT = 220;
	//private TreeLayout<Integer, Integer> layout;
	private ModRadialTreeLayout<Integer,Integer> radialLayout;
	private Map<Integer,Icon> iconMap;
	private Map<Integer, Double> edgeMap;
	private AdjMatrix adm;
	private int molIndex;
	private double norm;
	
	public TestTree(AdjMatrix adm, int molIndex, double norm) throws Exception {
		this.adm = adm;
		this.norm = norm;
		this.molIndex = molIndex;
		graph = new DelegateTree<Integer, Integer>();
		iconMap = new HashMap<>();
		edgeMap = new HashMap<>();
		createTree();
		radialLayout = new ModRadialTreeLayout<Integer,Integer>(graph, 250, 250);//, 900, 900);
        radialLayout.setSize(new Dimension(1200, 1000));
        vv =  new VisualizationViewer<Integer,Integer>(radialLayout, new Dimension(1200, 1000));
        vv.setBackground(Color.white);
        vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.QuadCurve<Integer, Integer>());
        vv.getRenderContext().setEdgeDrawPaintTransformer(new EdgeColor());
        vv.getRenderContext().setEdgeStrokeTransformer(new EdgeStrokeFunc());
        
        final DefaultVertexIconTransformer<Integer> vertexIconTransformer =
            	new DefaultVertexIconTransformer<Integer>();
        vertexIconTransformer.setIconMap(iconMap);
        vv.getRenderContext().setVertexIconTransformer(vertexIconTransformer);        
        this.add(vv);
        
        final DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();
        vv.setGraphMouse(graphMouse);
	}
	
	public void setMolIndex(int molIndex) throws Exception {
		this.molIndex = molIndex;
		createTree();
		radialLayout = new ModRadialTreeLayout<Integer,Integer>(graph, 250, 250);
		vv.setGraphLayout(radialLayout);
		vv.repaint();
	}
	
	
	private class EdgeColor implements Transformer<Integer,Paint> {
		public Paint transform(Integer i) {
			double dP = edgeMap.get(i);
			Color col;
			if (dP > 1) {
				col = Color.green;
			} else if (dP < -1) {
				col = Color.red;
			} else {
				col = Color.gray;
			}
			return col;
		}
	}

	private class EdgeStrokeFunc implements Transformer<Integer,Stroke> {
		protected final Stroke zero = new BasicStroke(1);
		protected final Stroke basic = new BasicStroke(3);
		protected final Stroke heavy = new BasicStroke(5);
		//protected final Stroke dotted = RenderContext.DOTTED;

		public Stroke transform(Integer i) {
			double dP = Math.abs(edgeMap.get(i));
			Stroke st;
			if (dP == 0) {
				st = zero;
			} else if (dP > 300) {
				st = heavy;
			} else {
				st = basic;
			}
			return st;
		}

	}
	
	private void createTree() throws Exception {
		Molecule rootMol = adm.getMolArray()[molIndex];
		StructureDisplay sd = new StructureDisplay(rootMol.getMol());
		String fieldStr = rootMol.getFieldName();
		iconMap.put(0, sd.getIcon(ICON_WIDTH, ICON_HEIGHT, null,
				rootMol.getPotency(), null, fieldStr));
		graph.setRoot(0);
		
		int childNum = 1;
		for (int i = 0; i < adm.getMCSMatrix().toArray()[molIndex].length; ++i) {
			if (adm.getMCSMatrix().getQuick(i, molIndex) != null) {
				SMSDpair pair = (SMSDpair) adm.getMCSMatrix().getQuick(i, molIndex);
				Molecule mol = adm.getMolArray()[i];
				sd = new StructureDisplay(mol.getMol());
				double dP = adm.getConnMatrix().getQuick(i, molIndex);
				Color col;
				if (dP > 1) {
					col = Color.green;
				} else if (dP < -1) {
					col = Color.red;
				} else {
					col = Color.blue;
				}
				Collection<Integer> highL = i > molIndex ? pair.targetHi() : pair.queryHi();
				iconMap.put(childNum, sd.getIcon(ICON_WIDTH, ICON_HEIGHT,
						highL, mol.getPotency(), col, fieldStr));
				graph.addEdge(edgeFactory.create(), 0, childNum);	
				edgeMap.put(childNum - 1, dP);		// edge numbering has to start from 0
				++childNum;
			}
		}
	}
	
	   public static void main(String[] args) throws Exception {
		   String file = args[0];
		   SDFreader sdf = new SDFreader(file);
		   TreeSet<Molecule> map = new TreeSet<>();
			//HashMap<Integer, Icon> map = new HashMap<>();
			int cnt = 0;
			for (IAtomContainer mol : sdf.sdfMap().keySet()) {
				//StructureDiagramGenerator sdg = new StructureDiagramGenerator();
//		        sdg.setMolecule(mol.clone());
//				sdg.generateCoordinates();
//		        mol = sdg.getMolecule();
//				StructureDisplay sd = new StructureDisplay(mol);
//				map.put(cnt, sd.getIcon(200, 200));
				//++cnt;
				String s = "Rfms Ic50 Um Hpad4 Avg";
				String val = sdf.sdfMap().get(mol)[1]; 	// index of field is 0
				if (val == null || mol.getAtomCount() == 0) {
					continue;
				}
				ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
				mol = ExtAtomContainerManipulator.removeHydrogens(mol);
				ExtAtomContainerManipulator.aromatizeCDK(mol);

				Molecule molec = new Molecule(mol, Double.parseDouble(val), s);
				map.add(molec);
				++cnt;
				//if (cnt == 20) break;
			}
			AdjMatrix adm = new AdjMatrix(map);
			
			int molIndex = 0;
			int j = 0;
			int max = 0;
			ArrayList<SMSDpair> arr = new ArrayList<>();
			while (j < adm.getMCSMatrix().toArray().length) {
			Object[] smsdArr = adm.getMCSMatrix().toArray()[j];
			for (Object pair : smsdArr) {
				if (pair != null) {
					arr.add((SMSDpair) pair);
				}
			}
			if (arr.size() > max){
				max = arr.size();
				molIndex = j;
			}
			++j;
			}
			
			
		   JFrame frame = new JFrame();
	        Container content = frame.getContentPane();
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        content.add(new TestTree(adm, molIndex, 100));
	        frame.pack();
	        frame.setVisible(true);
			}
}
