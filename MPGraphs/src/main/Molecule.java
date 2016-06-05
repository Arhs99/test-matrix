/**
 * 
 */
package main;

import java.io.Serializable;
import java.util.ArrayList;
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
	private String molID = "";
	private int index = -1; 							// this is the index of the molecule in 
														//AdjMatrix.molArray or -1 if no pairing exists 
	private Map<Integer, Set<Integer>> atomMapping;		// key is query atom number,
														// value is set of target molecules as <Integer> indices of 
														// Adj.Matrix.molArray
	private String[] fieldStr;							// string array of available fields from sdf
	private String[] values;							// string array of values indexed as in fieldStr[] 
	
	
	public Molecule(IAtomContainer ac, double potency) throws CloneNotSupportedException, CDKException {
		StructureDiagramGenerator sdg = new StructureDiagramGenerator();
        sdg.setMolecule(ac.clone());
		sdg.generateCoordinates();
        this.ac = sdg.getMolecule();
        this.ac.setStereoElements(new ArrayList(0));
		this.potency = potency;
		atomMapping = new HashMap<Integer, Set<Integer>>();
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

	public Map<Integer, Set<Integer>> getAtomMapping() {
		return atomMapping;
	}


	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String[] getFieldStr() {
		return fieldStr;
	}

	public void setFieldStr(String[] fieldStr) {
		this.fieldStr = fieldStr;
	}

	public String[] getValues() {
		return values;
	}

	public void setValues(String[] values) {
		this.values = values;
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
