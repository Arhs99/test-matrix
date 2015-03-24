package viewer;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JCheckBox;
import java.awt.GridLayout;
import javax.swing.JList;
import javax.swing.AbstractListModel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.BoxLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class SDFDialogue extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4856812747180003360L;
	private final JPanel contentPanel = new JPanel();
	private JList list;
	private JList list_1;
	private final String[] fieldStr;
	private JTextField textField;
	private int fieldInd = -1;
	private int idInd = 0;
	private double norm = 100;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			String[] val = new String[] {"1", "2", "asd", "bbb", "a", "1212", "ww",
					"1", "2", "asd", "bbb", "a", "1212", "ww", 
					"1", "2", "asd", "bbb", "a", "1212", "ww"};
			SDFDialogue dialog = new SDFDialogue(val);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void showDial() {
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.pack();
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		this.setVisible(true);
	}

	/**
	 * Create the dialog.
	 */
	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	public SDFDialogue(String[] fieldstr) {
		setTitle("Select fields");
		this.fieldStr = fieldstr;
		setBounds(100, 100, 449, 294);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new GridLayout(0, 3, 10, 10));
		{
			JScrollPane scrollPane = new JScrollPane();
			contentPanel.add(scrollPane);
			{
				list = new JList();
				list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				scrollPane.setViewportView(list);
				list.setModel(new AbstractListModel() {
					String[] values = fieldStr;
					public int getSize() {
						return values.length;
					}
					public Object getElementAt(int index) {
						return values[index];
					}
				});
			}
			{
				JLabel lblActivityField = new JLabel("Activity field");
				lblActivityField.setLabelFor(list);
				lblActivityField.setHorizontalAlignment(SwingConstants.CENTER);
				scrollPane.setColumnHeaderView(lblActivityField);
			}
		}
		{
			JScrollPane scrollPane = new JScrollPane();
			contentPanel.add(scrollPane);
			{
				list_1 = new JList();
				String[] str = new String[fieldStr.length + 1];
				str[0] = "Default (1,2,...)";
				for (int i = 0; i < fieldStr.length; ++i) {
					str[i + 1] = fieldStr[i];
				}
				final String values[] = str;
				list_1.setModel(new AbstractListModel() {					
					public int getSize() {
						return values.length;
					}
					public Object getElementAt(int index) {
						return values[index];
					}
				});
				list_1.setSelectedIndex(0);		//set selection to the default value
				scrollPane.setViewportView(list_1);
			}
			{
				JLabel lblIdField = new JLabel("ID field");
				lblIdField.setLabelFor(list_1);
				lblIdField.setHorizontalAlignment(SwingConstants.CENTER);
				scrollPane.setColumnHeaderView(lblIdField);
			}
		}
		{
			JPanel panel = new JPanel();
			contentPanel.add(panel);
			panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
			{
				JLabel lblPropertyThreshold = new JLabel("Threshold");
				panel.add(lblPropertyThreshold);
			}
			{
				textField = new JTextField();
				textField.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
					    textField.selectAll();
					}
				});
				textField.setActionCommand("");
				textField.setText("100");
				textField.setName("");
				panel.add(textField);
				textField.setColumns(5);
			}
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						norm = Double.parseDouble(textField.getText());						
						fieldInd = list.getSelectedIndex();
						idInd = list_1.getSelectedIndex();
						if (fieldInd !=  -1) {
							SDFDialogue.this.setVisible(false);
						}
						System.out.println(getFieldInd() + " " + idInd);
						System.out.println(getNorm());						
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						SDFDialogue.this.setVisible(false);
						System.out.println(getFieldInd() + " " + idInd);
						System.out.println(getNorm());
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	public int getFieldInd() {
		return fieldInd;
	}

	public int getIdInd() {
		return idInd;
	}

	public double getNorm() {
		return norm;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println(e.getActionCommand());
		
	}

}
