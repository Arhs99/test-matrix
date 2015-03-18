package main;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import viewer.HeatMap;
import viewer.SideDisplay;

public class Main {
	private final AdjMatrix adm;
	final SideDisplay disp;
	final HeatMap heat;
	
	public Main(File file) throws Exception {
		SDFreader sdf = new SDFreader(file);
		Set<Molecule> set = new TreeSet<>();
		int cnt = 0;
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
			if (cnt == 10) break;
		}
		adm = new AdjMatrix(set);
		disp = new SideDisplay();
		heat = new HeatMap(adm, true, disp, Gradient.GRADIENT_RED_TO_GREEN);
		int CCcount = adm.getCCDoubleMatr().length;
		final String comboDesc[] = new String[CCcount + 1];
		comboDesc[0] = "Show all";
		for (int i = 1; i < CCcount + 1 && adm.molVector()[i-1].length > 1; ++i) {
			comboDesc[i] = "Cluster " + i + " : " + adm.molVector()[i-1].length +
					" compounds";
		}
	}
	
	public static void showGUI() {
		final JFrame f = new JFrame("Heatmap");
		final JPanel extra = new JPanel();

		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().setLayout(new BorderLayout());
//		heat.setToolTipText("");
//		f.getContentPane().add(extra, BorderLayout.WEST);
//		extra.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
//		extra.setSize(new Dimension(1000, 1000));
//		extra.add(heat);
//		
//		
//		
//		
//		JComboBox comboBox = new JComboBox();
//		comboBox.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				JComboBox cb = (JComboBox)e.getSource();
//				int ind = cb.getSelectedIndex();
//				heat.updateMap(ind);
//				f.validate();
//			}
//		});
//		comboBox.setAlignmentY(Component.TOP_ALIGNMENT);
//		disp.add(comboBox);
//		comboBox.setModel(new DefaultComboBoxModel(comboDesc));
//		f.getContentPane().add(disp, BorderLayout.EAST);
		
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
						Main main = new Main(file);
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
