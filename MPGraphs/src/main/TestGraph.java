package main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeSet;

import javax.swing.JFrame;

import org.apache.commons.collections15.Factory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;

import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.matrix.GraphMatrixOperations;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;

public class TestGraph {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String file = args[0];
		Scanner sc = new Scanner(new BufferedReader(new FileReader(file)));
		double[][] arr = new double[243][243];
		
		int i = 0;
		int j = 0;
		while (sc.hasNext()) {
			String s = sc.next();
			double r = s == "NaN"? 0.0 : Double.parseDouble(s);
			arr[i][j] = r;
			++j;
			if (j == 243) {
				j = 0;
				++i;
			}
		}

//		SDFreader sdf = new SDFreader(file);
//		TreeSet<Molecule> map = new TreeSet<>();
//		int cnt = 0;
//		for (IAtomContainer mol : sdf.sdfMap().keySet()) {
//			String s = "Rfms Ic50 Um Hpad4 Avg";
//			String val = sdf.sdfMap().get(mol)[0]; 	// index of field is 0
//			if (val == null || mol.getAtomCount() == 0) {
//				continue;
//			}
//			ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
//			mol = ExtAtomContainerManipulator.removeHydrogens(mol);
//			ExtAtomContainerManipulator.aromatizeCDK(mol);
//
//			Molecule molec = new Molecule(mol, Double.parseDouble(val), s);
//			map.add(molec);
//			++cnt;
//			if (cnt == 80) break;
//		}


		Factory<Graph<Integer,Number>> graphFactory =
				new Factory<Graph<Integer,Number>>() {
			public Graph<Integer,Number> create() {
				return new UndirectedSparseGraph<Integer,Number>();
			}
		};

		Factory<Integer> vertexFactory = new Factory<Integer>() {
			int count;
			public Integer create() {
				return count++;
			}};
			Factory<Number> edgeFactory = new Factory<Number>() {
				int count;
				public Number create() {
					return count++;
				}};

				Map<Number, Number> wtMap = new HashMap<>();

//				final AdjMatrix adm = new AdjMatrix(map);
				DenseDoubleMatrix2D matrix = new DenseDoubleMatrix2D(arr);
				Graph<? extends Object, ? extends Object> g = 
						GraphMatrixOperations .matrixToGraph(matrix, graphFactory,
								vertexFactory, edgeFactory, wtMap);
				final VisualizationViewer<Integer,Number> vv = 
						new VisualizationViewer<Integer,Number>(new FRLayout(g));
				vv.setPreferredSize(new Dimension(1000, 1000));
				//System.out.println(adm.getCCDoubleMatr()[0]);
				//System.out.println(wtMap.values());


				//PrintStream out = new PrintStream(new FileOutputStream("output.txt"));
				//System.setOut(out);
				//System.out.println(adm.getConnMatrix());
				//final SideDisplay disp = new SideDisplay();
				//final HeatMap heat = new HeatMap(adm, true, disp, Gradient.GRADIENT_RED_TO_GREEN);
				final JFrame f = new JFrame("Graph");


				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
						//f.setSize(1300, 1000);
						f.setLayout(new BorderLayout());
						f.add(vv);
						f.pack();
						f.setVisible(true);
					}
				});



	}

}
