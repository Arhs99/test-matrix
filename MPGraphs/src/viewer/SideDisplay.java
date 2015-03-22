package viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import main.Molecule;
import java.awt.Font;


public class SideDisplay extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1387483091072407189L;
	private StructureDisplay tdp1;
	private StructureDisplay tdp2;
	private JLabel label1 = new JLabel();
	private JLabel label2 = new JLabel();
	private JLabel idLabel1 = new JLabel();
	private JLabel idLabel2 = new JLabel();
	public SideDisplay() throws Exception {
		super();
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		try {
			tdp1 = new StructureDisplay();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			tdp2 = new StructureDisplay();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.add(Box.createRigidArea(new Dimension(300,60)));
		
		JPanel top = new JPanel(new BorderLayout());
		top.setBackground(Color.WHITE);
		JPanel bot = new JPanel(new BorderLayout());
		bot.setBackground(Color.WHITE);
	
		top.add(tdp1, BorderLayout.CENTER);
		top.add(label1, BorderLayout.PAGE_END);
		idLabel1.setFont(new Font("Dialog", Font.BOLD, 14));
		top.add(idLabel1, BorderLayout.NORTH);
		idLabel1.setHorizontalAlignment(JLabel.CENTER);
		label1.setHorizontalAlignment(JLabel.CENTER);
		top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.black),
                tdp1.getBorder()));
		this.add(top);
		
		this.add(Box.createVerticalGlue());
		
		bot.add(tdp2, BorderLayout.CENTER);
		bot.add(label2, BorderLayout.PAGE_END);
		idLabel2.setFont(new Font("Dialog", Font.BOLD, 14));
		bot.add(idLabel2, BorderLayout.NORTH);
		idLabel2.setHorizontalAlignment(JLabel.CENTER);
		label2.setHorizontalAlignment(JLabel.CENTER);
		bot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.black),
                tdp2.getBorder()));
		this.add(bot);

		this.add(Box.createRigidArea(new Dimension(300,300)));
		
		
		
		this.validate();
	}
	
	public void set(Molecule query, Molecule target, Collection<Integer> queryHi,
			Collection<Integer> targetHi) throws Exception {
		tdp1.setMol(query.getMol());
		tdp1.highlightSelect(queryHi);
		idLabel1.setText(query.getMolID());
		label1.setText(query.getFieldName() + ": " +
		query.getPotency().toString());
		
		
		
		tdp2.setMol(target.getMol());
		tdp2.highlightSelect(targetHi);
		idLabel2.setText(target.getMolID());
		label2.setText(target.getFieldName() + ": " +
		target.getPotency().toString());
		
		
		this.repaint();
		
	}
	
	public void clear() throws Exception {
		tdp1.clear();
		tdp2.clear();
		label1.setText("");
		label2.setText("");
		idLabel1.setText("");
		idLabel2.setText("");
		
		this.repaint();
	}
	

}
