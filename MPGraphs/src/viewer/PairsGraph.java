package viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.ToolTipManager;

import main.AdjMatrix;
import main.Gradient;
import main.Molecule;
import main.PairsModel;
import main.SDFreader;
import main.PairsModel.MolTransf;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;


import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.algorithms.matrix.GraphMatrixOperations;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

public class PairsGraph  extends JPanel {
	private Graph<Integer, Number> g;
	private VisualizationViewer<Integer, Number> vv;
	private SideDisplay disp;
	private PairsModel pm;
	private static final Color[] colors = Gradient.GRADIENT_RED_TO_GREEN;
	private static int W = 800;
	private static int H = 800;
	private HashMap<Integer, Double> dPMap;
	private double min;
	private double max;
	private ArrayList<Integer> arr;
	private JTextArea text;
	
	public PairsGraph(SideDisplay disp) {
		super();
		g = new UndirectedSparseGraph<Integer, Number>();
		this.disp = disp;
		Layout<Integer, Number> layout = new StaticLayout<Integer, Number>(g);
		vv = new VisualizationViewer<>(layout, new Dimension(W, H));
		vv.setBackground(Color.black);
		JPanel panel = new JPanel();
		vv.add(panel, BorderLayout.EAST);
		panel.setLayout(new BorderLayout(0, 0));		
		text = new JTextArea();
		text.setForeground(Color.WHITE);
		text.setBackground(Color.BLACK);
		//text.setText("Stats\n\nThis is stats");
		panel.add(text, BorderLayout.NORTH);
		this.add(vv);
	}
	
	@SuppressWarnings("unchecked")
	public void set(PairsModel pmod, int clustInd, int transfInd) {
		this.pm = pmod;
		g = new UndirectedSparseGraph<Integer, Number>();
		TreeMap<MolTransf, ArrayList<Integer>> map = pm.TransfClustMap(clustInd);
		arr = (ArrayList<Integer>) map.values().toArray()[transfInd];
		dPMap = new HashMap<>();
		for (int i = 0; i < arr.size(); ++i) {
			g.addVertex(i);
			int ind = arr.get(i);
			Double dP = pm.getdPArr().get(ind) * pm.getTrArr().get(ind).getDirection();
			dPMap.put(i, dP);
			System.out.println(dP);		
		}
		min = Collections.min(dPMap.values());
		max = Collections.max(dPMap.values());
		double sz = dPMap.values().size();
		double sum = 0;
		for (double v : dPMap.values()) {
			sum += v;
		}
		double avg = sum / sz;
		sum = 0;
		for (double v : dPMap.values()) {
			sum += (avg - v) * (avg - v);
		}
		double stdDev = sz > 1 ? Math.sqrt(sum / (sz - 1)) : 0.0;
		String title = map.keySet().iterator().next().toString() + "\n";
		String desc;
		desc = String.format("Average: %f%n" + "StdDev: %f%n", avg, stdDev);
		text.setText(title + desc + "\n");
		
		Layout<Integer, Number> layout = new StaticLayout<Integer, Number>(g, new Transformer<Integer,Point2D>() {
			public Point2D transform(Integer i) {
				Double yBound = Math.max(Math.abs(min), Math.abs(max));
				if (yBound == 0)
					yBound = 1.0; // Any non zero
				
				Double x = 40 + ((W - 50.0)/dPMap.size()) * (i );
				Double y = (H/2) - (H/2 - 60) *  dPMap.get(i) / yBound;
				System.out.println(x + ", " + y + " " + yBound);
				//System.out.println(dPMap.values());
				return new Point2D.Double(x, y);
			}
		});
		layout.setSize(new Dimension(W, H));
		vv.setGraphLayout(layout);
		ToolTipManager.sharedInstance().setReshowDelay(0);
		ToolTipManager.sharedInstance().setInitialDelay(0);
		vv.getRenderContext().setVertexShapeTransformer(
        		new ConstantTransformer(new Rectangle2D.Float(-6,-6,12,12)));
		vv.setVertexToolTipTransformer(new Transformer<Integer, String>() {			
			@Override
			public String transform(Integer v) {
				try {
					disp.clear();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				int index = arr.get(v);
				Molecule query;
				Molecule target;
				Collection<Integer> queryHi;
				Collection<Integer> targetHi;
				if (pm.getTrArr().get(index).getDirection() > 0) {
					query = pm.getqArr().get(index);
					target = pm.gettArr().get(index);
					queryHi = pm.getqHilArr().get(index);
					targetHi = pm.gettHilArr().get(index);
				} else {
					target = pm.getqArr().get(index);
					query = pm.gettArr().get(index);
					targetHi = pm.getqHilArr().get(index);
					queryHi = pm.gettHilArr().get(index);
				}
				
				try {
					disp.set(query, target, queryHi, targetHi);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return dPMap.get(v).toString();
			}
		});
		
		// Setup up a new vertex to paint transformer...
		 Transformer<Integer,Paint> vertexPaint = new Transformer<Integer,Paint>() {
		 public Paint transform(Integer i) {
			 double dP = dPMap.get(i);
			 if (dP < -1 / pm.getNorm()) {
				 return Color.RED;
			 } else if (dP > 1 / pm.getNorm()) {
				 return Color.GREEN;
			 } else {
				 return Color.GRAY;
			 }
		 }
		 };
		 vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
		 vv.addPreRenderPaintable(new VisualizationViewer.Paintable(){
             public void paint(Graphics g) {
            	 g.setColor(Color.WHITE);
            	 g.drawLine(20, H/2, W-20, H/2);
            	 g.drawLine(20, 20, 20, H-20);
             }

			@Override
			public boolean useTransform() {
				// TODO Auto-generated method stub
				return false;
			}});
		 
             
		 this.validate();
	}
	
}
