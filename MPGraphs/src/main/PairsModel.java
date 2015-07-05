package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;


public class PairsModel {
	ArrayList<Molecule> qArr;
	ArrayList<Molecule> tArr;
	ArrayList<IAtomContainer> lArr;
	ArrayList<IAtomContainer> rArr;
	ArrayList<MolTransf> trArr;
	ArrayList<Double> qProp;
	ArrayList<Double> tProp;
	ArrayList<Double> dPArr;
	ArrayList<Integer> clustArr;
	ArrayList<Collection<Integer>> qHilArr;
	ArrayList<Collection<Integer>> tHilArr;
	int size;
	double norm;
	
	Integer[] transfInd;
	TreeMap<MolTransf, ArrayList<Integer>> transfFreq;
	private AdjMatrix adm;
	
    /**
     * Adds explicit hydrogens (without coordinates) to the IAtomContainer,
     * equaling the number of set implicit hydrogens.
     *
     * @param atomContainer the atom container to consider
     * @cdk.keyword hydrogens, adding
     */
    public static void convertImplicitToExplicitHydrogens(IAtomContainer atomContainer) {
        List<IAtom> hydrogens = new ArrayList<IAtom>();
        List<IBond> newBonds = new ArrayList<IBond>();
        List<Integer> atomIndex = new ArrayList<Integer>();

        for (IAtom atom : atomContainer.atoms()) {
            if (!atom.getSymbol().equals("H")) {
                Integer hCount = atom.getImplicitHydrogenCount();
                if (hCount != null) {
                    for (int i = 0; i < hCount; i++) {

                        IAtom hydrogen = atom.getBuilder().newInstance(IAtom.class, "H");
                        hydrogen.setAtomTypeName("H");
                        hydrogen.setImplicitHydrogenCount(0);
                        hydrogens.add(hydrogen);
                        newBonds.add(atom.getBuilder().newInstance(IBond.class,
                                atom, hydrogen, CDKConstants.BONDORDER_SINGLE
                        ));
                    }
                    atom.setImplicitHydrogenCount(0);
                }
            }
        }
        for (IAtom atom : hydrogens) atomContainer.addAtom(atom);
        for (IBond bond : newBonds) atomContainer.addBond(bond);
    }
	
	public class MolTransf implements Comparable<MolTransf> {

		private int direction;
		private String left;
		private String right;
		public MolTransf(IAtomContainer ac1, IAtomContainer ac2) throws CDKException {
			SmilesGenerator sg = SmilesGenerator.unique();
			String s1 = "";
			convertImplicitToExplicitHydrogens(ac1);
			convertImplicitToExplicitHydrogens(ac2);
//			IChemObjectBuilder     builder = SilentChemObjectBuilder.getInstance();
////			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(ac1);
//			CDKHydrogenAdder.getInstance(builder).addImplicitHydrogens(ac1);
////			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(ac2);
//			CDKHydrogenAdder.getInstance(builder).addImplicitHydrogens(ac2);
			if (ac1.getAtomCount() != 0) {
			try {
				for (IAtom a : ac1.atoms()) {
					if (a.getImplicitHydrogenCount() == null)
						a.setImplicitHydrogenCount(0);
				}
				s1 = sg.create(ac1);
				s1 = s1.replace("([H])", "");
				s1 = s1.replace("[H]", "");
				s1 = s1.replace("[C]", "C");
				s1 = s1.replace("[N]", "N");
				//s1 = s1.replaceAll("\\[[HCN]\\]", "");
				s1 = s1.replace(" ", "");
			} catch (CDKException e) {
				// TODO Auto-generated catch block
				System.err.println("ac1 " + ac1);
				System.err.println(e.toString());
				e.printStackTrace();
				s1="%%";
			}
			}
			
			
			String s2 = "";
			if (ac2.getAtomCount() != 0) {
			try {
				for (IAtom a : ac2.atoms()) {
					if (a.getImplicitHydrogenCount() == null)
						a.setImplicitHydrogenCount(0);
				}
				s2 = sg.create(ac2);
				s2 = s2.replace("([H])", "");
				s2 = s2.replace("[H]", "");
				s2 = s2.replace("[C]", "C");
				s2 = s2.replace("[N]", "N");
				//s2 = s2.replaceAll("\\[[HCN]\\]", "");
				s2 = s2.replace(" ", "");
			} catch (CDKException e) {
				// TODO Auto-generated catch block
				System.err.println("ac2 " + ac2);
				System.err.println(e.toString());
				e.printStackTrace();
				s2 = "%%";
			}
			}
			
			if (s1.compareTo(s2) > 0 || s1.equals(s2)) {
				left = s1;
				right = s2;
				direction = 1;
			} else {
				left = s2;
				right = s1;
				direction = -1;
			}
		}
		
