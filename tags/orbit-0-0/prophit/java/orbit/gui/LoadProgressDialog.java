package orbit.gui;

import orbit.model.CallID;
import orbit.model.CallFractionSolver;
import orbit.model.CallFractionSolverData;
import orbit.model.CallGraph;
import orbit.parsers.DashProfParser;
import orbit.solver.SocketConnection;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collection;

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

		new LoadThread().start();
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

		cbParsed = new JCheckBox("Parsing file " + profileFile.getName());
		cbSolved = new JCheckBox("Reconstructing graph");
		lblError = new JLabel("           ");
		btnOK = new JButton("OK");
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

	private class LoadThread
		extends Thread
	{
		public void run()
		{
			try
			{
				long startTime = System.currentTimeMillis();
				System.out.println("Parsing " + profileFile);
			
				DashProfParser parser = new DashProfParser(new FileReader(profileFile));
				parser.execute();

				System.out.println("\tParsed in " + ( System.currentTimeMillis() - startTime ) + " ms");
			
				Collection callIDs = parser.getCallIDs();
				Collection proxyCallIDs = parser.getProxyCallIDs();
				CallID[] callIDArray = (CallID[])parser.getCallIDs().toArray(new CallID[0]);
				double[] fractions;
			
				cbParsed.setSelected(true);

				CallFractionSolverData data = new CallFractionSolverData(callIDs, proxyCallIDs);
				CallFractionSolver solver = new CallFractionSolver(data);
			
				File fractionsFile = new File(profileFile.getAbsolutePath() + ".graph");
				if ( !fractionsFile.exists() ||
					 fractionsFile.lastModified() < profileFile.lastModified() )
				{
					// System.setProperty("solver.user.name", "JAVA_USER");
					SocketConnection.Factory factory = new SocketConnection.Factory("neos.mcs.anl.gov", 3333);
					factory.setDebug(true, profileFile.getName());
					fractions = solver.execute(factory);

					FileWriter writer = new FileWriter(fractionsFile);
					solver.writeToFile(writer, fractions);
					writer.close();
				}
				else
				{
					fractions = solver.readFromFile(new FileReader(fractionsFile), callIDs.size());
				}
			
				cg = new CallGraph(callIDArray, fractions);
			
				cbSolved.setSelected(true);
			}
			catch (Exception x)
			{
				x.printStackTrace();
				lblError.setText("Error parsing file : " + x.getMessage());
				cg = null;
			}
			btnOK.setEnabled(true);
		}
	}		
}
