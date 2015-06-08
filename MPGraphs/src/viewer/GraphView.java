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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingWorker;
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
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;

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
	private Map<Integer,Paint> edgePaints =
		LazyMap.<Integer,Paint>decorate(new HashMap<Integer,Paint>(),
				new ConstantTransformer(Color.blue));
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
	
	private void initVV() {
		graph = GraphMatrixOperations .matrixToGraph(heat.getMatrix(),
				graphFactory, vertexFactory, edgeFactory);
		layout = new AggregateLayout<Integer,Integer>(new FRLayout<Integer,Integer>(graph));
		//		layout.setMaxIterations(100);
		//layout.setInitializer(new RandomLocationTransformer<Integer>(new Dimension(1200, 800), 0));
		layout.setSize(new Dimension(1200, 800));
		vv = new VisualizationViewer<Integer,Integer>(layout, new Dimension(1200, 900));
		vv.setBackground(Color.white);
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

		vv.getRenderContext().setEdgeDrawPaintTransformer(MapTransformer.<Integer,Paint>getInstance(edgePaints));
		vv.getRenderContext().setEdgeStrokeTransformer(new Transformer<Integer,Stroke>() {
			protected final Stroke THIN = new BasicStroke(1);
			protected final Stroke THICK= new BasicStroke(2);
			public Stroke transform(Integer e)
			{
				Paint c = edgePaints.get(e);
				if (c == Color.LIGHT_GRAY)
					return THIN;
				else 
					return THICK;
			}
		});
		
		//add restart button
		JButton scramble = new JButton("Restart");
		scramble.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Layout layout = vv.getGraphLayout();
				layout.initialize();
				Relaxer relaxer = vv.getModel().getRelaxer();
				if(relaxer != null) {
					relaxer.stop();
					relaxer.prerelax();
					relaxer.relax();
				}
			}

		});
		
		DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
		vv.setGraphMouse(gm);
		
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

		final String COMMANDSTRING = "Edges removed for clusters: ";
		final String eastSize = COMMANDSTRING + edgeBetweennessSlider.getValue();
		
		final TitledBorder sliderBorder = BorderFactory.createTitledBorder(eastSize);
		eastControls.setBorder(sliderBorder);
		//eastControls.add(eastSize);
		eastControls.add(Box.createVerticalGlue());
		
		clusterAndRecolor(layout, 0, similarColors);
		
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
				    	 clusterAndRecolor(layout, numEdgesToRemove, similarColors);
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
		p.add(gm.getModeComboBox());
		south.add(p);
		
		this.setLayout(new BorderLayout(0, 0));
		this.add(vv);
		this.add(south, BorderLayout.SOUTH);
		this.validate();
		this.repaint();
	}
	
	public void update() {
		this.removeAll();
		//initVviewer ivv = new initVviewer();
		//ivv.execute();
		initVV();
	}
	
	public GraphView(HeatMap heat, double norm) {
		//super();
		this.heat = heat;
		this.norm = norm;
		//this.molIndex = molIndex;
		initFactories();
		//initVviewer ivv = new initVviewer();
		//ivv.execute();
		initVV();
	}
	
	public void clusterAndRecolor(AggregateLayout<Integer,Integer> layout,
			int numEdgesToRemove,
			Color[] colors) {
			//Now cluster the vertices by removing the top 50 edges with highest betweenness
			//		if (numEdgesToRemove == 0) {
			//			colorCluster( g.getVertices(), colors[0] );
			//		} else {
			
			Graph<Integer,Integer> g = layout.getGraph();
	        layout.removeAll();

			EdgeBetweennessClusterer<Integer,Integer> clusterer =
				new EdgeBetweennessClusterer<Integer,Integer>(numEdgesToRemove);
			Set<Set<Integer>> clusterSet = clusterer.transform(g);
			List<Integer> edges = clusterer.getEdgesRemoved();

			int i = 0;
			//Set the colors of each node so that each cluster's vertices have the same color
			for (Iterator<Set<Integer>> cIt = clusterSet.iterator(); cIt.hasNext();) {

				Set<Integer> vertices = cIt.next();
				Color c = colors[i % colors.length];

				colorCluster(vertices, c);
				i++;
			}
			for (Integer e : g.getEdges()) {

				if (edges.contains(e)) {
					edgePaints.put(e, Color.lightGray);
				} else {
					edgePaints.put(e, Color.black);
				}
			}

		}

		private void colorCluster(Set<Integer> vertices, Color c) {
			for (Integer v : vertices) {
				vertexPaints.put(v, c);
			}
		}
		
//		private void groupCluster(AggregateLayout<Integer,Integer> layout, Set<Integer> vertices) {
//			if(vertices.size() < layout.getGraph().getVertexCount()) {
//				Point2D center = layout.transform(vertices.iterator().next());
//				Graph<Integer,Integer> subGraph = SparseMultigraph.<Integer,Integer>getFactory().create();
//				for(Integer v : vertices) {
//					subGraph.addVertex(v);
//				}
//				Layout<Integer,Integer> subLayout = 
//					new CircleLayout<Integer,Integer>(subGraph);
//				subLayout.setInitializer(vv.getGraphLayout());
//				subLayout.setSize(new Dimension(40,40));
//
//				layout.put(subLayout,center);
//				vv.repaint();
//			}
//		}

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
				if (cnt == 5) break;
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
	        //pt.setMolIndex(molIndex);
	        content.add(pt);
	        frame.pack();
	        frame.setVisible(true);
			}

}
