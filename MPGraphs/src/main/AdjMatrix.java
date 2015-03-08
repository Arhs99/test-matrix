package main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.similarity.Tanimoto;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.impl.DenseObjectMatrix2D;


public class AdjMatrix {
	private DoubleMatrix2D connMatrix;
	private Molecule[] molArray;
	private DenseObjectMatrix2D MCSMatrix;
	private static final double TANI_THRES = 0.60;
	private CComp cc;

	public AdjMatrix(TreeSet<Molecule> map) {		
		molArray = map.toArray(new Molecule[map.size()]);
		System.out.println(map.size() + "  " + molArray.length);
		connMatrix = new DenseDoubleMatrix2D(molArray.length, molArray.length);
		MCSMatrix = new DenseObjectMatrix2D(molArray.length, molArray.length);

		try {
			init(map);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Molecule[] getMolArray() {
		return molArray;
	}

	public DoubleMatrix2D getConnMatrix() {
		return connMatrix;
	}

	public DenseObjectMatrix2D getMCSMatrix() {
		return MCSMatrix;
	}

	private void init(TreeSet<Molecule> map) throws Exception {
		Molecule[] arr = map.toArray(new Molecule[map.size()]);
		for (int i = 0; i < arr.length; ++i) {

			for (int j = i+1; j < arr.length; ++j) {
				Molecule mol1 = arr[i];
				Molecule mol2 = arr[j];
				double diff = DeltaP.logDiff(mol1.getPotency(), mol2.getPotency(), 100);

				IFingerprinter fg = new Fingerprinter();
				IBitFingerprint fingerprint1 = fg.getBitFingerprint(mol1.getMol());
				IBitFingerprint fingerprint2 = fg.getBitFingerprint(mol2.getMol());
				double tanimoto = Tanimoto.calculate(fingerprint1, fingerprint2);
				if (tanimoto < TANI_THRES) continue;
				SMSDpair mcsp = new SMSDpair(mol1.getMol(), mol2.getMol());
				IAtomContainer[] pair = mcsp.pairDiff();
				if (pair == null) continue;
				System.out.println("YAPA");

				if (ConnectivityChecker.isConnected(pair[0]) && 
						ConnectivityChecker.isConnected(pair[1])) {
					connMatrix.set(i, j, -diff);
					MCSMatrix.set(i, j, mcsp);
					connMatrix.set(j, i, +diff);
					MCSMatrix.set(j, i, mcsp);
				}
			}						
		}	
	}
	//returns a sorted array of CC matrices
	/**
	 * @return
	 * a sorted array of CC matrices
	 */
	public DenseObjectMatrix2D[] getCCSMSDMatr() {
		if (cc == null) {
			cc = new CComp();
		}
		DenseObjectMatrix2D[] matArr = new DenseObjectMatrix2D[cc.count];
		for (int i = 0; i < cc.count; ++i) {
			
			int select[] = new int[cc.compArr.get(i).toArray().length];
			for(int a = 0; a < select.length; ++a) {
				select[a] =  (int) cc.compArr.get(i).toArray()[a];
			}
			matArr[i] = new DenseObjectMatrix2D(select.length, select.length);
			matArr[i].assign(MCSMatrix.viewSelection(select, select));
			//System.out.println(i + " " + connMatrix.viewSelection(select, select));
		}
		return matArr;
	}
	
	public DenseDoubleMatrix2D[] getCCDoubleMatr() {
		if (cc == null) {
			cc = new CComp();
		}
		DenseDoubleMatrix2D[] matArr = new DenseDoubleMatrix2D[cc.count];
		for (int i = 0; i < cc.count; ++i) {			
			int select[] = new int[cc.compArr.get(i).toArray().length];
			for(int a = 0; a < select.length; ++a) {
				select[a] =  (int) cc.compArr.get(i).toArray()[a];
			}
			matArr[i] = new DenseDoubleMatrix2D(select.length, select.length);
			matArr[i].assign(connMatrix.viewSelection(select, select));
		}
		return matArr;
	}
	
	//array of CCs
	public Molecule[][] molVector() {
		if (cc == null) {
			cc = new CComp();
		}
		Molecule[][] molVec = new Molecule[cc.count][];
		for (int i = 0; i < cc.count; ++i) {
			Collections.sort(cc.compArr.get(i));	//Essential to sort this vector so 'for' loop below traverses it on order
			molVec[i] = new Molecule[cc.compArr.get(i).toArray().length];
			for(int a = 0; a < cc.compArr.get(i).toArray().length; ++a) {
				molVec[i][a] =  molArray[(int) cc.compArr.get(i).toArray()[a]];
			}
		}
		
		return molVec;
	}
	
	
	//Identify connected components
	private class CComp {
		private byte[] vColor;   //1 for white vertices
	    private int[] id;           
	    private int[] size;        
	    private int count;
	    private ArrayList<ArrayList<Integer>> compArr;
	    
	    public CComp() {
	        vColor = new byte[molArray.length];
	        compArr = new ArrayList<ArrayList<Integer>>();
	        for (int i = 0; i < vColor.length; ++i) {
	        	vColor[i] = 1;
	        }
	        count = 0;
	        id = new int[molArray.length];
	        size = new int[molArray.length];
	        for (int v = 0; v < molArray.length; v++) {
	            if (vColor[v] == 1) {
	            	compArr.add(new ArrayList<Integer>());
	                dfs(v);
	                count++;
	            }
	        }
	        //Sort the array by size of connceted components
	        Collections.sort(compArr, new Comparator<ArrayList<Integer>>(){ 
	        	public int compare(ArrayList<Integer> o1, ArrayList<Integer> o2) {
	        		return o2.size() - o1.size(); // sort inverse order
	        	}
	        }
	        );
	        
	    }
	    // run dfs to get CC it is high cost since it essentially treats the graph as directed
	    private void dfs(int v) {
	        vColor[v] = 0;
	        for (int w = 0; w < molArray.length; w++) {
	        	if (MCSMatrix.getQuick(v, w) == null)
	        		continue;
	            if (vColor[w] == 1) {
	                dfs(w);
	            }
	        }
	        id[v] = count;
	        compArr.get(count).add(v);
	        size[count]++;
	    }
		
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
			String val = sdf.sdfMap().get(mol)[0]; 	// index of field is 0
			if (val == null || mol.getAtomCount() == 0) {
				continue;
			}
			ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
			mol = ExtAtomContainerManipulator.removeHydrogens(mol);
			ExtAtomContainerManipulator.aromatizeCDK(mol);

			Molecule molec = new Molecule(mol, Double.parseDouble(val), s);
			map.add(molec);
			++cnt;
			if (cnt == 20) break;
		}

		final AdjMatrix adm = new AdjMatrix(map);
		final SideDisplay disp = new SideDisplay();
		final HeatMap heat = new HeatMap(adm, true, disp, Gradient.GRADIENT_RED_TO_GREEN);
		int CCcount = adm.getCCDoubleMatr().length;
		final String comboDesc[] = new String[CCcount + 1];
		comboDesc[0] = "Show all";
		for (int i = 1; i < CCcount + 1 && adm.molVector()[i-1].length > 1; ++i) {
			comboDesc[i] = "Cluster " + i + " : " + adm.molVector()[i-1].length +
					" compounds";
		}
		//PrintStream out = new PrintStream(new FileOutputStream("output.txt"));
		//System.setOut(out);
		//System.out.println(adm.getCCDoubleMatr()[0]);



		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				
				final JFrame f = new JFrame("Heatmap");
				final JPanel extra = new JPanel();

				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				f.getContentPane().setLayout(new BorderLayout());
				heat.setToolTipText("");
				f.getContentPane().add(extra, BorderLayout.WEST);
				extra.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
				extra.setSize(new Dimension(1000, 1000));
				extra.add(heat);
				
				
				JComboBox comboBox = new JComboBox();
				comboBox.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JComboBox cb = (JComboBox)e.getSource();
						int ind = cb.getSelectedIndex();
						heat.updateMap(ind);
						f.validate();
					}
				});
				comboBox.setAlignmentY(Component.TOP_ALIGNMENT);
				disp.add(comboBox);
				comboBox.setModel(new DefaultComboBoxModel(comboDesc));
				f.getContentPane().add(disp, BorderLayout.EAST);
				f.pack();
				f.setVisible(true);
			}
		});


	}


}
