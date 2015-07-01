package main;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
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



public final class SMSDpair{
	private IAtomContainer query = new AtomContainer();
	private IAtomContainer target = new AtomContainer();
	private final Isomorphism smsd;
	private Collection<Integer> queryHL;
	private Collection<Integer> targetHL;
	private AtomAtomMapping atMapping;
	private ArrayList<Integer> qryConnAtom;
	private ArrayList<Integer> trgConnAtom;
	private SmilesGenerator sg = SmilesGenerator.absolute().aromatic(); 
	
	public SMSDpair(IAtomContainer mol1, IAtomContainer mol2) throws CDKException, CloneNotSupportedException {
	
		ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol1);
		ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol2);

		mol1 = ExtAtomContainerManipulator.removeHydrogens(mol1);
		mol2 = ExtAtomContainerManipulator.removeHydrogens(mol2);

		smsd = new Isomorphism(mol1, mol2, Algorithm.DEFAULT, true, true, true);
		smsd.setChemFilters(true, true, true);
		
		if (smsd != null) {
			this.query = smsd.getQuery();
			this.target = smsd.getTarget();
		
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
	 * @throws CDKException
	 * @throws Exception
	 */
	public String queryFrag() throws CDKException, Exception {
		return sg.create(this.pairDiff()[0]);
	}
	
	/**
	 * @return SMILES absolute representation of target fragment
	 * @throws CDKException
	 * @throws Exception
	 */
	public String targetFrag() throws CDKException, Exception {
		return sg.create(this.pairDiff()[1]);
	}
	
	public IAtomContainer[] pairDiff() throws CloneNotSupportedException, Exception {		// 0 query, 1 target

		IAtomContainer[] pair = new IAtomContainer[2];
		IAtomContainer Mol1 = smsd.getQuery();
		IAtomContainer Mol2 = smsd.getTarget();

		ArrayList<IAtom> toDel1 = new ArrayList<>();
		ArrayList<IAtom> toDel2 = new ArrayList<>();
		ArrayList<IBond> bond1 = new ArrayList<>();
		ArrayList<IBond> bond2 = new ArrayList<>();
		
		if (smsd.getFirstAtomMapping() == null) {
			return null;
		}

		for(IBond bond : Mol1.bonds()) {
			bond1.add(bond);
			
		}
		
		for(IBond bond : Mol2.bonds()) {
			bond2.add(bond);
		}

		
		for (Entry<IAtom, IAtom> mapping: smsd.getFirstAtomMapping().getMappingsByAtoms().entrySet()) {
			IAtom eAtom = mapping.getKey();
			toDel1.add(eAtom);
			IAtom pAtom = mapping.getValue();
			toDel2.add(pAtom);
		}
		for (IAtom atom : toDel1) {
			Mol1.removeAtomAndConnectedElectronContainers(atom);
		}


		for (IAtom atom : toDel2) {
			Mol2.removeAtomAndConnectedElectronContainers(atom);
		}
		
		pair[0] = Mol1;
		pair[1] = Mol2;
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
	
	public boolean isValid() throws CDKException, Exception {
		//if (queryFrag().equals(targetFrag())) return true;
		if (pairDiff()[0].equals(pairDiff()[1])) return true; // test if 'equals' does what we expect
		Map<Integer, Integer> map = smsd.getFirstAtomMapping().getMappingsByIndex();
		Set<Integer> tSet = new HashSet<>(this.getTrgConnAtom());
		for (int i : this.getQryConnAtom()) {
			Integer img = map.get(i);
			System.out.println(i + " " + img);
			if (img == null) return false;
			if (!tSet.contains(img)) return false;
		}
		return true;
	}
	
//	public static void main(String[] args) throws Exception {
//
//		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
//		IAtomContainer mol1 = sp.parseSmiles("FC1CCC([H])C(C)C1"); //(args[0]);
//		IAtomContainer mol2 = sp.parseSmiles("CC1CC(Cl)CCC1"); //(args[1]);
//		StructureDiagramGenerator sdg = new StructureDiagramGenerator();
//        sdg.setMolecule(mol1.clone());
//		sdg.generateCoordinates();
//        mol1 = sdg.getMolecule();
//        sdg.setMolecule(mol2.clone());
//		sdg.generateCoordinates();
//        mol2 = sdg.getMolecule();
//		SMSDpair mcsp = new SMSDpair(mol1, mol2);
//		
//		System.out.println(mcsp.getSMSD().getAllAtomMapping());
//		IAtomContainer q1 = mcsp.getSMSD().getFirstAtomMapping().getMapCommonFragmentOnQuery();
//		IAtomContainer t1 = mcsp.getSMSD().getFirstAtomMapping().getMapCommonFragmentOnTarget();
//		IAtomContainer com = mcsp.getSMSD().getFirstAtomMapping().getCommonFragment();
//		System.out.println(mcsp.getQryConnAtom());
//		System.out.println(mcsp.getTrgConnAtom());
//		
//		JPanel panel = new JPanel();
//		panel.setPreferredSize(new Dimension(1500, 600));
//		StructureDisplay tdp1 = new StructureDisplay(mcsp.rxnmol());//(mcsp.query);
//		panel.add(tdp1);
//		StructureDisplay tdp2 = new StructureDisplay(mcsp.prdmol());
//		panel.add(tdp2);
//		StructureDisplay tdp3 = new StructureDisplay(q1);
//		//tdp1.highlightSelect(mcsp.queryHi()); //keyset for query molecule
//		panel.add(tdp3);
//		StructureDisplay tdp4 = new StructureDisplay(com);
//		//tdp2.highlightSelect(mcsp.targetHi()); //values for target molecule
//		panel.add(tdp4);
//		System.out.println(mcsp.pairDiff()[0]);
//		System.out.println(mcsp.pairDiff()[1]);
//		System.out.println(mcsp.isValid());
//		
//
//
//		final JFrame f = new JFrame("Test");
//		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		f.getContentPane().setPreferredSize(new Dimension(1500, 600));
//		f.getContentPane().add(panel);
//
//		javax.swing.SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
//				f.pack();
//				f.setVisible(true);
//			}
//		});
//
//	}
}
