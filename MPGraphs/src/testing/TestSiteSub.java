package testing;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import main.DeltaP;
import main.MolClustPair;
import main.Molecule;
import main.PairsModel;
import main.SMSDpair;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;

import viewer.StructureDisplay;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.util.RandomLocationTransformer;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.DefaultVertexIconTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;

public class TestSiteSub extends JPanel {
	Factory<Integer> edgeFactory;
	
	
	private DelegateTree<Integer, Integer> graph;
	private VisualizationViewer<Integer, Integer> vv;
	private static final int ICON_WIDTH = 180;
	private static final int ICON_HEIGHT = 180;
	private Map<Integer,Icon> iconMap;
	private Map<Integer, Double> edgeMap;
	private PairsModel pm;
	private int molIndex;
	private double norm;


	private int clust;
	
	public TestSiteSub(PairsModel pm) throws Exception {
		edgeFactory = new Factory<Integer>() {
			int i=0;
			public Integer create() {
				return i++;
			}};
		this.pm = pm;
		graph = new DelegateTree<Integer, Integer>();
		iconMap = new HashMap<>();
		edgeMap = new HashMap<>();
		createTree();
		
		FRLayout<Integer,Integer> flayout = new FRLayout<Integer, Integer>(graph);
        flayout.setMaxIterations(100);
        flayout.setInitializer(new RandomLocationTransformer<Integer>(new Dimension(1200, 800), 0));
        flayout.setSize(new Dimension(1200, 800));
        vv = new VisualizationViewer<>(flayout, new Dimension(1200, 900));
		
        
        vv.setBackground(Color.white);
        vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.QuadCurve<Integer, Integer>());
        vv.getRenderContext().setEdgeDrawPaintTransformer(new EdgeColor());
        vv.getRenderContext().setEdgeStrokeTransformer(new EdgeStrokeFunc());
        vv.getRenderContext().setArrowFillPaintTransformer(new EdgeColor());// ConstantTransformer(Color.lightGray));
        vv.getRenderContext().setArrowDrawPaintTransformer(new EdgeColor());//ConstantTransformer(Color.black));
        
        vv.getRenderContext().setVertexShapeTransformer(new Transformer<Integer, Shape>() {
			@Override
			public Shape transform(Integer arg0) {
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
	
	public void setMolIndex(int molIndex, int clust) throws Exception {	//modi
		edgeFactory = new Factory<Integer>() {
			int i=0;
			public Integer create() {
				return i++;
			}};
		this.molIndex = molIndex;
		this.clust = clust;
		this.graph = new DelegateTree<Integer, Integer>();
		this.iconMap = new HashMap<>();
		this.edgeMap = new HashMap<>();
		createTree();
	
		
		FRLayout<Integer,Integer> flayout = new FRLayout<Integer, Integer>(graph);
        flayout.setMaxIterations(100);
        flayout.setInitializer(new RandomLocationTransformer<Integer>(new Dimension(1200, 800), 0));
        flayout.setSize(new Dimension(1200, 800));
        vv.setGraphLayout(flayout);
		
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
		Molecule rootMol = pm.getAdm().molVector()[clust][molIndex];		 
		StructureDisplay sd = new StructureDisplay(rootMol.getMol());
		String fieldStr = rootMol.getFieldName().substring(0, 10); // show only 10 first chars of field name
		iconMap.put(0, sd.getIcon(ICON_WIDTH, ICON_HEIGHT, null,
				rootMol.getPotency(), Color.black, fieldStr, rootMol.getMolID()));
		graph.setRoot(0);
		
		int childNum = 1;
		Map<Integer, Set<MolClustPair>> map = rootMol.getAtomMapping();
		//if (map.isEmpty()) return;
		for (int i : map.keySet()) {
				Set<MolClustPair> clustMap = map.get(i);
				for (MolClustPair mcPair : clustMap) {
					SMSDpair pair = (SMSDpair) pm.getAdm().getCCSMSDMatr()[mcPair.clust()].get(
							molIndex, mcPair.atom());
					Molecule mol = pm.getAdm().molVector()[mcPair.clust()][mcPair.atom()];
					sd = new StructureDisplay(mol.getMol());
					double dP = (DeltaP.logDiff(rootMol.getPotency(), mol.getPotency(), norm) == Double.MIN_VALUE ?
							0 : DeltaP.logDiff(mol.getPotency(), rootMol.getPotency(), norm));
					Color col;
					if (dP > 1) {
						col = Color.green;
					} else if (dP < -1) {
						col = Color.red;
					} else {
						col = Color.gray;
					}
					Collection<Integer> highL = mcPair.atom() > molIndex ? pair.targetHi() : pair.queryHi();
					iconMap.put(childNum, sd.getIcon(ICON_WIDTH, ICON_HEIGHT,
							highL, mol.getPotency(), col, fieldStr, mol.getMolID()));
					graph.addEdge(edgeFactory.create(), 0, childNum);	
					edgeMap.put(childNum - 1, dP);		// edge numbering has to start from 0
					++childNum;
				}
				
			}
		}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
