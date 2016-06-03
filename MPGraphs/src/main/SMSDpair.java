package main;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.ShortestPaths;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.smsd.AtomAtomMapping;
import org.openscience.smsd.Isomorphism;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.smsd.interfaces.Algorithm;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;

import viewer.StructureDisplay;



public final class SMSDpair implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private IAtomContainer query = new AtomContainer();
	private IAtomContainer target = new AtomContainer();
	transient private final Isomorphism smsd;
	private Collection<Integer> queryHL;
	private Collection<Integer> targetHL;
	private AtomAtomMapping atMapping;
	private ArrayList<Integer> qryConnAtom;
	private ArrayList<Integer> trgConnAtom;
	transient private SmilesGenerator sg = SmilesGenerator.unique(); //absolute().aromatic(); 
	private IAtomContainer[] pair;

	public SMSDpair(IAtomContainer mol1, IAtomContainer mol2) throws Exception {

		ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol1);
		ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol2);

		mol1 = ExtAtomContainerManipulator.removeHydrogens(mol1);
		mol1.setStereoElements(new ArrayList(0));
		//		See http://efficientbits.blogspot.co.uk/2015/10/java-serialization-great-power-but-at.html		
		mol2 = ExtAtomContainerManipulator.removeHydrogens(mol2);
		mol2.setStereoElements(new ArrayList(0));

		smsd = new Isomorphism(mol1, mol2, Algorithm.DEFAULT, true, true, true);
		smsd.setChemFilters(true, true, true);

		if (smsd != null) {
			this.query = smsd.getQuery();
			this.target = smsd.getTarget();

			this.pair = new IAtomContainer[2];

			setPairDiff();

			atMapping = smsd.getFirstAtomMapping();
			//atMapping = smsd.getAllAtomMapping().get(0);

			queryHL = new HashSet<>();
			for (int i = 0; i < query.getAtomCount(); ++i) {
				if (! atMapping.getMappingsByIndex().keySet().contains(i)) {
					queryHL.add(i);
				}
			}
			targetHL = new HashSet<>();
			for (int i = 0; i < target.getAtomCount(); ++i) {
				if (!atMapping.getMappingsByIndex().values().contains(i)) {
					targetHL.add(i);
				}
			}

			qryConnAtom = new ArrayList<>();
			for (int i : atMapping.getMappingsByIndex().keySet()) {
				for (int j : queryHi()) {
					if (query.getBond(query.getAtom(i), query.getAtom(j)) != null) {
						qryConnAtom.add(i);
					}
				}
			}

			trgConnAtom = new ArrayList<>();
			for (int i : atMapping.getMappingsByIndex().values()) {
				for (int j : targetHi()) {
					if (target.getBond(target.getAtom(i), target.getAtom(j)) != null) {
						trgConnAtom.add(i);
					}
				}
			}

			/*		Handle the case when one of (qry, target) mcs pair has no substituent */
			if (queryHi().isEmpty() && !targetHi().isEmpty()) {
				for (int i: atMapping.getMappingsByIndex().keySet()) {
					if (trgConnAtom.contains(atMapping.getMappingsByIndex().get(i))) {
						qryConnAtom.add(i);
					}
				}
			}

			if (!queryHi().isEmpty() && targetHi().isEmpty()) {
				for (int i: qryConnAtom) {
					trgConnAtom.add(atMapping.getMappingsByIndex().get(i));
				}
			}
		}		

	}
	
	public IAtomContainer rxnmol() {
		return smsd.getQuery();
		//return query;
	}

	public IAtomContainer prdmol() {
		return smsd.getTarget();
		//return target;
	}
	
	/**
	 * @return SMILES absolute representation of query fragment
	 * @throws CloneNotSupportedException 
	 * @throws CDKException
	 * @throws Exception
	 */
	public String queryFrag() throws CloneNotSupportedException {
		try {
			IAtomContainer ac1 = this.pairDiff()[0].clone();
			for (IAtom a : ac1.atoms()) {
				if (a.getImplicitHydrogenCount() == null)
					a.setImplicitHydrogenCount(0);
			}
			return sg.create(ac1);
		} catch (CDKException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	
	/**
	 * @return SMILES absolute representation of target fragment
	 * @throws CloneNotSupportedException 
	 * @throws CDKException
	 * @throws Exception
	 */
	public String targetFrag() throws CloneNotSupportedException {
		
		try {
			IAtomContainer ac1 = this.pairDiff()[1].clone();
			for (IAtom a : ac1.atoms()) {
				if (a.getImplicitHydrogenCount() == null)
					a.setImplicitHydrogenCount(0);
			}
			return sg.create(ac1);
		} catch (CDKException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	
	private void setPairDiff() throws CloneNotSupportedException, Exception {		// 0 query, 1 target
		IAtomContainer Mol1 = smsd.getQuery().clone();
		IAtomContainer Mol2 = smsd.getTarget().clone();

		ArrayList<Integer> toDel1 = new ArrayList<>();
		ArrayList<Integer> toDel2 = new ArrayList<>();
//		ArrayList<IBond> bond1 = new ArrayList<>();
//		ArrayList<IBond> bond2 = new ArrayList<>();
//		System.out.println(Mol1.getAtom(5));
//		System.out.println(smsd.getQuery().getAtom(5));
//		System.out.println(Mol2.getAtom(5));
//		System.out.println(smsd.getTarget().getAtom(5));
		
//		if (smsd.getFirstAtomMapping() == null) {
//			return;
//		}

		//System.out.println(smsd.getFirstAtomMapping().getMappingsByIndex());
		
		for (Entry<IAtom, IAtom> mapping: smsd.getFirstAtomMapping().getMappingsByAtoms().entrySet()) {
			IAtom eAtom = mapping.getKey();
			toDel1.add(smsd.getQuery().getAtomNumber(eAtom));
			IAtom pAtom = mapping.getValue();
			toDel2.add(smsd.getTarget().getAtomNumber(pAtom));			
		}
		
		
		IAtom[] qAArr = new IAtom[Mol1.getAtomCount()];
		for (int i = 0; i < qAArr.length; ++i) {
			qAArr[i] = Mol1.getAtom(i);
		}
		
		IAtom[] tAArr = new IAtom[Mol2.getAtomCount()];
		for (int i = 0; i < tAArr.length; ++i) {
			tAArr[i] = Mol2.getAtom(i);
		}
		
		for (Integer i : toDel1) {
			IAtom atom = qAArr[i];//Mol1.getAtom(i);
			if (atom == null) continue;
			//Mol1.removeAtom(i);
			Mol1.removeAtomAndConnectedElectronContainers(atom);
			
		}
		for (Integer i : toDel2) {
			IAtom atom = tAArr[i]; //Mol2.getAtom(i);
			if (atom == null) continue;
			//Mol2.removeAtom(atom);
			Mol2.removeAtomAndConnectedElectronContainers(atom);			
		}
		
		pair[0] = Mol1;
		pair[1] = Mol2;
	}
	
	public IAtomContainer[] pairDiff() {
		return pair;
	}
	/**
	 * ordered pair of molecules with B - AB
	 * @return
	 * @throws Exception
	 */
	public Collection<Integer> queryHi() {
		return queryHL;
	}
	
	public Collection<Integer> targetHi() {
		return targetHL;
	}
	
//	public Isomorphism getSMSD() {
//		return smsd;
//	}
	
	/**
	 * @return array of indices of connection atoms in query AtomContainer
	 */
	public ArrayList<Integer> getQryConnAtom() {
		return qryConnAtom;
	}
	
	/**
	 * @return array of indices of connection atoms in target AtomContainer
	 */
	public ArrayList<Integer> getTrgConnAtom() {
		return trgConnAtom;
	}
	
	/**
	 * @return distance of qryConn and targetCon atoms refernced on qryConn atom
	 */
	public int distQT() {
		if (!getQryConnAtom().isEmpty() && !getTrgConnAtom().isEmpty() &&
				!atMapping.getMappingsByIndex().isEmpty()) {
			int qry = getQryConnAtom().get(0);
			int tar = getTrgConnAtom().get(0);
			Integer ref = atMapping.getMappingsByIndex().get(tar);
			if (ref == null) return 99;
			ShortestPaths sp = new ShortestPaths(query, query.getAtom(qry));
			return sp.distanceTo(query.getAtom(ref));
		}
		return 99;
	}
	public boolean isValid() throws CDKException, Exception {
		if (!ConnectivityChecker.isConnected(pair[0]) || 
				!ConnectivityChecker.isConnected(pair[1])) {
			return false;
		}

		//System.out.println(distQT());
		if (queryFrag().equals(targetFrag()) && distQT() < 4) {
		//checking if qry and target fragments are identical and so no need to check if they are 
		//attached to the same atom in the MCS **unless** their distance is >= 4
		//correct is to check if they are on same ring rather than their distance **ToDO**
			return true;
		}
		
		Map<Integer, Integer> map = atMapping.getMappingsByIndex();
		Set<Integer> tSet = new HashSet<>(this.getTrgConnAtom());
		for (int i : this.getQryConnAtom()) {
			Integer img = map.get(i);
			//System.out.println(i + " " + img);
			if (img == null) return false;
			if (!tSet.contains(img)) return false;
		}
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println("ha");
		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		IAtomContainer mol1 = sp.parseSmiles("FCCOC1CCC([H])C(CCCCCCCC)C1"); //(args[0]);
		IAtomContainer mol2 = sp.parseSmiles("CCCCCCCCC1C(OCCF)CCCC1"); //(args[1]);
		StructureDiagramGenerator sdg = new StructureDiagramGenerator();
        sdg.setMolecule(mol1.clone());
		sdg.generateCoordinates();
        mol1 = sdg.getMolecule();
        sdg.setMolecule(mol2.clone());
		sdg.generateCoordinates();
        mol2 = sdg.getMolecule();
		SMSDpair mcsp = new SMSDpair(mol1, mol2);
		
//		System.out.println(mcsp.getSMSD().getAllAtomMapping());
//		IAtomContainer q1 = mcsp.getSMSD().getFirstAtomMapping().getMapCommonFragmentOnQuery();
//		IAtomContainer t1 = mcsp.getSMSD().getFirstAtomMapping().getMapCommonFragmentOnTarget();
//		IAtomContainer com = mcsp.getSMSD().getFirstAtomMapping().getCommonFragment();
		System.out.println(mcsp.getQryConnAtom());
		System.out.println(mcsp.getTrgConnAtom());
		
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(1500, 600));
		StructureDisplay tdp1 = new StructureDisplay(mcsp.rxnmol());//(mcsp.query);
		panel.add(tdp1);
		StructureDisplay tdp2 = new StructureDisplay(mcsp.prdmol());
		panel.add(tdp2);
		StructureDisplay tdp3 = new StructureDisplay(mcsp.pairDiff()[0]);
		tdp1.highlightSelect(mcsp.queryHi()); //keyset for query molecule
		panel.add(tdp3);
		StructureDisplay tdp4 = new StructureDisplay(mcsp.pairDiff()[1]);
		tdp2.highlightSelect(mcsp.targetHi()); //values for target molecule
		panel.add(tdp4);
		//System.out.println(mcsp.pairDiff()[0]);
		//System.out.println(mcsp.pairDiff()[1]);
		System.out.println(mcsp.isValid());
		


		final JFrame f = new JFrame("Test");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().setPreferredSize(new Dimension(1500, 600));
		f.getContentPane().add(panel);

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				f.pack();
				f.setVisible(true);
			}
		});

	}
}
