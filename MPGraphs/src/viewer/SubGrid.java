package viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputAdapter;
import javax.vecmath.Point2d;

import main.AdjMatrix;
import main.DeltaP;
import main.Molecule;
import main.SDFreader;
import main.SMSDpair;

import org.apache.commons.collections15.Factory;
import org.openscience.cdk.geometry.GeometryUtil;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;

public class SubGrid extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	Factory<Integer> edgeFactory;
	private static final int ICON_WIDTH = 250;
	private static final int ICON_HEIGHT = 250;
	private AdjMatrix adm;
	private int molIndex;
	private double norm = 100.0;
	private static final Color HIL = Color.MAGENTA;
	private SelectionIcon rootIcon = null;
	private int atomIndex;
	private Set<Molecule> green;
	private Set<Molecule> red;
	private JPanel rootPanel;
	private JPanel side;
	private JPanel greenPanel;
	private JPanel redPanel;
	private Molecule rootMol;
	private String[] comboIDs;
	
	public SubGrid(AdjMatrix adm) throws Exception {
		super();
		setBorder(new EmptyBorder(5, 5, 5, 5));
		setLayout(new BorderLayout(0, 0));
		setPreferredSize(new Dimension(1200, 800));
		setSize(new Dimension(1200, 800));
		
		rootPanel = new JPanel();
		rootPanel.setLayout(new BorderLayout(0, 0));
		rootPanel.setSize(new Dimension(ICON_WIDTH + 40, ICON_HEIGHT + 60));
		add(rootPanel, BorderLayout.CENTER);
		
		side = new JPanel();
		add(side, BorderLayout.EAST);
		side.setLayout(new GridLayout(0, 2, 10, 10));
		
		greenPanel = new JPanel();
		greenPanel.setLayout(new BoxLayout(greenPanel, BoxLayout.Y_AXIS));
		JScrollPane scrollPane = new JScrollPane(greenPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(ICON_WIDTH + 30, 800));
		side.add(scrollPane);
		//side.add(greenPanel);
		
		redPanel = new JPanel();
		redPanel.setLayout(new BoxLayout(redPanel, BoxLayout.Y_AXIS));
		scrollPane = new JScrollPane(redPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(ICON_WIDTH + 30, 800));
		side.add(scrollPane);
				
		this.adm = adm;
		
		comboIDs = new String[adm.getMolArray().length];
		int j = 0;
		for (Molecule mol : adm.getMolArray()) {
			comboIDs[j++] = mol.getMolID();
		}

		MouseListener listener = new MouseListener();
		rootPanel.addMouseListener(listener);
		rootPanel.addMouseMotionListener(listener);
		this.validate();
	}
	
	public void setIndex(int molIndex, int atomIndex) throws Exception {
		this.molIndex = molIndex;
		rootMol = adm.getMolArray()[molIndex];	
		rootPanel.removeAll();
		String fieldStr = rootMol.getFieldName().length() > 10?  // show only 10 first chars of field name
				rootMol.getFieldName().substring(0, 10) : rootMol.getFieldName(); 
		rootIcon = new SelectionIcon(ICON_WIDTH + 40, ICON_HEIGHT + 40, rootMol.getMol(),
				rootMol.getPotency(), fieldStr, rootMol.getMolID(), rootMol.getAtomMapping().keySet());
		JLabel rootLabel = new JLabel(rootIcon);
		rootPanel.add(rootLabel);
		JComboBox molIDs = new JComboBox();
		molIDs.setEditable(true);
		molIDs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
				int ind = cb.getSelectedIndex();
				if (ind != -1) {
					try {
						setIndex(ind, 1);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		molIDs.setModel(new DefaultComboBoxModel(comboIDs));
		molIDs.setEditable(true);
		rootPanel.add(molIDs, BorderLayout.SOUTH);
		setAtomIndex(atomIndex);
	}
	public void setAtomIndex(int atomIndex) throws Exception {
		this.atomIndex = atomIndex;
		side.removeAll();
		//redPanel.removeAll();
		//greenPanel.removeAll();
		red = new TreeSet<>();
		green = new TreeSet<>(new Comparator<Molecule>() {
			@Override
			public int compare(Molecule o1, Molecule o2) {
				return - o1.compareTo(o2);
			}			
		});
		greenPanel = new JPanel();
		greenPanel.setLayout(new BoxLayout(greenPanel, BoxLayout.Y_AXIS));
		//greenPanel.setPreferredSize(new Dimension(ICON_WIDTH + 30, 2000));
		redPanel = new JPanel();
		redPanel.setLayout(new BoxLayout(redPanel, BoxLayout.Y_AXIS));
		//redPanel.setPreferredSize(new Dimension(ICON_WIDTH + 30, 2000));
		createTree();		
		JScrollPane scrollPane = new JScrollPane(greenPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(ICON_WIDTH + 15, 800));
		side.add(scrollPane);
		scrollPane = new JScrollPane(redPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(ICON_WIDTH + 15, 800));
		side.add(scrollPane);
		this.validate();
		this.repaint();
	}
	
	private void createTree() throws Exception {		
		Molecule rootMol = adm.getMolArray()[molIndex];		 
		String fieldStr = rootMol.getFieldName().length() > 10?  // show only 10 first chars of field name
				rootMol.getFieldName().substring(0, 10) : rootMol.getFieldName(); 
		Set<Integer> set = rootMol.getAtomMapping().get(atomIndex);
		if (set == null) return;
		for (int i : set) {					
					Molecule mol = adm.getMolArray()[i];
					if (mol.getPotency().compareTo(rootMol.getPotency()) <= 0) {
						green.add(mol);
					} else {
						red.add(mol);
					}
				}
		for (Molecule mol : green) {
			StructureDisplay sd = new StructureDisplay(mol.getMol());
			double dP = (DeltaP.logDiff(rootMol.getPotency(), mol.getPotency(), norm) == Double.MIN_VALUE ?
					0 : DeltaP.logDiff(mol.getPotency(), rootMol.getPotency(), norm));
			Color col;
			if (dP > 1) {
				col = Color.red;				
			} else if (dP < -1) {
				col = Color.green;
			} else {
				col = Color.gray;
			}
			SMSDpair pair = (SMSDpair) adm.getMCSMatrix().get(
					molIndex, mol.getIndex());
			Collection<Integer> highL = mol.getIndex() > molIndex ? pair.targetHi() : pair.queryHi();
			JLabel label = new JLabel(sd.getFlatIcon(ICON_WIDTH, ICON_HEIGHT,
					highL, mol.getPotency(), col, fieldStr, mol.getMolID()));
			final int iconIndex = mol.getIndex();
			label.addMouseListener(new java.awt.event.MouseListener() {
				
				@Override
				public void mouseReleased(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void mousePressed(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void mouseExited(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void mouseEntered(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void mouseClicked(MouseEvent e) {
					try {
						setIndex(iconIndex, 1);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}					
				}
			});
			greenPanel.add(label);	
		}
		
		//Scrollbar scrollbar = new Scrollbar();
		//greenPanel.add(scrollbar);
		
		for (Molecule mol : red) {
			StructureDisplay sd = new StructureDisplay(mol.getMol());
			double dP = (DeltaP.logDiff(rootMol.getPotency(), mol.getPotency(), norm) == Double.MIN_VALUE ?
					0 : DeltaP.logDiff(mol.getPotency(), rootMol.getPotency(), norm));
			Color col;
			if (dP > 1) {
				col = Color.red;				
			} else if (dP < -1) {
				col = Color.green;
			} else {
				col = Color.gray;
			}
			SMSDpair pair = (SMSDpair) adm.getMCSMatrix().get(
					molIndex, mol.getIndex());
			Collection<Integer> highL = mol.getIndex() > molIndex ? pair.targetHi() : pair.queryHi();
			JLabel label = new JLabel(sd.getFlatIcon(ICON_WIDTH, ICON_HEIGHT,
					highL, mol.getPotency(), col, fieldStr, mol.getMolID()));
			final int iconIndex = mol.getIndex();
			label.addMouseListener(new java.awt.event.MouseListener() {
				
				@Override
				public void mouseReleased(MouseEvent e) {					
				}
				
				@Override
				public void mousePressed(MouseEvent e) {					
				}
				
				@Override
				public void mouseExited(MouseEvent e) {					
				}
				
				@Override
				public void mouseEntered(MouseEvent e) {					
				}
				
				@Override
				public void mouseClicked(MouseEvent e) {
					try {
						setIndex(iconIndex, 1);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}					
				}
			});
			redPanel.add(label);
		}
	}
	
	public class MouseListener extends MouseInputAdapter {
		private IAtom atomSel = null;
		private IBond bondSel = null;
		public void mouseClicked(MouseEvent e) {
//			if (rootIcon == null) return;
//			Point p = e.getPoint();
//			Point2d modelPoint = rootIcon.getRenderer().toModelCoordinates(p.getX(), p.getY());
//			IAtom atom = GeometryUtil.getClosestAtom(modelPoint.getX(), modelPoint.getY(), rootIcon.getMol());
//
//			try {
//				setIndex(molIndex + 1, 1);
//			} catch (Exception e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
		}
		
		
		public void mouseMoved(MouseEvent e) {
			if (rootIcon == null) return;
			Point p = e.getPoint();
			Point2d modelPoint = rootIcon.getRenderer().toModelCoordinates(p.getX(), p.getY());
			//Set<Integer> set = rootMol.getAtomMapping().keySet();
			IAtom atom = GeometryUtil.getClosestAtom(modelPoint.getX(), modelPoint.getY(), rootIcon.getMol());
			IBond bond = GeometryUtil.getClosestBond(modelPoint.getX(), modelPoint.getY(), rootIcon.getMol());
			double da = atom.getPoint2d().distanceSquared(modelPoint);
			double db = bond.get2DCenter().distanceSquared(modelPoint);
			if (da > 0.5 && db > 0.5) {
				if (atomSel == null && bondSel ==  null) {
					return;
				}
				if (atomSel != null) {
					atomSel.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
					atomSel = null;
				}
				if (bondSel != null) {
					bondSel.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
					bondSel = null;
				}
			} else if (da < db) {
				if (bondSel != null) {
					bondSel.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
					bondSel = null;
				}
				
				if (atom.equals(atomSel)) {
					return;
				} else {
					if (atomSel != null) {
						atomSel.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
					}
					atomSel = atom;
					atomSel.setProperty(StandardGenerator.HIGHLIGHT_COLOR, HIL);
					try {
						setAtomIndex(rootIcon.getMol().getAtomNumber(atomSel));
						//System.out.println(rootIcon.getMol().getAtomNumber(atomSel));
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			} else {
				if (atomSel != null) {
					atomSel.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
					atomSel = null;
				}
				if (bond.equals(bondSel)) {
					return;
				} else {
					if (bondSel != null) {
						bondSel.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
					}
					bondSel = bond;
					bondSel.setProperty(StandardGenerator.HIGHLIGHT_COLOR, HIL);
				}
 			}
			repaint();
		}
	}
	public JPanel returnPanel() {
		return rootPanel;
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
		SubGrid tst = new SubGrid(adm);
		tst.setIndex(2, 1);
		JFrame frame = new JFrame();
        Container content = frame.getContentPane();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        content.add(tst);
        frame.pack();
        frame.setVisible(true);
		}

	}
