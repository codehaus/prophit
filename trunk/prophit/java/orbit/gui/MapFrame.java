package orbit.gui;

import orbit.parsers.DashProfParser;
import orbit.model.Call;
import orbit.model.CallID;
import orbit.model.CallGraph;

import gl4java.GLContext;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.applet.*;
import java.io.*;
import java.util.*;

public class MapFrame
	extends JFrame
{
	public static void main(String[] args) throws Exception
	{
		if ( args.length > 0 && "-help".equals(args[0]) )
		{
			System.err.println("Usage : java MapFrame [ <profile data file> ]");
			System.exit(1);
		}

		String profileFileName = null;
		if ( args.length > 0 )
		{
			profileFileName = args[0];
		}

		String gljLib = null;
		String glLib = null;
		String gluLib = null;
		
		if ( !GLContext.loadNativeLibraries(gljLib, glLib, gluLib) )
		{
			System.out.println("could not load native libs:"+
							   gljLib + ", " + glLib + ", " + gluLib);
		}
		else
		{
			System.out.println("loaded native libs:"+
							   gljLib + ", " + glLib + ", " + gluLib);
		}

		// Solves the lightweight/heavyweight mixing problem
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		
		MapFrame map = new MapFrame();

		if ( profileFileName != null )
		{
			if ( !map.loadProfile(new File(profileFileName)) )
			{
				System.err.println("Unable to load " + profileFileName);
				System.exit(1);
			}
		}
		
		map.setSize(800, 600);
		UIUtil.centerWindow(map);
		map.show();
	}

	private final Controller controller = new Controller();
	
	private MapCanvas cvsMap = null;
	private JSlider depthSlider;
	
	public MapFrame()
	{
		super(Strings.getUILabel(MapFrame.class, "title"));
		
		addMenus();
		addToolbar();
		
		setDefaultCloseOperation( EXIT_ON_CLOSE );
	}

	public boolean loadProfile(File profileFile)
	{
		LoadProgressDialog dlgLoad = new LoadProgressDialog(null, profileFile);
		UIUtil.centerWindow(dlgLoad);
		dlgLoad.setVisible(true);
		
		CallGraph cg = dlgLoad.getCallGraph();
		if ( cg == null )
		{
			System.err.println("Unable to load " + profileFile);
			return false;
		}
		else
		{
			if ( cvsMap != null )
			{
				cvsMap.cvsDispose();
				getContentPane().remove(cvsMap);
			}
			
			cvsMap = new MapCanvas(800, 600, cg);
			cvsMap.setLevels(depthSlider.getValue());
			getContentPane().add(cvsMap, BorderLayout.CENTER);
			cvsMap.requestFocus();
			pack();
			return true;
		}
	}
	
    private void addMenus()
    {
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar( menuBar );

		// Add File menu
		JMenu fileMenu = menuBar.add( new JMenu( Strings.getUILabel(MapFrame.class, "menu.file") ) );
		fileMenu.setMnemonic( KeyEvent.VK_F );
		
		addMenuItem(fileMenu, Strings.getUILabel(MapFrame.class, "menu.file.open"), KeyEvent.VK_O, KeyEvent.VK_O, new ActionListener()
			{
				public void actionPerformed( ActionEvent e ) { controller.doOpen(); }
			});
		addMenuItem(fileMenu, Strings.getUILabel(MapFrame.class, "menu.file.exit"), KeyEvent.VK_X, KeyEvent.VK_X, new ActionListener()
			{
				public void actionPerformed( ActionEvent e ) { controller.doExit(); }
			});
    }

	private void addToolbar()
	{
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);

		toolbar.add(createDepthSlider());
		
		getContentPane().add(toolbar, BorderLayout.NORTH);
	}

	private JSlider createDepthSlider()
	{
		// Create a new JSlider with min = 1, max = 15, value = MapCanvas.DEFAULT_LEVELS
		depthSlider = new JSlider(1, 15, MapCanvas.DEFAULT_LEVELS);
		depthSlider.setMajorTickSpacing(2);
		depthSlider.setMinorTickSpacing(1);
		depthSlider.setPaintTicks(true);
		depthSlider.setPaintLabels(true);
		depthSlider.setSnapToTicks(true);

		depthSlider.addChangeListener(new ChangeListener()
			{
				public void stateChanged(ChangeEvent e)
				{
					JSlider source = (JSlider)e.getSource();
					if ( cvsMap != null && !source.getValueIsAdjusting() )
					{
						cvsMap.setLevels(source.getValue());
					}
				}
			});
		return depthSlider;
	}

	private JMenuItem addMenuItem(JMenu menu, String name, int menuEvent,
								  int acceleratorEvent, ActionListener listener)
	{
		JMenuItem item = menu.add( new JMenuItem( name, menuEvent ) );
		item.addActionListener(listener);
		if ( acceleratorEvent != -1 )
		{
			item.setAccelerator(KeyStroke.getKeyStroke(acceleratorEvent, Event.CTRL_MASK));
		}
		return item;
	}

	private JFileChooser newFileChooser()
	{
		String startDirectory = System.getProperty("user.dir");
		JFileChooser chooser = new JFileChooser(startDirectory);
		chooser.setFileFilter(new javax.swing.filechooser.FileFilter()
			{
				public boolean accept(File f) { return
													f.isDirectory() ||
													f.getName().endsWith(".prof") ||
													f.getName().endsWith(".txt"); }
				public String getDescription() { return "Java profiles"; }
			});
		return chooser;
	}
	
	public class Controller
	{
		public void doOpen()
		{
			// Log.debug(LOG, "doOpen");
			if ( doCloseDocument() )
			{
				JFileChooser chooser = newFileChooser();
				int returnVal = chooser.showOpenDialog(MapFrame.this);
				if ( returnVal == JFileChooser.APPROVE_OPTION )
				{
					loadProfile(chooser.getSelectedFile());
				}
			}
		}

		public boolean doCloseDocument() 
		{ 
			// Log.debug(LOG, "doCloseDocument");
			boolean close = true;
			if ( cvsMap != null )
			{
				int option = JOptionPane.showConfirmDialog(null, 
														   Strings.getUILabel(MapFrame.class, "doClose.message"),
														   Strings.getUILabel(MapFrame.class, "doClose.title"),
														   JOptionPane.YES_NO_OPTION);
				close = (  option == JOptionPane.YES_OPTION );
			}
			return close; 
		}

		public void doExit()
		{
			if ( doCloseDocument() )
			{
				if ( cvsMap != null )
				{
					cvsMap.cvsDispose();
				}
				dispose();
				System.exit( 0 );
			}
		}
	}
}

