package testing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JToolTip;

import main.AdjMatrix;
import main.Gradient;
import main.Molecule;
import main.SDFreader;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;

import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.matrix.GraphMatrixOperations;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;

public class TestGraph {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String file = args[0];
//		Scanner sc = new Scanner(new BufferedReader(new FileReader(file)));
//		double[][] arr = new double[243][243];
//		
//		int i = 0;
//		int j = 0;
//		while (sc.hasNext()) {
//			String s = sc.next();
//			double r = s == "NaN"? 0.0 : Double.parseDouble(s);
//			arr[i][j] = r;
//			++j;
//			if (j == 243) {
//				j = 0;
//				++i;
//			}
//		}

		SDFreader sdf = new SDFreader(file);
		final Color[] colors = Gradient.GRADIENT_RED_TO_GREEN;
		TreeSet<Molecule> map = new TreeSet<>();
		int cnt = 0;
		for (IAtomContainer mol : sdf.sdfMap().keySet()) {
			String s = "Rfms Ic50 Um Hpad4 Avg";
			String val = sdf.sdfMap().get(mol)[0]; 	// index of field is 0
			if (val == null || mol.getAtomCount() == 0) {
				continue;
			}
			ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
			mol = ExtAtomContainerManipulator.removeHydrogens(mol);
			ExtAtomContainerManipulator.aromatizeCDK(mol);

			Molecule molec = new Molecule(mol, Double.parseDouble(val), s);
			map.add(molec);
			++cnt;
			if (cnt == 80) break;
		}


		Factory<Graph<Integer,Number>> graphFactory =
				new Factory<Graph<Integer,Number>>() {
			public Graph<Integer,Number> create() {
				return new UndirectedSparseGraph<Integer,Number>();
			}
		};

		Factory<Integer> vertexFactory = new Factory<Integer>() {
			int count = 0;		// assuming the order of vertices is the same as in adm.getmolArr
			public Integer create() {
				return count++;
			}};
			Factory<Number> edgeFactory = new Factory<Number>() {
				int count;
				public Number create() {
					return count++;
				}};

				Map<Number, Number> wtMap = new HashMap<>();

				final AdjMatrix adm = new AdjMatrix(map);
				//				DenseDoubleMatrix2D matrix = new DenseDoubleMatrix2D(arr);
				Graph<? extends Object, ? extends Object> g = 
						GraphMatrixOperations .matrixToGraph(adm.getConnMatrix(), graphFactory,
								vertexFactory, edgeFactory, wtMap);
				Layout<Integer, Number> layout = new FRLayout(g);
				layout.setSize(new Dimension(800,800));
				final VisualizationViewer<Integer,Number> vv = 
						new VisualizationViewer<Integer,Number>(layout) {
					/**
							 * 
							 */
							private static final long serialVersionUID = 1L;

					public JToolTip createToolTip() {
						
						//if (overVertex)
						//	return molToolTip;
						//else
							return new JToolTip();
					}
				};
				vv.setPreferredSize(new Dimension(900, 900));
				
				// Setup up a new vertex to paint transformer...
				 Transformer<Integer,Paint> vertexPaint = new Transformer<Integer,Paint>() {
				 public Paint transform(Integer i) {
					 double pot = Math.min(100, adm.getMolArray()[i].getPotency());
					 double norm = pot / 100; // Assuming max=100 and min=0
					 int colorIndex = (int) Math.floor(norm * (colors.length - 1));
					 return colors[colorIndex];
				 }
				 };
				 vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
				
				DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
				gm.setMode(ModalGraphMouse.Mode.TRANSFORMING);
				vv.setGraphMouse(gm);
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
