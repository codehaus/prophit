package orbit.gui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

/**
 * Displays the Orbit logo, version, and copyright statement.
 */
class AboutBox
	extends JDialog
{
	/*
	 * Layout is top-to-bottom:
	 * - Orbit logo
	 * - Version number
	 * - Copyright statement
	 * - homePage
	 */
	public AboutBox(Frame owner)
	{
		// is modal
		super(owner, Strings.getUILabel(AboutBox.class, "title"), true);

		setResizable(false);

		getContentPane().setLayout(new SGLayout(5, 1, SGLayout.CENTER, SGLayout.CENTER, SGLayout.FILL, SGLayout.FILL));
		
		getContentPane().add(new JLabel(Strings.getUILabel(AboutBox.class, "name")));
		getContentPane().add(new JLabel(Strings.getUILabel(AboutBox.class, "version")));
		getContentPane().add(new JLabel(Strings.getUILabel(AboutBox.class, "copyright")));
		getContentPane().add(new JLabel(Strings.getUILabel(AboutBox.class, "homePage")));
		JButton btnOK = (JButton)getContentPane().add(new JButton(Strings.getUILabel(AboutBox.class, "ok")));
		btnOK.addActionListener(new ActionListener()
			{
				public void actionPerformed( ActionEvent e ) 
				{
					hide();

				}
			});
		getRootPane().setDefaultButton(btnOK);

		addKeyListener(new KeyAdapter()
			{
				public void keyPressed(KeyEvent e)
				{
					if ( e.getKeyCode() == KeyEvent.VK_ESCAPE )
						hide();
				}
			});

		pack();
	}
}
