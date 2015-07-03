package main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.similarity.Tanimoto;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;

import viewer.HeatMap;
import viewer.PairsTree;
import viewer.SideDisplay;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.impl.DenseObjectMatrix2D;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MouseInputAdapter;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;


public class AdjMatrix {
	private DoubleMatrix2D connMatrix;
	private Molecule[] molArray;
	private DenseObjectMatrix2D MCSMatrix;
	private static final double TANI_THRES = 0.60;
	private CComp cc;
	private JProgressBar progressBar;

	public AdjMatrix(Set<Molecule> map) {		
		molArray = map.toArray(new Molecule[map.size()]);
		connMatrix = new DenseDoubleMatrix2D(molArray.length, molArray.length);
		MCSMatrix = new DenseObjectMatrix2D(molArray.length, molArray.length);

		try {
			init(map);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public AdjMatrix(Set<Molecule> map, JProgressBar progressBar) {
		molArray = map.toArray(new Molecule[map.size()]);
		connMatrix = new DenseDoubleMatrix2D(molArray.length, molArray.length);
		MCSMatrix = new DenseObjectMatrix2D(molArray.length, molArray.length);
		this.progressBar = progressBar;
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

	private void init(Set<Molecule> map) throws Exception {
		Molecule[] arr = map.toArray(new Molecule[map.size()]);
		if (progressBar != null) {
			progressBar.setValue(0);
		}
		for (int i = 0; i < arr.length; ++i) {
			System.out.print("\r " + (i + 1) + " out of " + arr.length + " molecules");
			if (progressBar != null) {
				progressBar.setValue(100 * (i + 1) / arr.length);
				System.out.println(100 * (i + 1) / arr.length);
			}
			for (int j = i+1; j < arr.length; ++j) {
				Molecule mol1 = arr[i];
				IAtomContainer ac1 = mol1.getMol();
				ac1.setProperty(mol1.getFieldName(), mol1.getPotency());
				Molecule mol2 = arr[j];
				IAtomContainer ac2 = mol2.getMol();
				ac2.setProperty(mol2.getFieldName(), mol2.getPotency());
				double diff = DeltaP.logDiff(mol1.getPotency(), mol2.getPotency(), 100);
				IFingerprinter fg = new Fingerprinter();
				IBitFingerprint fingerprint1 = fg.getBitFingerprint(ac1);
				IBitFingerprint fingerprint2 = fg.getBitFingerprint(ac2);
				double tanimoto = Tanimoto.calculate(fingerprint1, fingerprint2);
				if (tanimoto < TANI_THRES) continue;
				SMSDpair mcsp = new SMSDpair(ac1, ac2);
				IAtomContainer[] pair = mcsp.pairDiff();
				if (pair == null) continue;

				//if (ConnectivityChecker.isConnected(pair[0]) && 
				//		ConnectivityChecker.isConnected(pair[1]) &&
				if (mcsp.isValid()) {
					connMatrix.set(i, j, -diff);
					MCSMatrix.set(i, j, mcsp);
					connMatrix.set(j, i, +diff);
					MCSMatrix.set(j, i, mcsp);
					
					mol1.setIndex(i);
					mol2.setIndex(j);
					for (int key : mcsp.getQryConnAtom()) {
						Set<Integer> set = mol1.getAtomMapping().get(key);
						if (set == null) {
							set = new TreeSet<Integer>();
						}
						
						set.add(j);		// Add to set the index j of target molecule
						mol1.getAtomMapping().put(key, set);  
					}
					
					for (int key : mcsp.getTrgConnAtom()) {
						Set<Integer> set = mol2.getAtomMapping().get(key);
						if (set == null) {
							set = new TreeSet<Integer>();
						}
						
						set.add(i);		// Add to set the index j of query molecule
						mol2.getAtomMapping().put(key, set);
					}
				}
			}						
		}
		System.out.println();
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
			Collections.sort(cc.compArr.get(i));	//Essential to sort this vector so 'for' loop below traverses it in order
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
			if (cnt == 20) break;
		}

		AdjMatrix adm = new AdjMatrix(map);
//		FileOutputStream fos = new FileOutputStream("test.adm");
//		ObjectOutputStream oos = new ObjectOutputStream(fos);
//		oos.writeObject(adm.molArray);
//		oos.close();
		PairsModel pm = new PairsModel(adm, 100);
		pm.printPairTansf();
		System.out.println(pm.size());
	}


}
