package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

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
	ArrayList<IAtomContainer> qArr;
	ArrayList<IAtomContainer> tArr;
	ArrayList<IAtomContainer> lArr;
	ArrayList<IAtomContainer> rArr;
	ArrayList<MolTransf> trArr;
	ArrayList<Double> qProp;
	ArrayList<Double> tProp;
	ArrayList<Double> dPArr;
	ArrayList<Integer> clustArr;
	int size;
	
	Integer[] transfInd;
	TreeMap<MolTransf, ArrayList<Integer>> transfFreq;
	
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
	
	private class MolTransf implements Comparable<MolTransf> {
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
			
			if (s1.compareTo(s2) > 0) {
				left = s1;
				right = s2;
				direction = 1;
			} else {
				left = s2;
				right = s1;
				direction = -1;
			}
		}
		
		public String toString() {
			return left + " -> " + right;
		}
		
		public int getDirection() {
			return direction;
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
		qArr = new ArrayList<>();
		tArr = new ArrayList<>();
		lArr = new ArrayList<>();
		rArr = new ArrayList<>();
		trArr = new ArrayList<>();
		qProp = new ArrayList<>();
		tProp = new ArrayList<>();
		dPArr = new ArrayList<>();
		clustArr = new ArrayList<>();
				
		for (int k = 0; k < adm.getCCSMSDMatr().length; ++k) {
			if (adm.molVector()[k].length > 1) {
				for (int i = 0; i < adm.molVector()[k].length; ++i) {
					for (int j = i + 1; j < adm.molVector()[k].length; ++j) {
						if (adm.getCCSMSDMatr()[k].get(i, j) == null) {
							continue;
						}						
						SMSDpair pair = (SMSDpair) adm.getCCSMSDMatr()[k].get(i, j);
						IAtomContainer query = adm.molVector()[k][i].getMol();
						
						double queryPot = adm.molVector()[k][i].getPotency();
						
						
						IAtomContainer target = adm.molVector()[k][j].getMol();
						
						double targetPot = adm.molVector()[k][j].getPotency();
						
						IAtomContainer queryBit = pair.pairDiff()[0];
						IAtomContainer targetBit = pair.pairDiff()[1];
						MolTransf mt = new MolTransf(queryBit, targetBit);

						
						if (mt.getDirection() == -1) {
							IAtomContainer tac;
							tac = target;
							target = query;
							query = tac;
							
							tac = targetBit;
							targetBit = queryBit;
							queryBit = tac;
							
							Double temp;
							temp = targetPot;
							targetPot = queryPot;
							queryPot = temp;
						}
						
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
		
		final HashMap<MolTransf, ArrayList<Integer>> hash = new HashMap<>(); // hashtable of pair , Array of indexes
		//for (int j = 0; j < 3; j++) {
		HashMap<MolTransf, Integer> test = new HashMap<>();
			for (int i = 0; i < transfInd.length; ++i) {
				
				ArrayList<Integer> keys = hash.get(trArr.get(i));
				if (keys != null) {
					System.out.println(">>>>>>> " + trArr.get(i));
				} else {
					keys = new ArrayList<>();
				}
				keys.add(i);
				hash.put(trArr.get(i), keys);
			}
		//}
		// tree to store previous table in order of frequency of key transformation
		transfFreq = new TreeMap<>(new Comparator<MolTransf>() {
			
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
		transfFreq.putAll(hash);
		
		for (int i = 0; i < hash.size(); ++i) {
			MolTransf mt = (MolTransf) hash.keySet().toArray()[i];
			int freq = transfFreq.get(mt).size();
		}
		
	}
	
	public String[] comboTransf() {
		String[] st = new String[transfFreq.size()];
		for (int i = 0; i < st.length; ++i) {
			MolTransf mt = (MolTransf) transfFreq.keySet().toArray()[i];
			int freq = transfFreq.get(mt).size();
			st[i] = mt + " (" + freq + ")";
		}
		
		return st;
	}
	
	public void printPairTansf() {
		for (String s : comboTransf())
			System.out.println(s);
	}
	
	public int size() {
		return size;
	}
	
	public static void main(String[] args) {
		
	}

}
