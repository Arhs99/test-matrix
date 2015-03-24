package main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.event.MouseInputAdapter;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;

import viewer.HeatMap;
import viewer.PairsTree;
import viewer.SDFDialogue;
import viewer.SideDisplay;

public class Main extends JPanel {
	private AdjMatrix adm;
	private SideDisplay disp;
	private HeatMap heat;
	private Task task;
	private JProgressBar progressBar;
	private JPanel extra;
	private PairsTree ptree;
	private double norm = 100.0;
	private int fieldInd;
	private int idInd;
	
	class Task extends SwingWorker<AdjMatrix, Void> {
		private SDFreader sdf;

		public Task(SDFreader sdf) {
			this.sdf = sdf;
		}
		@Override
		public AdjMatrix doInBackground() throws Exception {
			Set<Molecule> set = new TreeSet<>();
			int cnt = 0;
			String s = sdf.fieldStr()[fieldInd];
			for (IAtomContainer mol : sdf.sdfMap().keySet()) {
				
				String val = sdf.sdfMap().get(mol)[fieldInd];
				if (val == null || mol.getAtomCount() == 0) {
					continue;
				}
				
//				boolean isUNSET = false;
//				for (IBond bond : mol.bonds()) {
//					if (bond.getOrder() == IBond.Order.UNSET) {
//						isUNSET = true;
//						break;
//					}
//				}
//				
//				if (isUNSET) break;
				
//				ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
//				mol = ExtAtomContainerManipulator.removeHydrogens(mol);
//				ExtAtomContainerManipulator.aromatizeCDK(mol);

				Molecule molec = new Molecule(mol, Double.parseDouble(val), s);
				if (idInd == 0) {
					molec.setMolID("ID " + Integer.toString(cnt + 1));
				} else {
					String id = sdf.sdfMap().get(mol)[idInd - 1];
					molec.setMolID(id);
				}				
				set.add(molec);
				++cnt;
				if (cnt == 20) break;
			}
			return new AdjMatrix(set, progressBar);
		}
		
		public void done() {
			try {
				adm = get();
				PairsModel pm;
				pm = new PairsModel(adm, norm);	
				disp = new SideDisplay();
				heat = new HeatMap(adm, true, disp, Gradient.GRADIENT_RED_TO_GREEN);
				int CCcount = adm.getCCDoubleMatr().length;
				final String comboDesc[] = new String[CCcount + 1];
				comboDesc[0] = "Show all";
				for (int i = 1; i < CCcount + 1 && adm.molVector()[i-1].length > 1; ++i) {
					comboDesc[i] = "Cluster " + i + " : " + adm.molVector()[i-1].length +
							" compounds";
							}
				heat.setToolTipText("");
				ptree = new PairsTree(heat, 0, norm);
				initListeners();
				
				extra.remove(progressBar);
				extra.add(heat);
				JComboBox comboClustBox = new JComboBox();
				comboClustBox.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JComboBox cb = (JComboBox)e.getSource();
						int ind = cb.getSelectedIndex();
						heat.updateMap(ind);
						Main.this.validate();
					}
				});
				comboClustBox.setAlignmentY(Component.TOP_ALIGNMENT);
				disp.add(comboClustBox);
				comboClustBox.setModel(new DefaultComboBoxModel(comboDesc));
				
				JComboBox comboTransfBox = new JComboBox();
				comboTransfBox.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
//						JComboBox cb = (JComboBox)e.getSource();
//						int ind = cb.getSelectedIndex();
//						heat.updateMap(ind);
//						Main.this.validate();
					}
				});
				comboTransfBox.setAlignmentY(Component.TOP_ALIGNMENT);
				disp.add(comboTransfBox);
				comboTransfBox.setModel(new DefaultComboBoxModel(pm.comboTransf()));
				
				Main.this.add(disp, BorderLayout.EAST);				
				Main.this.validate();
				Main.this.repaint();
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	private void initListeners() {
		ptree.getVViewer().addMouseListener(new MouseInputAdapter() {
			public void mouseClicked(MouseEvent e) {
				//System.out.println(e.getSource() + " " + ptree.getVViewer().hashCode());
				extra.remove(ptree);
				extra.add(heat);
				disp.setVisible(true);
				extra.validate();
			}
		});
		
		heat.addMouseListener(new MouseInputAdapter() {
			private int dataX(Point p) {
				Dimension d = heat.getSize();
				int w = d.width - 61;	// remove borders
				double scaleX = w * 1.0 / (heat.getData().length + 1.0);
				return (int) Math.floor((p.getX() - 31.0) / scaleX);
			}
			
			private int dataY(Point p) {
				Dimension d = heat.getSize();
				int h = d.height - 61;
				double scaleY = h * 1.0 / (heat.getData().length + 1.0);
				return (int) Math.floor((p.getY() - 31.0) / scaleY);
			}
			
			public void mouseClicked(MouseEvent e) {
				Point p = e.getPoint();
				int mouseY = dataY(p);
				if (mouseY > 0 && mouseY <= heat.getData().length) {
					try {
						ptree.setMolIndex(mouseY - 1);
						extra.remove(heat);
						disp.setVisible(false);
						extra.add(ptree);
						Main.this.validate();
						
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		
	}
	
	public Main(SDFreader sdf, int fieldInd, int idInd, double norm) throws Exception {
		super(new BorderLayout());
		this.setPreferredSize(new Dimension(1000, 1000));
		this.norm = norm;
		this.fieldInd = fieldInd;
		this.idInd = idInd;
		extra = new JPanel();
		extra.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		extra.setSize(new Dimension(1000, 1000));
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
        progressBar.setStringPainted(true);
        extra.add(progressBar);
        this.add(extra);
        //this.repaint();
		Task task = new Task(sdf);
		task.execute();		
		}
	
	public static void showGUI() {
		final JFrame f = new JFrame("Heatmap");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().setPreferredSize(new Dimension(1300, 1000));
		
		JMenuBar menuBar = new JMenuBar();
		f.setJMenuBar(menuBar);
		
		final JFileChooser fc = new JFileChooser();
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmImportSdf = new JMenuItem("Import sdf");
		mntmImportSdf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showOpenDialog(f);

		        if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File file = fc.getSelectedFile();
		            try {
		            	SDFreader sdf = new SDFreader(file);
		            	String fieldStr[] = sdf.fieldStr();
		            	SDFDialogue fieldDialog = new SDFDialogue(fieldStr);
		            	fieldDialog.showDial();
		            	
		            	if (fieldDialog.getFieldInd() != -1) {
							Main main = new Main(sdf, fieldDialog.getFieldInd(), fieldDialog.getIdInd(),
									fieldDialog.getNorm());
							f.getContentPane().removeAll();
							f.getContentPane().add(main);
							f.validate();
		            	}
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
		            System.out.println("Opening: " + file.getName() + ".");
		            fc.setSelectedFile(null);
		        } else {
		        	System.out.println("Open command cancelled by user.");
		        }
			}
		});
		mnFile.add(mntmImportSdf);
		
		JMenuItem mntmExportSdf = new JMenuItem("Export sdf");
		mntmExportSdf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showSaveDialog(f);
				 if (returnVal == JFileChooser.APPROVE_OPTION) {
			            File file = fc.getSelectedFile();
			            //This is where a real application would open the file.
			            System.out.println("Saving: " + file.getName() + ".");
			        } else {
			        	System.out.println("Save command cancelled by user.");
			        }
				}
		});
		mnFile.add(mntmExportSdf);
		f.pack();
		f.setVisible(true);
	}
	
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				showGUI();
			}
	});
}

}
