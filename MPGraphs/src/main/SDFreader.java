package main;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.interfaces.IChemSequence;
import org.openscience.cdk.io.iterator.IteratingSDFReader;


public class SDFreader {
	//private IChemSequence sequence;
	private HashSet<String> fields;
	private String[] fieldStr;
	private Map<IAtomContainer,String[]> sdfMap;
	public SDFreader(String file) throws Exception {
		FileReader in = new FileReader(file);
		IteratingSDFReader reader = new IteratingSDFReader(
				in, DefaultChemObjectBuilder.getInstance()
				);
		reader.setSkip(true);
		fields = new HashSet<String>();
		IAtomContainerSet molSet = DefaultChemObjectBuilder.getInstance().newInstance
				(IAtomContainerSet.class);
		while (reader.hasNext()) {
			IAtomContainer mol = reader.next();
			molSet.addAtomContainer(mol);
			Map<Object, Object> map = mol.getProperties();
			for (Object s : map.keySet()) {
				fields.add((String) s);
			}
		}

		sdfMap = new HashMap<IAtomContainer, String[]>();
		fieldStr = fields.toArray(new String[fields.size()]); 
		for (IAtomContainer mol : molSet.atomContainers()) {
			//System.out.println(mol.getProperties());
			IAtomContainer ac = new AtomContainer(mol);
			String[] val = new String[fields.size()];
			for (int j = 0; j < fieldStr.length; ++j) {
				val[j] = (String) mol.getProperty(fieldStr[j]);
			}

			sdfMap.put(ac, val);
		}
	}

//	public Set<IAtomContainer> set() {
//		int n = this.sequence.getChemModelCount();
//		HashSet<IAtomContainer> set = new HashSet<IAtomContainer>();
//		for (int i = 0; i < n; i++) {
//			IChemModel mod = this.sequence.getChemModel(i);
//			set.add(mod.getMoleculeSet().getAtomContainer(0));
//		}
//		return set;
//	}

	/**
	 * Returns a set of fields from the sdf file
	 * 
	 * @return
	 */
	public Set<String> fields() {	//change to private 
		return fields;
	}

	/**
	 * Returns a String array represantation of fields
	 * @return
	 */
	public String[] fieldStr() {	//change to private 
		return fieldStr;
	}
	
	/**
	 * Returns a map of key = IMolecule and value = a set of fields from the sdf in text form
	 * original order in sdf is not respected
	 * @return
	 */
	public Map<IAtomContainer, String[]> sdfMap() { //returns a map rep of sdf file
		return sdfMap;
	}


	/**
	 * @param args
	 * @throws CDKException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws Exception {
		String file = args[0];
		SDFreader sdf = new SDFreader(file);
				System.out.println("Size : " + sdf.sdfMap().size());
				String[] fieldsSet = sdf.fieldStr();
				for (String s : fieldsSet) {
					System.out.print(s + " | ");
				}
				System.out.println();
				for (IAtomContainer mol : sdf.sdfMap().keySet()) {
					System.out.print(mol.getAtomCount() + " | ");
					for (int i = 0; i < fieldsSet.length; ++i) {
						String val = sdf.sdfMap().get(mol)[i];
						if (val == null) {
							System.out.print("null | ");
						} else {
						System.out.print(val + " | ");
						}
					}
					System.out.println();
				}
	}

}
