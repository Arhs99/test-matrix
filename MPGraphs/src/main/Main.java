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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.MouseInputAdapter;

import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;
import viewer.GraphView;
import viewer.HeatMap;
import viewer.PairsGraph;
import viewer.PairsTree;
import viewer.SDFDialogue;
import viewer.SideDisplay;
import viewer.SubGrid;

public class Main extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private AdjMatrix adm;
	private SideDisplay disp;
	private HeatMap heat = null;
	private Task task;
	private JProgressBar progressBar;
	private JPanel extra;
	private PairsTree ptree;
	private double norm = 100.0;
	private int fieldInd;
	private int idInd;
	private JComboBox comboClustBox;
	private JComboBox comboTransfBox;
	private int clustInd = 0;
	private JLabel lblMolecules;
	private Component rigidArea_1;
	private JPanel progPanel;
	private SubGrid sub;
	private JMenuBar menuBar;
	private GraphView graphView = null;
	
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
			if (s == null) s = "";
			for (IAtomContainer mol : sdf.sdfMap().keySet()) {
				
				IAtomContainer cleanedMol = ExtAtomContainerManipulator.checkAndCleanMolecule(mol);
				
				//IAtomContainer cleanedMol = MoleculeSanityCheck.checkAndCleanMolecule(mol);
				if (!ConnectivityChecker.isConnected(cleanedMol)) {
					continue;
					}
				
				String val = sdf.sdfMap().get(mol)[fieldInd];
				if (val == null || mol.getAtomCount() == 0) {
					continue;
				}
				
				ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(cleanedMol);
				cleanedMol = ExtAtomContainerManipulator.removeHydrogens(cleanedMol);
				ExtAtomContainerManipulator.aromatizeCDK(cleanedMol);

				Molecule molec = new Molecule(cleanedMol, Double.parseDouble(val), s);
				if (idInd == 0) {
					molec.setMolID("ID " + Integer.toString(cnt + 1));
				} else {
					String id = sdf.sdfMap().get(mol)[idInd - 1];
					molec.setMolID(id);
				}				
				set.add(molec);
				++cnt;
				//if (cnt == 120) break;
			}
			return new AdjMatrix(set, progressBar, fieldInd, idInd, norm);
		}
		
		public void done() {
			try {
				adm = get();

				 try
			      {
			         FileOutputStream fileOut =
			         new FileOutputStream("test.ser");
			         ObjectOutputStream out = new ObjectOutputStream(fileOut);
			         out.writeObject(adm);
			         out.close();
			         fileOut.close();
			         System.out.printf("Serialized data is saved in test.ser");
			      }catch(IOException i)
			      {
			          i.printStackTrace();
			          System.err.printf("Test.ser failed");
			      }
				initMain();
//							
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	private void initMain() throws Exception {
		final PairsModel pm;
		sub = new SubGrid(adm);				
		pm = new PairsModel(adm, norm);
		String[] comboTransf = pm.comboTransf();
		disp = new SideDisplay();
		heat = new HeatMap(adm, true, disp, Gradient.GRADIENT_RED_TO_GREEN);
		graphView = new GraphView(heat, 100);
		final PairsGraph pg = new PairsGraph(disp);
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
		
		extra.remove(progPanel);
		//extra.removeAll();
		extra.add(heat);
		extra.validate();
		
		class comboListener implements ActionListener {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
				if (e.getSource() == comboClustBox) {
					int ind = cb.getSelectedIndex();
					extra.removeAll();
					clustInd = ind;
					heat.updateMap(ind);
					extra.add(heat);
					comboTransfBox.setModel(new DefaultComboBoxModel
							(pm.comboTransfClust(ind)));
					extra.repaint();
					extra.validate();
				}
				
				if (e.getSource() == comboTransfBox) {
					int ind = cb.getSelectedIndex();
					pg.set(pm, clustInd, ind);
					extra.removeAll();
					extra.add(pg);
					//extra.setVisible(true);
					extra.repaint();
					extra.validate();
					//Main.this.validate();
				}							
			}
		}
		
		comboListener comboListener = new comboListener();
		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		disp.add(panel_1, BorderLayout.SOUTH);
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.PAGE_AXIS));
		
		
		JLabel lblNewLabel = new JLabel("Clusters");
		lblNewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		panel_1.add(lblNewLabel);
		
		comboClustBox = new JComboBox();
		lblNewLabel.setLabelFor(comboClustBox);
		comboClustBox.addActionListener(comboListener);
		comboClustBox.setAlignmentY(Component.TOP_ALIGNMENT);
		panel_1.add(comboClustBox);
		comboClustBox.setModel(new DefaultComboBoxModel(comboDesc));
		
		Component rigidArea = Box.createRigidArea(new Dimension(10, 10));
		panel_1.add(rigidArea);
		
		JLabel lblNewLabel_1 = new JLabel("Transformations");
		lblNewLabel_1.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel_1.add(lblNewLabel_1);
		comboTransfBox = new JComboBox();
		comboTransfBox.addActionListener(comboListener);
		panel_1.add(comboTransfBox);
		comboTransfBox.setModel(new DefaultComboBoxModel(comboTransf));
		
		Main.this.add(disp, BorderLayout.EAST);				
		Main.this.validate();
		Main.this.repaint();
	}
	
	private void initListeners() {
		
		ptree.addPropertyChangeListener("PTreeState", new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				extra.remove(ptree);
				extra.add(heat);
				disp.setVisible(true);
				extra.validate();
			}
		});
		
		sub.returnPanel().addMouseListener(new MouseInputAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (sub.isVisible()) {
					extra.remove(sub);
					extra.add(heat);
					disp.setVisible(true);
					extra.validate();
				}
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
					if (SwingUtilities.isLeftMouseButton(e)) {
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
					} else if (SwingUtilities.isRightMouseButton(e)) {
						int mainInd = heat.getMolArray()[mouseY - 1].getIndex();
						if (mainInd > -1) {
						try {
							sub.setIndex(mainInd, 1);
							extra.remove(heat);
							disp.setVisible(false);
							extra.add(sub);
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					}
				}
			}
			
		});
		
	}
	
	
	public JMenuBar MenuBar() {
		return menuBar;
	}


	public void initMenu() {
		final JFileChooser fc = new JFileChooser();
		menuBar = new JMenuBar();
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmImportSdf = new JMenuItem("Import sdf");
		mntmImportSdf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showOpenDialog(null);

		        if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File file = fc.getSelectedFile();
		            try {
		            	SDFreader sdf = new SDFreader(file);
		            	String fieldStr[] = sdf.fieldStr();
		            	SDFDialogue fieldDialog = new SDFDialogue(fieldStr);
		            	fieldDialog.showDial();
		            	
		            	if (fieldDialog.getFieldInd() != -1) {
		            		Main.this.removeAll();
							init(sdf, fieldDialog.getFieldInd(), fieldDialog.getIdInd(),
									fieldDialog.getNorm());
//							f.getContentPane().removeAll();
//							f.getContentPane().add(main);
//							f.validate();
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
		
		JMenuItem mntmImportSer = new JMenuItem("Load ser");
		mntmImportSer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showOpenDialog(null);

		        if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File file = fc.getSelectedFile();
					try
				      {
				         FileInputStream fileIn = new FileInputStream(file);
				         ObjectInputStream in = new ObjectInputStream(fileIn);
				         adm = (AdjMatrix) in.readObject();
				         Main.this.removeAll();
				         init(null, adm.getFieldInd(), adm.getIdInd(), adm.getNorm());
				         in.close();
				         fileIn.close();
				         
				      }
					catch(IOException i)
				      {
				         i.printStackTrace();
				         System.err.println("AdjMatrix class failed");
				         return;
				      }catch(ClassNotFoundException c)
				      {
				         System.err.println("AdjMatrix class not found");
				         c.printStackTrace();
				         return;
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
		mnFile.add(mntmImportSer);
		
		
		JMenuItem mntmExportSdf = new JMenuItem("Export sdf");
		mntmExportSdf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showSaveDialog(null);
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
		
		JMenu mnView = new JMenu("View");
		menuBar.add(mnView);
		
		JMenuItem mntmMatrix = new JMenuItem("Matrix");
		mntmMatrix.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (Main.this.heat != null) {
					extra.removeAll();//(graphView);
					disp.setVisible(true);
					extra.add(heat);
					Main.this.validate();
					Main.this.repaint();
				}
			}
		});
		mnView.add(mntmMatrix);
		
		JMenuItem mntmGraph = new JMenuItem("Graph");
		mntmGraph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (Main.this.graphView != null) {
					extra.removeAll();//(heat);
					disp.setVisible(false);
					graphView.update();
					extra.add(graphView);
					Main.this.validate();
					Main.this.repaint();
				}
			}
		});
		mnView.add(mntmGraph);
		
		menuBar.add(Box.createHorizontalGlue());
		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);
		JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null, "(c) 2015 Kostas Papadopoulos\n\n" +
			"Pre-alpha\n" +
			"No guarantees that anything will work as expected\n", "About",
					    JOptionPane.PLAIN_MESSAGE);
			}
		});
		mnHelp.add(mntmAbout);
	}
	

	
	private void init(SDFreader sdf, int fieldInd, int idInd, double norm) throws Exception {
		this.norm = norm;
		this.fieldInd = fieldInd;
		this.idInd = idInd;
		extra = new JPanel();
		extra.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		extra.setSize(new Dimension(1000, 1000));
		
		progPanel = new JPanel();
		progPanel.setLayout(new BorderLayout(0, 0));
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progPanel.add(progressBar, BorderLayout.SOUTH);
        extra.add(progPanel);
        if (sdf == null) {
        	initMain();
        	this.add(extra);
            this.validate();
        	return;
        }        
        lblMolecules = new JLabel("Molecules : " + sdf.sdfMap().size());
        progPanel.add(lblMolecules, BorderLayout.NORTH);
        this.add(extra);
        this.validate();
        task = new Task(sdf);
        task.execute();
		
	}
	
	public Main() throws Exception {
		super(new BorderLayout());
		this.setPreferredSize(new Dimension(1000, 1000));
		initMenu();
		}
	
	public static void showGUI() throws Exception {
		final JFrame f = new JFrame("Heatmap");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().setPreferredSize(new Dimension(1300, 1000));
		Main main = new Main();
		
		f.setJMenuBar(main.MenuBar());
		f.add(main);
		f.pack();
		f.setVisible(true);
	}
	
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					showGUI();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	});
}

}
