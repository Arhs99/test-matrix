package main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import viewer.HeatMap;
import viewer.SideDisplay;

public class Main extends JPanel implements PropertyChangeListener {
	private AdjMatrix adm;
	private SideDisplay disp;
	private HeatMap heat;
	private Task task;
	private JProgressBar progressBar;
	private JPanel extra;
	
	class Task extends SwingWorker<AdjMatrix, Void> {
		private File file;

		public Task(File file) {
			this.file = file;
		}
		@Override
		public AdjMatrix doInBackground() throws Exception {
			SDFreader sdf = new SDFreader(file);
			Set<Molecule> set = new TreeSet<>();
			int cnt = 0;
			//int progress = 0;
			//this.setProgress(progress);
			for (IAtomContainer mol : sdf.sdfMap().keySet()) {
				String s = "Rfms Ic50 Um Hpad4 Avg";
				String val = sdf.sdfMap().get(mol)[1]; 	// index of field is 0
				if (val == null || mol.getAtomCount() == 0) {
					continue;
				}
				
				//ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
				//mol = ExtAtomContainerManipulator.removeHydrogens(mol);
				//ExtAtomContainerManipulator.aromatizeCDK(mol);

				Molecule molec = new Molecule(mol, Double.parseDouble(val), s);
				set.add(molec);
				++cnt;
				//progress += 10;
                //this.setProgress(progress);
				if (cnt == 20) break;
			}
			return new AdjMatrix(set, progressBar);
			//return null;
		}
		
		public void done() {
			try {
				adm = get();
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
				extra.remove(progressBar);
				extra.add(heat);
				JComboBox comboBox = new JComboBox();
				comboBox.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JComboBox cb = (JComboBox)e.getSource();
						int ind = cb.getSelectedIndex();
						heat.updateMap(ind);
						Main.this.validate();
					}
				});
				comboBox.setAlignmentY(Component.TOP_ALIGNMENT);
				disp.add(comboBox);
				comboBox.setModel(new DefaultComboBoxModel(comboDesc));
				Main.this.add(disp, BorderLayout.EAST);
				
				Main.this.validate();
				Main.this.repaint();
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
//		if ("progress" == evt.getPropertyName()) {
//            int progress = (Integer) evt.getNewValue();
//            System.out.println(progress);
//            progressBar.setValue(progress);
//		}
	}
	
	public Main(File file) throws Exception {
		super(new BorderLayout());
		this.setPreferredSize(new Dimension(1000, 1000));
		extra = new JPanel();
		extra.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		extra.setSize(new Dimension(1000, 1000));
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
        progressBar.setStringPainted(true);
        extra.add(progressBar);
        this.add(extra);
        //this.repaint();
		Task task = new Task(file);
		task.addPropertyChangeListener(this);
		task.execute();
		//JPanel extra = new JPanel();
		
		
		
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
		            	//f.removeAll();
						Main main = new Main(file);
						f.getContentPane().removeAll();
						f.getContentPane().add(main);//, BorderLayout.WEST);
						f.validate();
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
