package viewer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToolTip;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import main.AdjMatrix;
import main.Gradient;
import main.Molecule;
import main.SDFreader;
import main.SMSDpair;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;
import org.apache.commons.collections15.functors.MapTransformer;
import org.apache.commons.collections15.map.LazyMap;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;

import edu.uci.ics.jung.algorithms.cluster.EdgeBetweennessClusterer;
import edu.uci.ics.jung.algorithms.layout.AggregateLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.util.Relaxer;
import edu.uci.ics.jung.algorithms.matrix.GraphMatrixOperations;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer;

public class GraphView extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6291448645703591947L;
	private VisualizationViewer<Integer, Integer> vv;
	private Factory<Integer> edgeFactory;
	private Factory<Integer> vertexFactory;
	private Factory<Graph<Integer, Integer>> graphFactory;
	private AggregateLayout<Integer,Integer> layout;
	private Graph<Integer, Integer> graph;
	private HeatMap heat;
	private double norm;
	private Map<Integer,Paint> vertexPaints = 
			LazyMap.<Integer,Paint>decorate(new HashMap<Integer,Paint>(),
					new ConstantTransformer(Color.white));
//	private Map<Integer,Paint> edgePaints =
//		LazyMap.<Integer,Paint>decorate(new HashMap<Integer,Paint>(),
//				new ConstantTransformer(Color.blue));
	
	private boolean onVertex = false;
	private StructureDisplay tdp1;
	private ImageToolTip molToolTip;
	private Map<Integer, Molecule> vertexMap;
	
	
	public final Color[] similarColors =
		{
			new Color(216, 134, 134),
			new Color(135, 137, 211),
			new Color(134, 206, 189),
			new Color(206, 176, 134),
			new Color(194, 204, 134),
			new Color(145, 214, 134),
			new Color(133, 178, 209),
			new Color(103, 148, 255),
			new Color(60, 220, 220),
			new Color(30, 250, 100)
		};
	private DefaultModalGraphMouse<Integer, Integer> graphMouse;
	
	private class PTGraphMouse<V,E> extends DefaultModalGraphMouse<V,E> {
		public void mouseClicked(MouseEvent e) {
			GraphView.this.firePropertyChange("PTreeState", true, false);
		}
	}
	
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
			public UndirectedSparseGraph<Integer,Integer> create() {
				return new UndirectedSparseGraph<Integer,Integer>();
//			public DirectedSparseGraph<Integer,Integer> create() {
//				return new DirectedSparseGraph<Integer,Integer>();
			}
		};	
	}
	
	private void initTip() throws Exception {
		graphMouse = new PTGraphMouse<>();
		tdp1 = new StructureDisplay();
		molToolTip = new ImageToolTip(tdp1);
	}
	
	private void initVV() {
		graph = GraphMatrixOperations .matrixToGraph(heat.getMatrix(),
				graphFactory, vertexFactory, edgeFactory);
		
		vertexMap = new HashMap<>();
		for (int i = 0; i < heat.getMolArray().length; ++i) {
				Molecule mol = heat.getMolArray()[i];
				vertexMap.put(i, mol);
			}
		layout = new AggregateLayout<Integer,Integer>(new FRLayout<Integer,Integer>(graph));
		layout.setSize(new Dimension(1200, 800));
		vv = new VisualizationViewer<Integer,Integer>(layout, new Dimension(1200, 900)){
			/**
			 * 
			 */
			private static final long serialVersionUID = -5247947698370164030L;

			public JToolTip createToolTip() {
				if (onVertex) {
					onVertex = false;
					ToolTipManager.sharedInstance().setReshowDelay(0);
					ToolTipManager.sharedInstance().setInitialDelay(0);
					return molToolTip;
				} else {
					return new JToolTip();
				}
			}
		};
		vv.setBackground(Color.white);
		vv.setGraphMouse(graphMouse);
		//Tell the renderer to use our own customized color rendering
		vv.getRenderContext().setVertexFillPaintTransformer(MapTransformer.<Integer,Paint>getInstance(vertexPaints));
		vv.getRenderContext().setVertexDrawPaintTransformer(new Transformer<Integer,Paint>() {
			public Paint transform(Integer v) {
				if(vv.getPickedVertexState().isPicked((Integer) v)) {
					return Color.cyan;
				} else {
					return Color.BLACK;						}
			}
		});
		
        vv.setVertexToolTipTransformer(new Transformer<Integer, String>() {			
			@Override
			public String transform(Integer v) {
				Molecule mol = vertexMap.get(v);
				onVertex = true;
				ToolTipManager.sharedInstance().setEnabled(true);
				//System.out.println(vertexMap + " " + graph.getVertexCount());
				try {
					tdp1.setMol(mol.getMol());				
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return mol.getMolID();
			}
		});
        vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.W);        
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        
//		vv.getRenderContext().setEdgeDrawPaintTransformer(MapTransformer.<Integer,Paint>getInstance(edgePaints));
//		vv.getRenderContext().setEdgeStrokeTransformer(new Transformer<Integer,Stroke>() {
//			protected final Stroke THIN = new BasicStroke(1);
//			protected final Stroke THICK= new BasicStroke(2);
//			public Stroke transform(Integer e)
//			{
//				Paint c = edgePaints.get(e);
//				if (c == Color.LIGHT_GRAY)
//					return THIN;
//				else 
//					return THICK;
//			}
//		});
		
		//add restart button
		JButton scramble = new JButton("Restart");
		scramble.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				//Layout layout = vv.getGraphLayout();
				SwingWorker<Layout, Void> worker 
				= new SwingWorker<Layout, Void>() {				 
					@Override
					public Layout doInBackground() {
						Layout lt = vv.getGraphLayout();
						lt.initialize();
						return lt;
					}

					public void done() {
						try {
							layout = (AggregateLayout<Integer, Integer>) get();
						} catch (InterruptedException | ExecutionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
				//worker.execute();
				layout.initialize();
				Relaxer relaxer = vv.getModel().getRelaxer();
				if(relaxer != null) {
					relaxer.stop();
					relaxer.prerelax();
					relaxer.relax();
				}
			}

		});
		
//		DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
//		vv.setGraphMouse(gm);
		
		//Create slider to adjust the number of edges to remove when clustering
		final JSlider edgeBetweennessSlider = new JSlider(JSlider.HORIZONTAL);
        edgeBetweennessSlider.setBackground(Color.WHITE);
		edgeBetweennessSlider.setPreferredSize(new Dimension(210, 50));
		edgeBetweennessSlider.setPaintTicks(true);
		edgeBetweennessSlider.setMaximum(graph.getEdgeCount());
		edgeBetweennessSlider.setMinimum(0);
		edgeBetweennessSlider.setValue(0);
		edgeBetweennessSlider.setMajorTickSpacing(10);
		edgeBetweennessSlider.setPaintLabels(true);
		edgeBetweennessSlider.setPaintTicks(true);
		
//		edgeBetweennessSlider.setBorder(BorderFactory.createLineBorder(Color.black));
		//TO DO: edgeBetweennessSlider.add(new JLabel("Node Size (PageRank With Priors):"));
		//I also want the slider value to appear
		final JPanel eastControls = new JPanel();
		eastControls.setOpaque(true);
		eastControls.setLayout(new BoxLayout(eastControls, BoxLayout.Y_AXIS));
		eastControls.add(Box.createVerticalGlue());
		eastControls.add(edgeBetweennessSlider);

		final String COMMANDSTRING = "Edges removed: ";
		final String eastSize = COMMANDSTRING + edgeBetweennessSlider.getValue();
		
		final TitledBorder sliderBorder = BorderFactory.createTitledBorder(eastSize);
		eastControls.setBorder(sliderBorder);
		//eastControls.add(eastSize);
		eastControls.add(Box.createVerticalGlue());
		
		clusterAndRecolor(0, similarColors);
		
		edgeBetweennessSlider.addChangeListener(new ChangeListener() {
			private int numEdgesToRemove;

			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					this.numEdgesToRemove = source.getValue();
					SwingWorker<Void, Void> worker 
					= new SwingWorker<Void, Void>() {				 
						@Override
						public Void doInBackground() {
							clusterAndRecolor(numEdgesToRemove, similarColors);
							vv.validate();
							vv.repaint();
							return null;
						}
					};
					worker.execute();
					sliderBorder.setTitle(
							COMMANDSTRING + edgeBetweennessSlider.getValue());
					eastControls.repaint();
					vv.validate();
					vv.repaint();
				}
			}
		});
		
		this.add(new GraphZoomScrollPane(vv));
		JPanel south = new JPanel();
		JPanel grid = new JPanel(new GridLayout(2,1));
		grid.add(scramble);

		south.add(grid);
		south.add(eastControls);
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createTitledBorder("Mouse Mode"));
		p.add(graphMouse.getModeComboBox());
		south.add(p);
		
		this.setLayout(new BorderLayout(0, 0));
		this.add(vv);
		this.add(south, BorderLayout.SOUTH);
		this.validate();
		this.repaint();
	}
	
	public void update() {
		this.removeAll();
		initFactories();
		initVV();
	}
	
	public GraphView(HeatMap heat, double norm) {
		//super();
		this.heat = heat;
		this.norm = norm;
		initFactories();
		try {
			initTip();
			initVV();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void clusterAndRecolor(
			int numEdgesToRemove,
			Color[] colors) {
			//Now cluster the vertices by removing the top 50 edges with highest betweenness
			//		if (numEdgesToRemove == 0) {
			//			colorCluster( g.getVertices(), colors[0] );
			//		} else {
			
			//Graph<Integer,Integer> g = layout.getGraph();
	        //layout.removeAll();

			EdgeBetweennessClusterer<Integer,Integer> clusterer =
				new EdgeBetweennessClusterer<Integer,Integer>(numEdgesToRemove);
			Set<Set<Integer>> clusterSet = clusterer.transform(graph);
			List<Integer> edges = clusterer.getEdgesRemoved();

			int i = 0;
			//Set the colors of each node so that each cluster's vertices have the same color
			for (Iterator<Set<Integer>> cIt = clusterSet.iterator(); cIt.hasNext();) {

				Set<Integer> vertices = cIt.next();
				Color c = colors[i % colors.length];

				colorCluster(vertices, c);
				i++;
			}
			
//	Keep edges colouring out as it slows down for large |E|			
//			for (Integer e : graph.getEdges()) {
//
//				if (edges.contains(e)) {
//					edgePaints.put(e, Color.lightGray);
//				} else {
//					edgePaints.put(e, Color.black);
//				}
//			}

		}

		private void colorCluster(Set<Integer> vertices, Color c) {
			for (Integer v : vertices) {
				vertexPaints.put(v, c);
			}
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
				if (cnt == 10) break;
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
	        GraphView pt = new GraphView(heat, 100);
	        pt.update();
	        //pt.setMolIndex(molIndex);
	        content.add(pt);
	        frame.pack();
	        frame.setVisible(true);
			}

}
