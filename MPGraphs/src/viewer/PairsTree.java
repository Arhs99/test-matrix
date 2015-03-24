package viewer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import main.AdjMatrix;
import main.Gradient;
import main.Molecule;
import main.SDFreader;
import main.SMSDpair;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.RadialTreeLayout;
import edu.uci.ics.jung.algorithms.layout.util.RandomLocationTransformer;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.samples.ShortestPathDemo.MyEdgeStrokeFunction;
import edu.uci.ics.jung.samples.VertexImageShaperDemo.DemoVertexIconShapeTransformer;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.DefaultVertexIconTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.EllipseVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.VertexIconShapeTransformer;

@SuppressWarnings("serial")
public class PairsTree extends JPanel {
	Factory<Integer> edgeFactory;
		
		
	private DelegateTree<Integer, Integer> graph;
	private VisualizationViewer<Integer, Integer> vv;
	private static final int ICON_WIDTH = 180;
	private static final int ICON_HEIGHT = 180;
	//private TreeLayout<Integer, Integer> layout;
	//private RadialTreeLayout<Integer,Integer> radialLayout;
	private Map<Integer,Icon> iconMap;
	private Map<Integer, Double> edgeMap;
	private HeatMap heat;
	//private AdjMatrix adm;
	private int molIndex;
	private double norm;
	
	public PairsTree(HeatMap heat, int molIndex, double norm) throws Exception {
		this.heat = heat;
		edgeFactory = new Factory<Integer>() {
			int i=0;
			public Integer create() {
				return i++;
			}};
		//this.adm = adm;
		this.norm = norm;
		this.molIndex = molIndex;
		graph = new DelegateTree<Integer, Integer>();
		iconMap = new HashMap<>();
		edgeMap = new HashMap<>();
		createTree();
		
		FRLayout<Integer,Integer> flayout = new FRLayout<Integer, Integer>(graph);
        flayout.setMaxIterations(100);
        flayout.setInitializer(new RandomLocationTransformer<Integer>(new Dimension(1200, 800), 0));
        flayout.setSize(new Dimension(1200, 800));
        vv = new VisualizationViewer<>(flayout, new Dimension(1200, 900));
		
//		radialLayout = new RadialTreeLayout<Integer,Integer>(graph, 250, 250);//, 900, 900);
//        radialLayout.setSize(new Dimension(1200, 900));
//        vv =  new VisualizationViewer<Integer,Integer>(radialLayout, new Dimension(1200, 900));
        
        vv.setBackground(Color.white);
        vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.QuadCurve<Integer, Integer>());
        vv.getRenderContext().setEdgeDrawPaintTransformer(new EdgeColor());
        vv.getRenderContext().setEdgeStrokeTransformer(new EdgeStrokeFunc());
        vv.getRenderContext().setArrowFillPaintTransformer(new EdgeColor());// ConstantTransformer(Color.lightGray));
        vv.getRenderContext().setArrowDrawPaintTransformer(new EdgeColor());//ConstantTransformer(Color.black));
        
//        VertexIconShapeTransformer<Integer> vertexIconShapeTransformer =
//                new VertexIconShapeTransformer<Integer>(new EllipseVertexShapeTransformer<Integer>());
//        vertexIconShapeTransformer.setIconMap(iconMap);
        vv.getRenderContext().setVertexShapeTransformer(new Transformer<Integer, Shape>() {
			@Override
			public Shape transform(Integer arg0) {
				// TODO Auto-generated method stub
				return new Rectangle(ICON_WIDTH - 5, ICON_HEIGHT - 5);
			}
        	
        });
        
        DefaultVertexIconTransformer<Integer> vertexIconTransformer =
            	new DefaultVertexIconTransformer<Integer>();
        vertexIconTransformer.setIconMap(iconMap);
        vv.getRenderContext().setVertexIconTransformer(vertexIconTransformer);
        
        
        DefaultModalGraphMouse<Integer, Integer> graphMouse = new DefaultModalGraphMouse<>();
        vv.setGraphMouse(graphMouse);
        vv.addKeyListener(graphMouse.getModeKeyListener());
        
