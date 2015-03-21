package main;

public class Start {
	
	public static void showGUI() {
		
	}
	
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				showGUI();
			}
	});
}
}
