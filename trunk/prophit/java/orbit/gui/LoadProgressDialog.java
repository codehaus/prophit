package orbit.gui;

import orbit.model.CallID;
import orbit.model.CallGraph;
import orbit.parsers.Loader;
import orbit.parsers.LoaderFactory;
import orbit.util.Util;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

public class LoadProgressDialog
	extends JDialog
{
	private final File profileFile;
	
	private CallGraph cg = null;
	
	private JCheckBox cbParsed;
	private JCheckBox cbSolved;
	private JLabel    lblError;
	private JButton   btnOK;
	
	public LoadProgressDialog(Frame owner, File profileFile)
	{
		super(owner, "Load", true);

		this.profileFile = profileFile;
		
		addComponents();

		// new LoadThread().start();
	}

	public Dimension getPreferrefSize()
	{
		return new Dimension(200, 200);
	}
	
	public CallGraph getCallGraph()
	{
		return cg;
	}
	
	private void addComponents()
	{
		GridBagLayout layout = new GridBagLayout();
		getContentPane().setLayout(layout);

		cbParsed = new JCheckBox(Strings.getMessage(LoadProgressDialog.class, "parsingFile", profileFile.getName()));
		cbSolved = new JCheckBox(Strings.getUILabel(LoadProgressDialog.class, "reconstructingGraph"));
		lblError = new JLabel("           ");
		btnOK = new JButton(Strings.getUILabel(LoadProgressDialog.class, "OK"));
		btnOK.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					setVisible(false);
				}
			});

		cbParsed.setEnabled(false);
		cbSolved.setEnabled(false);
		btnOK.setEnabled(false);

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER; //end row

		layout.setConstraints(cbParsed, c);
		getContentPane().add(cbParsed);
		layout.setConstraints(cbSolved, c);
		getContentPane().add(cbSolved);
		layout.setConstraints(lblError, c);
		getContentPane().add(lblError);
		layout.setConstraints(btnOK, c);
		getContentPane().add(btnOK);
		
		getRootPane().setDefaultButton(btnOK);
		pack();
	}	
}