        JComboBox modeBox = graphMouse.getModeComboBox();
        modeBox.addItemListener(graphMouse.getModeListener());
        graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
        
        JPanel controls = new JPanel();
        controls.add(modeBox);
        setLayout(new BorderLayout(0, 0));
        this.add(vv);
        this.add(controls, BorderLayout.SOUTH);
	}
	
	public void setMolIndex(int molIndex) throws Exception {	//modi
		edgeFactory = new Factory<Integer>() {
			int i=0;
			public Integer create() {
				return i++;
			}};
		this.molIndex = molIndex;
		this.graph = new DelegateTree<Integer, Integer>();
		this.iconMap = new HashMap<>();
		this.edgeMap = new HashMap<>();
		createTree();
		
//		this.radialLayout = new RadialTreeLayout<Integer,Integer>(graph, 250, 250);
//		radialLayout.setSize(new Dimension(1200, 900));
//		vv.setGraphLayout(radialLayout);
		
		FRLayout<Integer,Integer> flayout = new FRLayout<Integer, Integer>(graph);
        flayout.setMaxIterations(100);
        flayout.setInitializer(new RandomLocationTransformer<Integer>(new Dimension(1200, 800), 0));
        flayout.setSize(new Dimension(1200, 800));
        vv.setGraphLayout(flayout);
		
//	VertexIconShapeTransformer<Integer> vertexIconShapeTransformer =
//                new VertexIconShapeTransformer<Integer>(new EllipseVertexShapeTransformer<Integer>());
//        vertexIconShapeTransformer.setIconMap(iconMap);
//        vv.getRenderContext().setVertexShapeTransformer(vertexIconShapeTransformer);
		
		vv.getRenderContext().setVertexShapeTransformer(new Transformer<Integer, Shape>() {
			@Override
			public Shape transform(Integer arg0) {
				// TODO Auto-generated method stub
				return new Rectangle(-ICON_WIDTH/2, -ICON_HEIGHT/2, ICON_WIDTH, ICON_HEIGHT);
			}
        	
        });
		
		 DefaultVertexIconTransformer<Integer> vertexIconTransformer =
	            	new DefaultVertexIconTransformer<Integer>();
	        vertexIconTransformer.setIconMap(iconMap);
	        vv.getRenderContext().setVertexIconTransformer(vertexIconTransformer);
		
	   
		//this.add(vv);
		this.validate();
	}
	
	public VisualizationViewer<Integer, Integer> getVViewer() {
		return vv;
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
		protected final Stroke basic = new BasicStroke(2);
		protected final Stroke heavy = new BasicStroke(4);
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
		Molecule rootMol = heat.getMolArray()[molIndex];
		 
		StructureDisplay sd = new StructureDisplay(rootMol.getMol());
		String fieldStr = rootMol.getFieldName().substring(0, 10); // show only 10 first chars of field name
		iconMap.put(0, sd.getIcon(ICON_WIDTH, ICON_HEIGHT, null,
				rootMol.getPotency(), Color.black, fieldStr, rootMol.getMolID()));
		graph.setRoot(0);
		
		int childNum = 1;
		for (int i = 0; i < heat.getMCSArr()[molIndex].length; ++i) {
			if (heat.getMCSArr()[i][molIndex] != null) {
				SMSDpair pair = (SMSDpair) heat.getMCSArr()[i][molIndex];
				Molecule mol = heat.getMolArray()[i];
				sd = new StructureDisplay(mol.getMol());
				double dP = heat.getData()[i][molIndex];
				Color col;
				if (dP > 1) {
					col = Color.green;
				} else if (dP < -1) {
					col = Color.red;
				} else {
					col = Color.gray;
				}
				Collection<Integer> highL = i > molIndex ? pair.targetHi() : pair.queryHi();
				iconMap.put(childNum, sd.getIcon(ICON_WIDTH, ICON_HEIGHT,
						highL, mol.getPotency(), col, fieldStr, mol.getMolID()));
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
	        PairsTree pt = new PairsTree(heat, 0, 100);
	        pt.setMolIndex(molIndex);
	        content.add(pt);
	        frame.pack();
	        frame.setVisible(true);
			}
}
