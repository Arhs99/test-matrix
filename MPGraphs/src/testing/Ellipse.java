package testing;

public class Ellipse {

	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		// TODO Auto-generated method stub
		int max = 20;
		for (int i = 0; i < max + 1; ++i)  {
			System.out.print("\r " + i + " out of " + max);
			Thread.sleep(220);
		}
		System.out.println();
	}

}
