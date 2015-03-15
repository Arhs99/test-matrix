package testing;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import main.AdjMatrix;
import main.DeltaP;
import main.Gradient;
import main.Molecule;
import main.SDFreader;
import main.SMSDpair;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.functors.ConstantTransformer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;

import viewer.StructureDisplay;

import edu.uci.ics.jung.algorithms.layout.PolarPoint;
import edu.uci.ics.jung.algorithms.layout.RadialTreeLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Tree;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.samples.RadialTreeLensDemo;
import edu.uci.ics.jung.samples.TreeCollapseDemo;
import edu.uci.ics.jung.samples.VertexImageShaperDemo.DemoVertexIconShapeTransformer;
import edu.uci.ics.jung.samples.VertexImageShaperDemo.DemoVertexIconTransformer;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.DefaultVertexIconTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.EllipseVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.VertexIconShapeTransformer;

@SuppressWarnings("serial")
public class TestTree extends JPanel {
	Factory<Integer> edgeFactory = new Factory<Integer>() {
		int i=0;
		public Integer create() {
			return i++;
		}};
		
		
	private DelegateTree<Integer, Integer> graph;
	private VisualizationViewer<Integer, Integer> vv;
	private static final int ICON_WIDTH = 200;
	private static final int ICON_HEIGHT = 200;
	//private TreeLayout<Integer, Integer> layout;
	private ModRadialTreeLayout<Integer,Integer> radialLayout;
	private Map<Integer,Icon> iconMap;
	
	public TestTree(ArrayList<SMSDpair> arr, double norm) throws Exception {
		//Graph<Integer, Integer> base =	new DirectedSparseMultigraph<Integer,Integer>();
		graph = new DelegateTree<Integer, Integer>();
		iconMap = new TreeMap<>();		// use a tree to keep stored molecules sorted
		createTree(arr, norm);
		radialLayout = new ModRadialTreeLayout<Integer,Integer>(graph, 220, 220);//, 900, 900);
        radialLayout.setSize(new Dimension(1000,1200));
        vv =  new VisualizationViewer<Integer,Integer>(radialLayout, new Dimension(1000,1200));
        vv.setBackground(Color.white);
        vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.QuadCurve<Integer, Integer>());
        vv.getRenderContext().setArrowFillPaintTransformer(new ConstantTransformer(Color.lightGray));
        final DefaultVertexIconTransformer<Integer> vertexIconTransformer =
            	new DefaultVertexIconTransformer<Integer>();
        vertexIconTransformer.setIconMap(iconMap);
        vv.getRenderContext().setVertexIconTransformer(vertexIconTransformer);        
        this.add(vv);
        //final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
        //this.add(panel);
        
        final DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();
        vv.setGraphMouse(graphMouse);
	}
	private void createTree(ArrayList<SMSDpair> arr, double norm) throws Exception {
		int numAdj = arr.size() - 1;
		System.out.println(arr.size());
		IAtomContainer rootMol = arr.get(0).rxnmol();
		StructureDisplay sd = new StructureDisplay(rootMol);
		iconMap.put(0, sd.getIcon(ICON_WIDTH, ICON_HEIGHT, null, 0.0, null));
		graph.setRoot(0);
		Color col;
		for (int i = 0; i < numAdj; ++i) {
			IAtomContainer mol = arr.get(i).prdmol();
			sd = new StructureDisplay(mol);
//			double diff = DeltaP.logDiff(arr.get(i)., arr.get(i).prdmol(), norm);
//			if ( > 0.05) {
//				col = Color.GREEN;
//			} else if (pot < -0.05) {
//				
//			}
			iconMap.put(i + 1, sd.getIcon(ICON_WIDTH, ICON_HEIGHT, arr.get(i).targetHi(), 0.0, Color.red));
			graph.addEdge(edgeFactory.create(), 0, i + 1);
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
			
			int molIndex = 5;
			
			ArrayList<SMSDpair> arr = new ArrayList<>();
			while (true) {
			Object[] smsdArr = adm.getMCSMatrix().toArray()[molIndex];
			for (Object pair : smsdArr) {
				if (pair != null) {
					arr.add((SMSDpair) pair);
				}
			}
			if (arr.size() > 1) break;
			++molIndex;
			}
		   JFrame frame = new JFrame();
	        Container content = frame.getContentPane();
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        System.out.println(arr.size());
	        content.add(new TestTree(arr, 100.0));
	        frame.pack();
	        frame.setVisible(true);
			}
}
