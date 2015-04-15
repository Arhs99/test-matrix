package main;

/**
 * @author kostasp
 * ordered pair of atom index, cluster index [0, inf)
 */
public class MolClustPair {
	int atomIndex;
	int clustIndex;
	public MolClustPair(int atomIndex, int clustIndex) {
		this.atomIndex = atomIndex;
		this.clustIndex = clustIndex;
	}
	public int atom() {
		return atomIndex;
	}
	
	public int clust() {
		return clustIndex;
	}

}
