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
	
	private BlockDiagramView blockView = null;
	private BlockDiagramModel blockModel = null;

	private Action backAction;
	private Action forwardAction;
	private Action parentAction;
	private Action rootAction;
	
	private JSlider depthSlider;
	
	public MapFrame()
	{
		super(Strings.getUILabel(MapFrame.class, "title"));

		createActions();
		addMenus();
		addToolbar();

		enableControls();
		
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
			if ( blockView != null )
			{
				blockView.cvsDispose();
				getContentPane().remove(blockView);
				blockModel.dispose();
				blockModel = null;
				System.gc();
			}
			
			blockModel = new BlockDiagramModel(cg);
			blockModel.addListener(new BlockDiagramModel.Listener()
				{
					public void modelInvalidated(BlockDiagramModel model)
					{
						enableControls();
					}
					
					public void requestRepaint(BlockDiagramModel model)
					{
					}
				});
			blockModel.setLevels(depthSlider.getValue());

			blockView = new BlockDiagramView(800, 600, blockModel);
			getContentPane().add(blockView, BorderLayout.CENTER);
			blockView.requestFocus();
			pack();

			enableControls();
			
			return true;
		}
	}
	
    private void createActions()
	{
		backAction = new AbstractAction("Back") // , new ImageIcon("images/rewind.gif"))
			{
				public void actionPerformed(ActionEvent e)
				{
					blockModel.getRootRenderState().previousRenderCall();
				}
			};
		forwardAction = new AbstractAction("Forward") // , new ImageIcon("images/rewind.gif"))
			{
				public void actionPerformed(ActionEvent e)
				{
					blockModel.getRootRenderState().nextRenderCall();
				}
			};
		parentAction = new AbstractAction("Parent") // , new ImageIcon("images/rewind.gif"))
			{
				public void actionPerformed(ActionEvent e)
				{
					blockModel.getRootRenderState().setRenderCallToParent();
				}
			};
		rootAction = new AbstractAction("Root") // , new ImageIcon("images/rewind.gif"))
			{
				public void actionPerformed(ActionEvent e)
				{
					blockModel.getRootRenderState().setRenderCall(blockModel.getCallGraph());
				}
			};
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

		toolbar.add(backAction);
		toolbar.add(forwardAction);
		toolbar.add(parentAction);
		toolbar.add(rootAction);

		toolbar.addSeparator();

		toolbar.add(createDepthSlider());
		
		getContentPane().add(toolbar, BorderLayout.NORTH);
	}

	private JSlider createDepthSlider()
	{
		depthSlider = new JSlider(1, 15, BlockDiagramModel.DEFAULT_LEVELS);
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
					if ( blockModel != null && !source.getValueIsAdjusting() )
					{
						blockModel.setLevels(source.getValue());
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

	protected void enableControls()
	{
		if ( blockModel == null )
		{
			backAction.setEnabled(false);
			forwardAction.setEnabled(false);
			parentAction.setEnabled(false);
			rootAction.setEnabled(false);
		}
		else
		{
			backAction.setEnabled(blockModel.getRootRenderState().hasPreviousRenderCall());
			forwardAction.setEnabled(blockModel.getRootRenderState().hasNextRenderCall());
			parentAction.setEnabled(blockModel.getRootRenderState().hasParentRenderCall());
			rootAction.setEnabled(true);
		}
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
			if ( blockView != null )
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
				if ( blockView != null )
				{
					blockView.cvsDispose();
				}
				dispose();
				System.exit( 0 );
			}
		}
	}
}

