/**
 * 
 */
package main;

import java.io.Serializable;

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
	public Molecule(IAtomContainer ac, double potency) throws CloneNotSupportedException, CDKException {
		StructureDiagramGenerator sdg = new StructureDiagramGenerator();
        sdg.setMolecule(ac.clone());
		sdg.generateCoordinates();
        this.ac = sdg.getMolecule();
		this.potency = potency;
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
