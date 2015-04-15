/**
 * 
 */
package main;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.layout.StructureDiagramGenerator;

/**
 * @author kostasp
 *
 */
public class Molecule implements Serializable, Comparable<Molecule> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1230923932248065052L;
	private IAtomContainer ac;
	private Double potency;
	private String fieldName;
	private String molID;
	private Map<Integer, Set<MolClustPair>> atomMapping;		// key is query atom number,
	// value is a map of K = index of target molecule, V = set of pairs (atom index, clust idex)
	public Molecule(IAtomContainer ac, double potency) throws CloneNotSupportedException, CDKException {
		StructureDiagramGenerator sdg = new StructureDiagramGenerator();
        sdg.setMolecule(ac.clone());
		sdg.generateCoordinates();
        this.ac = sdg.getMolecule();
		this.potency = potency;
		atomMapping = new HashMap<Integer, Set<MolClustPair>>();
	}
	
	public Molecule(IAtomContainer ac, double potency, String field)
			throws CloneNotSupportedException,CDKException {
		this(ac, potency);
		this.fieldName = field;
	}
	
	public IAtomContainer getMol() {
		return ac;
	}
	public Double getPotency() {
		return potency;
	}
	
	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getMolID() {
		return molID;
	}

	public void setMolID(String molID) {
		this.molID = molID;
	}

	public Map<Integer, Set<MolClustPair>> getAtomMapping() {
		return atomMapping;
	}


	@Override
	public int compareTo(Molecule o) {
		if (this.ac.equals(o.ac)) {
		return 0;
		} else if (this.potency.equals(o.potency)) {
			return (this.ac.hashCode() - o.ac.hashCode());
		} else {
			return (- this.potency.compareTo(o.potency));
		}
	}
	
	public String toString() {
		return this.getMol().toString();
	}

}
