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
		if (args.length == 1) {
			SDFreader sdf = new SDFreader(args[0]);
			System.out.println("#Field\t|  ");
			for (int i = 0; i < sdf.fieldStr().length; ++i) {
				System.out.println((i + 1) + "\t|  " + sdf.fieldStr()[i]);
			}
			System.exit(0);
		}
		
		if (args.length < 3 || args.length > 4) {
			System.out.println("Usage : MPairs arg1 arg2 arg3 arg4\narg1 input.sdf\n" +
		        "arg2 output.sdf\n" + "arg3 number of field of interest in input.sdf\n" +
				"arg4 optional - minimum inactive value, default = 100\n\n  or \n" +
		        "MPairs arg1 for a list of fields of arg1 with Nos\n");
			System.exit(0);
		}
		String infile = args[0];
		String outfile = args[1];
		Integer ind = Integer.parseInt(args[2]) - 1;
		double norm = args.length == 3 ? 100 : Double.parseDouble(args[3]);

		SDFreader sdf = new SDFreader(infile);
		TreeSet<Molecule> map = new TreeSet<>();
		int cnt = 0;
		String mainProp = sdf.fieldStr()[ind];
		for (IAtomContainer mol : sdf.sdfMap().keySet()) {
			String val = sdf.sdfMap().get(mol)[ind]; 	// index of field is 0
			if (val == null || mol.getAtomCount() == 0) {
				continue;
			}
			ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
			mol = ExtAtomContainerManipulator.removeHydrogens(mol);
			ExtAtomContainerManipulator.aromatizeCDK(mol);

			Molecule molec = new Molecule(mol, Double.parseDouble(val), mainProp);
			map.add(molec);
			++cnt;
			//if (cnt == 20) break;
		}
		AdjMatrix adm = new AdjMatrix(map);
		IAtomContainerSet toWriteSet = DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class);
		for (int k = 0; k < adm.getCCSMSDMatr().length; ++k) {
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
						double dlnX = (DeltaP.logDiff(queryPot, targetPot, norm) == Double.MIN_VALUE ?
								0 : DeltaP.logDiff(queryPot, targetPot, norm));
						targetBit.setProperty("dlnX", dlnX);
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
