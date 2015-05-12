package testing;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.collections15.Factory;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;

import viewer.HeatMap;
import viewer.SideDisplay;

import edu.uci.ics.jung.algorithms.layout.AggregateLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.util.RandomLocationTransformer;
import edu.uci.ics.jung.algorithms.matrix.GraphMatrixOperations;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import main.AdjMatrix;
import main.Gradient;
import main.Molecule;
import main.SDFreader;
import main.SMSDpair;

public class TestGraphView extends JPanel {
	private VisualizationViewer<Integer, Integer> vv;
	private Factory<Integer> edgeFactory;
	private Factory<Integer> vertexFactory;
	private Factory<Graph<Integer, Integer>> graphFactory;
	private AggregateLayout<Integer,Integer> layout;
	private Graph<Integer, Integer> graph;
	private HeatMap heat;
	private double norm;
	private int molIndex;
	
	private void initFactories() {
		vertexFactory = new Factory<Integer>() {
			int count = 0;		// assuming the order of vertices is the same as in adm.getmolArr
			public Integer create() {
				return count++;
			}};
		edgeFactory = new Factory<Integer>() {
			int count = 0;
			public Integer create() {
				return count++;
			}};		
		graphFactory = new Factory<Graph<Integer,Integer>>() {
			public SparseMultigraph<Integer,Integer> create() {
				return new SparseMultigraph<Integer,Integer>();
			}
		};	
	}
	
	public TestGraphView(HeatMap heat, int molIndex, double norm) {
		this.heat = heat;
		this.norm = norm;
		this.molIndex = molIndex;
		initFactories();	
		graph = GraphMatrixOperations .matrixToGraph(heat.getMatrix(),
				graphFactory, vertexFactory, edgeFactory);
		layout = new AggregateLayout<Integer,Integer>(new FRLayout<Integer,Integer>(graph));
//		layout.setMaxIterations(100);
        layout.setInitializer(new RandomLocationTransformer<Integer>(new Dimension(1200, 800), 0));
        layout.setSize(new Dimension(1200, 800));
		vv = new VisualizationViewer<Integer,Integer>(layout, new Dimension(1200, 900));
		vv.setBackground(Color.white);
		
		this.add(vv);
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		   String file = args[0];
		   SDFreader sdf = new SDFreader(file);
		   TreeSet<Molecule> map = new TreeSet<>();
			int cnt = 0;
			for (IAtomContainer mol : sdf.sdfMap().keySet()) {
				String s = "Rfms Ic50 Um Hpad4 Avg";
				String val = sdf.sdfMap().get(mol)[1]; 	// index of field is 0
				if (val == null || mol.getAtomCount() == 0) {
					continue;
				}
				ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
				mol = ExtAtomContainerManipulator.removeHydrogens(mol);
				ExtAtomContainerManipulator.aromatizeCDK(mol);

				Molecule molec = new Molecule(mol, Double.parseDouble(val), s);
				molec.setMolID(Integer.toString(cnt + 100000));
				map.add(molec);
				++cnt;
				if (cnt == 120) break;
			}
			AdjMatrix adm = new AdjMatrix(map);
			SideDisplay disp = new SideDisplay();
			HeatMap heat = new HeatMap(adm, true, disp, Gradient.GRADIENT_RED_TO_GREEN);
			
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
	        TestGraphView pt = new TestGraphView(heat, 0, 100);
	        //pt.setMolIndex(molIndex);
	        content.add(pt);
	        frame.pack();
	        frame.setVisible(true);
			}

}