		public MolTransf() {
			// TODO Auto-generated constructor stub
		}

		public MolTransf invPair() {
			MolTransf inv = new MolTransf();
			inv.right = this.left;
			inv.left = this.right;
			inv.direction = - this.direction;
			return inv;
		}
		
		public String toString() {
			return left + " -> " + right;
		}
		
		public int getDirection() {
			return direction;
		}
		
		public String getLeft() {
			return left;
		}

		public String getRight() {
			return right;
		}

		@Override
		public int hashCode() {
			return this.toString().hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			return (o.getClass() == this.getClass() && 
					this.toString().equals(o.toString()));
		}
		
		@Override
		public int compareTo(MolTransf o) {
//			if (this.hashCode() == o.hashCode()) {
//				return 0;
//			} else
			return this.toString().compareTo(o.toString());
			//return this.left.concat(this.right).compareTo(o.left.concat(o.right));
		}
	}
	
	public PairsModel(AdjMatrix adm, double norm) throws Exception {
		this.adm = adm;
		qArr = new ArrayList<>();
		tArr = new ArrayList<>();
		lArr = new ArrayList<>();
		rArr = new ArrayList<>();
		trArr = new ArrayList<>();
		qProp = new ArrayList<>();
		tProp = new ArrayList<>();
		dPArr = new ArrayList<>();
		clustArr = new ArrayList<>();
		qHilArr = new ArrayList<>();
		tHilArr = new ArrayList<>();
		
		this.norm = norm;
				
		for (int k = 0; k < adm.getCCSMSDMatr().length; ++k) {
			if (adm.molVector()[k].length > 1) {
				for (int i = 0; i < adm.molVector()[k].length; ++i) {
					for (int j = i + 1; j < adm.molVector()[k].length; ++j) {
						if (adm.getCCSMSDMatr()[k].get(i, j) == null) {
							continue;
						}						
						SMSDpair pair = (SMSDpair) adm.getCCSMSDMatr()[k].get(i, j);
						Molecule query = adm.molVector()[k][i];												
						double queryPot = adm.molVector()[k][i].getPotency();						
						
						Molecule target = adm.molVector()[k][j];
						double targetPot = adm.molVector()[k][j].getPotency();
						
						IAtomContainer queryBit = pair.pairDiff()[0];
						IAtomContainer targetBit = pair.pairDiff()[1];
						
						Collection<Integer> queryHi = pair.queryHi();
						Collection<Integer> targetHi = pair.targetHi();
						
						MolTransf mt = new MolTransf(queryBit, targetBit);
						
						double dlnX = (DeltaP.logDiff(queryPot, targetPot, norm) == Double.MIN_VALUE ?
								0 : DeltaP.logDiff(queryPot, targetPot, norm));
						
						qArr.add(query);
						qProp.add(queryPot);
						tArr.add(target);
						tProp.add(targetPot);
						lArr.add(queryBit);
						rArr.add(targetBit);
						trArr.add(mt);
						clustArr.add(k + 1);
						dPArr.add(dlnX);
						qHilArr.add(queryHi);
						tHilArr.add(targetHi);
					}
				}				
			}
		}
		this.size = qArr.size();
		transfInd = new Integer[size];		// index of transformation strings
		
		for (int i = 0; i < size; ++i) {
			transfInd[i] = i;
		}
		
		Arrays.sort(transfInd, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return trArr.get(o1).compareTo(trArr.get(o2));
			}
			
		});
		
		transfFreq = transfMap(transfInd);
		
	}
	
	private TreeMap<MolTransf, ArrayList<Integer>> transfMap(Integer[] arr) {
		final HashMap<MolTransf, ArrayList<Integer>> hash = new HashMap<>(); // hashtable of pair , Array of indexes
		for (int i = 0; i < arr.length; ++i) {				
			ArrayList<Integer> keys = hash.get(trArr.get(arr[i]));
			if (keys == null) {
				keys = new ArrayList<>();
			}
			keys.add(arr[i]);
			hash.put(trArr.get(arr[i]), keys);
			
			if (!trArr.get(arr[i]).left.equals(trArr.get(arr[i]).right)) {
				//insert the inverse transforms too but not when left == right
				MolTransf inv = trArr.get(arr[i]).invPair();		
				keys = hash.get(inv);
				if (keys == null) {
					keys = new ArrayList<>();
				}
				keys.add(arr[i]);
				hash.put(inv, keys);
			}
						
		}
	// tree to store previous table in order of frequency of key transformation
		TreeMap<MolTransf, ArrayList<Integer>>transfMap = new TreeMap<>
			(new Comparator<MolTransf>() {
		@Override
		public int compare(MolTransf o1, MolTransf o2) {
			if (hash.get(o1).size() < hash.get(o2).size()) {
				return 1;
			} else if (hash.get(o1).size() > hash.get(o2).size()) {
				return -1;
			} else {
				return o1.compareTo(o2);
			}
		}
		
	});		
	transfMap.putAll(hash);
	return transfMap;
	}
	
	/** String representation of transformation sets sorted by size
	 * contains all clusters
	 * @return
	 */
	public String[] comboTransf() {
		String[] st = new String[transfFreq.size()];
		for (int i = 0; i < st.length; ++i) {
			MolTransf mt = (MolTransf) transfFreq.keySet().toArray()[i];
			int freq = transfFreq.get(mt).size();
			st[i] = mt + " (" + freq + ")";
		}
		
		return st;
	}
	
	/**
	 * @param n	cluster number (1 to ) n=0 for all clusters
	 * @return
	 */
	public TreeMap<MolTransf, ArrayList<Integer>> TransfClustMap(int n) {
		if (n == 0) return transfFreq;
		ArrayList<Integer> arr = new ArrayList<>();
		for (int i = 0; i < clustArr.size(); i++) {
			if (clustArr.get(i) == n) {
				arr.add(i);
			}
		}
		
		TreeMap<MolTransf, ArrayList<Integer>> temp =
				transfMap( arr.toArray(new Integer[arr.size()]));
		return temp;		
	}
	
	
	public String[] comboTransfClust(int n) {
		 TreeMap<MolTransf, ArrayList<Integer>> temp = TransfClustMap(n);
		
		String[] st = new String[temp.size()];
		for (int i = 0; i < st.length; ++i) {
			MolTransf mt = (MolTransf) temp.keySet().toArray()[i];
			int freq = temp.get(mt).size();
			st[i] = mt + " (" + freq + ")";
		}		
		return st;		
	}
	
	public void printPairTansf() {
		for (String s : comboTransf())
			System.out.println(s);
		System.out.println();
		for (String s : comboTransfClust(1))
			System.out.println(s);
	}
	
	public AdjMatrix getAdm() {
		return adm;
	}
	
	public int size() {
		return size;
	}
	
	public ArrayList<Molecule> getqArr() {
		return qArr;
	}

	public ArrayList<Molecule> gettArr() {
		return tArr;
	}

	public ArrayList<IAtomContainer> getlArr() {
		return lArr;
	}

	public ArrayList<IAtomContainer> getrArr() {
		return rArr;
	}

	public ArrayList<MolTransf> getTrArr() {
		return trArr;
	}

	public ArrayList<Double> getqProp() {
		return qProp;
	}

	public ArrayList<Double> gettProp() {
		return tProp;
	}

	public ArrayList<Double> getdPArr() {
		return dPArr;
	}

	public ArrayList<Integer> getClustArr() {
		return clustArr;
	}

	public double getNorm() {
		return norm;
	}

	public ArrayList<Collection<Integer>> getqHilArr() {
		return qHilArr;
	}

	public ArrayList<Collection<Integer>> gettHilArr() {
		return tHilArr;
	}

	public static void main(String[] args) {
		
	}

}
