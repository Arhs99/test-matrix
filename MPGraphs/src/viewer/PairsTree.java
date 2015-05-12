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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.ToolTipManager;

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
	private VisualizationViewer<Integer, Integer> vv1; 	//labels
	private VisualizationViewer<Integer, Integer> vv2;	//with vertices
	private static final int ICON_WIDTH = 180;
	private static final int ICON_HEIGHT = 180;
	private Map<Integer,Icon> iconMap;
	private Map<Integer, Double> edgeMap;
	private Map<Integer, Molecule> vertexMap;
	private HeatMap heat;
	private int molIndex;
	private double norm;
	private boolean hasLabels;
	private DefaultModalGraphMouse<Integer, Integer> graphMouse;
	private JPanel controls;
	private final Color[] colors = Gradient.GRADIENT_RED_TO_GREEN;
	private List<Double> potencies;
//	private JPanel sidePanel;
	private String fieldStr = "";
	private boolean onVertex = false;

	private StructureDisplay tdp1;

	private ImageToolTip molToolTip;
	
	private void initVV() {
		// with labels
		FRLayout<Integer,Integer> flayout = new FRLayout<Integer, Integer>(graph);
        flayout.setMaxIterations(100);
        flayout.setInitializer(new RandomLocationTransformer<Integer>(new Dimension(1200, 800), 0));
        flayout.setSize(new Dimension(1200, 800));
        vv1 = new VisualizationViewer<>(flayout, new Dimension(1200, 900));
        vv1.setBackground(Color.white);
        vv1.getRenderContext().setEdgeShapeTransformer(new EdgeShape.QuadCurve<Integer, Integer>());
        vv1.getRenderContext().setEdgeDrawPaintTransformer(new EdgeColor());
        vv1.getRenderContext().setEdgeStrokeTransformer(new EdgeStrokeFunc());
        vv1.getRenderContext().setArrowFillPaintTransformer(new EdgeColor());// ConstantTransformer(Color.lightGray));
        vv1.getRenderContext().setArrowDrawPaintTransformer(new EdgeColor());//ConstantTransformer(Color.black));
        vv1.getRenderContext().setVertexShapeTransformer(new Transformer<Integer, Shape>() {
			@Override
			public Shape transform(Integer arg0) {
				return new Rectangle(-ICON_WIDTH / 2, - ICON_HEIGHT / 2, ICON_WIDTH, ICON_HEIGHT);
			}        	
        });
        
        DefaultVertexIconTransformer<Integer> vertexIconTransformer =
            	new DefaultVertexIconTransformer<Integer>();
        vertexIconTransformer.setIconMap(iconMap);
        vv1.getRenderContext().setVertexIconTransformer(vertexIconTransformer);
        
        // vertices layout
        RadialTreeLayout<Integer,Integer> radialLayout = new RadialTreeLayout<>(graph, 250, 250);//, 900, 900);
		radialLayout.setSize(new Dimension(1200, 900));
		vv2 = new VisualizationViewer<Integer,Integer>(radialLayout, new Dimension(1200, 900)) {
			public JToolTip createToolTip() {
				if (onVertex) {
					return molToolTip;
				} else {
					return new JToolTip();
				}
			}
		};
		vv2.setBackground(Color.white);
	    //vv2.getRenderContext().setEdgeShapeTransformer(new EdgeShape.QuadCurve<Integer, Integer>());
	    vv2.getRenderContext().setEdgeDrawPaintTransformer(new ConstantTransformer(Color.lightGray));
	    //vv2.getRenderContext().setEdgeStrokeTransformer(new EdgeStrokeFunc());
	    vv2.getRenderContext().setArrowFillPaintTransformer(new ConstantTransformer(Color.lightGray));
	    vv2.getRenderContext().setArrowDrawPaintTransformer(new ConstantTransformer(Color.lightGray));
	    vv2.getRenderContext().setVertexFillPaintTransformer(new Transformer<Integer,Paint>() {
			 public Paint transform(Integer i) {
				 double pot = Math.min(norm, potencies.get(i));
				 double normal = 1.0 - pot / norm; // Assuming max=100 and min=0
				 int colorIndex = (int) Math.floor(normal * (colors.length - 1));
				 return colors[colorIndex];
			 }
			 });
//	    vv2.add(sidePanel, BorderLayout.EAST);
        vv1.setGraphMouse(graphMouse);
        vv1.addKeyListener(graphMouse.getModeKeyListener());
        vv2.setGraphMouse(graphMouse);
        vv2.addKeyListener(graphMouse.getModeKeyListener());
        vv2.setVertexToolTipTransformer(new Transformer<Integer, String>() {			
			@Override
			public String transform(Integer v) {
				Molecule mol = vertexMap.get(v);
				onVertex = true;
				//sidePanel.removeAll();
				try {
					tdp1.setMol(mol.getMol());
					StructureDisplay sd = new StructureDisplay(mol.getMol());
//					Icon icon = sd.getFlatIcon(ICON_WIDTH, ICON_HEIGHT,
//							null, mol.getPotency(), Color.BLACK, fieldStr,
//							mol.getMolID());
//					JLabel label = new JLabel(icon);
//					sidePanel.add(label);
//					vv2.validate();
//					vv2.repaint();					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return mol.getMolID();
			}
		});
	}
	
	private class PTGraphMouse<V,E> extends DefaultModalGraphMouse<V,E> {
		public void mouseClicked(MouseEvent e) {
			PairsTree.this.firePropertyChange("PTreeState", true, false);
		}
	}
	
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
		vertexMap = new HashMap<>();
		graphMouse = new PTGraphMouse<>(); //DefaultModalGraphMouse<>();
		controls = new JPanel();
//		sidePanel = new JPanel();
//		sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
		tdp1 = new StructureDisplay();
		molToolTip = new ImageToolTip(tdp1);
		createTree();
		initVV();
		ToolTipManager.sharedInstance().setReshowDelay(0);
		ToolTipManager.sharedInstance().setInitialDelay(0);
		vv = hasLabels? vv1 : vv2;
        
		JComboBox modeBox = graphMouse.getModeComboBox();
        modeBox.addItemListener(graphMouse.getModeListener());
        graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
        
        String[] combos = new String[]{"Labels", "Vertices"};
        final JComboBox jcb = new JComboBox(combos);
        jcb.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("rawtypes")
				JComboBox cb = (JComboBox)e.getSource();
				int ind = cb.getSelectedIndex();
				hasLabels = (ind == 0);
				//System.out.println(ind + " " + hasLabels + " " + e.toString());
				setGraphLayout();
				//PairsTree.this.validate();
			}
		});
                
        controls.add(modeBox);
        controls.add(jcb);
        setLayout(new BorderLayout(0, 0));
        this.add(vv);
        this.add(controls, BorderLayout.SOUTH);
	}
	
	private void setGraphLayout() {
		this.removeAll();
		vv = hasLabels? vv1 : vv2;
		this.add(vv);
        this.add(controls, BorderLayout.SOUTH);
		this.validate();
		this.repaint();
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
		initVV();
		
		this.removeAll();
		vv = hasLabels? vv1 : vv2;
		this.add(vv);
        this.add(controls, BorderLayout.SOUTH);
		this.validate();
		this.repaint();
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
		potencies = new ArrayList<>(); 
		StructureDisplay sd = new StructureDisplay(rootMol.getMol());
		fieldStr = rootMol.getFieldName();
		if (fieldStr.length() > 10)
			fieldStr = rootMol.getFieldName().substring(0, 10); // show only 10 first chars of field name
		iconMap.put(0, sd.getIcon(ICON_WIDTH, ICON_HEIGHT, null,
				rootMol.getPotency(), Color.black, fieldStr, rootMol.getMolID()));
		graph.setRoot(0);
		vertexMap.put(0, rootMol);
		potencies.add(rootMol.getPotency());
		
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
				vertexMap.put(childNum, mol);
				potencies.add(mol.getPotency());
				edgeMap.put(childNum - 1, dP);		// edge numbering has to start from 0
				++childNum;
			}
			hasLabels = childNum < 20;
		}
	}
	
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
				if (cnt == 19) break;
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
