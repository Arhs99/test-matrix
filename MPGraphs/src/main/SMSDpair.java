package main;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.smsd.Isomorphism;
import org.openscience.smsd.interfaces.Algorithm;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;

import viewer.StructureDisplay;



public final class SMSDpair{
	private final IAtomContainer query;
	private final IAtomContainer target;
	private final Isomorphism smsd;
	private Collection<Integer> queryHL;
	private Collection<Integer> targetHL;
	private SmilesGenerator sg = SmilesGenerator.absolute().aromatic(); 
	
	public SMSDpair(IAtomContainer mol1, IAtomContainer mol2) throws CDKException, CloneNotSupportedException {

		this.query = mol1.clone();
		this.target = mol2.clone();
		
		
		ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol1);
		ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol2);

		mol1 = ExtAtomContainerManipulator.removeHydrogens(mol1);
		mol2 = ExtAtomContainerManipulator.removeHydrogens(mol2);

		smsd = new Isomorphism(mol1, mol2, Algorithm.DEFAULT, true, true, true);
		smsd.setChemFilters(true, true, true);
		
		
		
		queryHL = new HashSet<>();
		for (int i = 0; i < smsd.getQuery().getAtomCount(); ++i) {
			if (!smsd.getFirstAtomMapping().getMappingsByIndex().keySet().contains(i)) {
				queryHL.add(i);
			}
		}
		targetHL = new HashSet<>();
		for (int i = 0; i < smsd.getTarget().getAtomCount(); ++i) {
			if (!smsd.getFirstAtomMapping().getMappingsByIndex().values().contains(i)) {
				targetHL.add(i);
			}
		}
		
	}
	public IAtomContainer rxnmol() {
		//return smsd.getQuery();
		return query;
	}

	public IAtomContainer prdmol() {
		//return smsd.getTarget();
		return target;
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
	
	public Isomorphism getSMSD() {
		return smsd;
	}
	public static void main(String[] args) throws Exception {

		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		IAtomContainer mol1 = sp.parseSmiles(args[0]);
		IAtomContainer mol2 = sp.parseSmiles(args[1]);

		SMSDpair mcsp = new SMSDpair(mol1, mol2);
		
		JPanel panel = new JPanel();
		StructureDisplay tdp1 = new StructureDisplay();//(mcsp.query);
		panel.add(tdp1);
		StructureDisplay tdp2 = new StructureDisplay(mcsp.target);
		panel.add(tdp2);
		StructureDisplay tdp3 = new StructureDisplay(mcsp.pairDiff()[0]);
		//tdp1.highlightSelect(mcsp.queryHi()); //keyset for query molecule
		panel.add(tdp3);
		StructureDisplay tdp4 = new StructureDisplay(mcsp.pairDiff()[1]);
		tdp2.highlightSelect(mcsp.targetHi()); //values for target molecule
		panel.add(tdp4);
		


		final JFrame f = new JFrame("Test");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(1500, 1200);
		f.getContentPane().add(panel);

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				f.pack();
				f.setVisible(true);
			}
		});

	}
}
