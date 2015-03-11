package testing;

import java.io.FileOutputStream;
import java.util.Collection;
import java.util.TreeSet;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;

import cern.colt.matrix.ObjectMatrix2D;

import main.AdjMatrix;
import main.DeltaP;
import main.Molecule;
import main.SDFreader;
import main.SMSDpair;

public class TestPairs {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		double norm = 100;		// this is the min inactive value - can be added via an command argument
		String infile = args[0];
		String outfile = args[1];
		Integer ind = Integer.parseInt(args[2]) - 1;

		SDFreader sdf = new SDFreader(infile);
		TreeSet<Molecule> map = new TreeSet<>();
		int cnt = 0;
		String mainProp = sdf.fieldStr()[ind];
		for (IAtomContainer mol : sdf.sdfMap().keySet()) {
			String val = sdf.sdfMap().get(mol)[0]; 	// index of field is 0
			if (val == null || mol.getAtomCount() == 0) {
				continue;
			}
			ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
			mol = ExtAtomContainerManipulator.removeHydrogens(mol);
			ExtAtomContainerManipulator.aromatizeCDK(mol);

			Molecule molec = new Molecule(mol, Double.parseDouble(val), mainProp);
			map.add(molec);
			++cnt;
			if (cnt == 20) break;
		}
		AdjMatrix adm = new AdjMatrix(map);
		IAtomContainerSet toWriteSet = DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class);
		for (int k = 0; k < adm.getCCSMSDMatr().length; ++k) { //ObjectMatrix2D matr : adm.getCCSMSDMatr()) {
			if (adm.molVector()[k].length > 1) {
				for (int i = 0; i < adm.molVector()[k].length; ++i) {
					for (int j = i + 1; j < adm.molVector()[k].length; ++j) {
						if (adm.getCCSMSDMatr()[k].get(i, j) == null) {
							continue;
						}						
						SMSDpair pair = (SMSDpair) adm.getCCSMSDMatr()[k].get(i, j); //matr[i][j];
						IAtomContainer query = adm.molVector()[k][i].getMol();
						double queryPot = adm.molVector()[k][i].getPotency();
						query.setProperty(mainProp, queryPot);
						query.setProperty("Cluster No", k + 1);
						IAtomContainer target = adm.molVector()[k][j].getMol();
						double targetPot = adm.molVector()[k][j].getPotency();
						target.setProperty(mainProp, targetPot);
						target.setProperty("Cluster No", k + 1);
						IAtomContainer queryBit;
						IAtomContainer targetBit;
						queryBit = pair.pairDiff()[0];
						targetBit = pair.pairDiff()[1];
						targetBit.setProperty("dlnX", DeltaP.logDiff(queryPot, queryPot, norm));
						queryBit.setProperty("Cluster No", k + 1);
						targetBit.setProperty("Cluster No", k + 1);
						toWriteSet.addAtomContainer(query);
						toWriteSet.addAtomContainer(target);
						toWriteSet.addAtomContainer(queryBit);
						toWriteSet.addAtomContainer(targetBit);
					}
				}				
			}
		}
		SDFWriter sdfWriter = new SDFWriter(new FileOutputStream(outfile));
		sdfWriter.write(toWriteSet);
        sdfWriter.close();
	}

}
