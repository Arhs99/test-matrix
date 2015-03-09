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


public class SideDisplay extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1387483091072407189L;
	private StructureDisplay tdp1;
	private StructureDisplay tdp2;
	private JLabel label1 = new JLabel();
	private JLabel label2 = new JLabel();
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
		
		BorderLayout bl_tdp1 = new BorderLayout();
		bl_tdp1.setVgap(5);
		bl_tdp1.setHgap(5);
		tdp1.setLayout(bl_tdp1);
		this.add(tdp1);
		
		tdp1.add(label1, BorderLayout.PAGE_END);
		label1.setHorizontalAlignment(JLabel.CENTER);// setAlignmentY(CENTER_ALIGNMENT);
		tdp1.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.black),
                tdp1.getBorder()));
		this.add(Box.createVerticalGlue());
		
		BorderLayout bl_tdp2 = new BorderLayout();
		bl_tdp2.setVgap(5);
		bl_tdp2.setHgap(5);
		tdp2.setLayout(bl_tdp2);	
		this.add(tdp2);
		
		tdp2.add(label2, BorderLayout.PAGE_END);
		label2.setHorizontalAlignment(JLabel.CENTER);//setAlignmentX(CENTER_ALIGNMENT);
		tdp2.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.black),
                tdp2.getBorder()));
		

		this.add(Box.createRigidArea(new Dimension(300,300)));
		
		
		
		//this.validate();
		
		//this.setPreferredSize(new Dimension(300, 1000));
		
		
	}
	public void set(Molecule query, Molecule target, Collection<Integer> queryHi,
			Collection<Integer> targetHi) throws Exception {
		tdp1.setMol(query.getMol());
		tdp1.highlightSelect(queryHi);
		label1.setText(query.getFieldName() + ": " +
		query.getPotency().toString());
		
		
		
		tdp2.setMol(target.getMol());
		tdp2.highlightSelect(targetHi);
		label2.setText(target.getFieldName() + ": " +
		target.getPotency().toString());
		
		
		this.repaint();
		
	}
	
	public void clear() throws Exception {
		tdp1.clear();
		tdp2.clear();
		label1.setText("");
		label2.setText("");
		
		this.repaint();
	}
	

}
