package orbit.gui;

import orbit.parsers.DashProfParser;
import orbit.model.Call;
import orbit.model.CallID;
import orbit.model.CallGraph;
import orbit.util.Log;

import org.apache.log4j.Category;

import gl4java.GLContext;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.event.*;
import java.applet.*;
import java.io.*;
import java.util.*;
import java.util.List;

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

		// GLContext debugging
		// GLContext.gljClassDebug = true;

		// Solves the lightweight/heavyweight mixing problem
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
		// Fix JavaWebStart bug
		// see http://developer.java.sun.com/developer/bugParade/bugs/4387437.html
		System.setSecurityManager(null);
		
		MapFrame map = new MapFrame();

		UIUtil.centerWindow(map);

		if ( profileFileName != null )
		{
			map.loadProfile(new File(profileFileName));
		}

		// map.pack();
		map.show();
	}

	public static Category LOG = Category.getInstance(MapFrame.class);

	// For some reason, without this fudge factor the diagram grows in width by 4
	//   pixels whenever a new profile is loaded
	private static final int DIAGRAM_WIDTH_FUDGE_FACTOR = 4;
	
	private final Controller controller = new Controller();
	
	private Action backAction;
	private Action forwardAction;
	private Action parentAction;
	private Action rootAction;

	private String workingDirectory = null;
	
	private BlockDiagramModel blockModel = null;

	private JToolBar         toolbar;
	private BlockDiagramView blockView = null;
	private JPanel           pnlContent;
	private CallDetailsView  callDetails;
	private JSlider          depthSlider;
	private JPanel           pnlStatus;
	private JLabel           lblStatus;
	
	public MapFrame()
	{
		super(Strings.getUILabel(MapFrame.class, "title"));

		createActions();
		addMenus();
		addToolbar();
		addComponents();

		enableControls();
		
		setSize(800, 600);
		setDefaultCloseOperation( EXIT_ON_CLOSE );
	}

	public void loadProfile(final File profileFile)
	{
		workingDirectory = profileFile.getParent();

		ProfileLoaderThread loader =
			new ProfileLoaderThread(profileFile,
									new ProfileLoaderThread.Callback()
										{
											public void parsed()
											{
												lblStatus.setText(Strings.getUILabel(MapFrame.class, "status.reconstructingGraph"));
											}
																	 
											public void solved()
											{
												lblStatus.setText(Strings.getMessage(MapFrame.class, "status.loaded", profileFile));
											}
																	 
											public void loadComplete(CallGraph cg, String errorText)
											{
												MapFrame.this.setCursor(Cursor.DEFAULT_CURSOR);
												if ( cg != null )
												{
													setCallGraph(cg);												
												}
												else
												{
													if ( errorText == null )
														errorText = "<unknown error>";
													String errorMsg = Strings.getMessage(MapFrame.class, "errorParsing.message",
																						 new Object[]{ profileFile.getName(),
																									   errorText });
													LOG.warn(errorMsg);
													lblStatus.setText(errorMsg);
													JOptionPane.showMessageDialog(MapFrame.this,
																				  errorMsg,
																				  Strings.getUILabel(MapFrame.class, "errorParsing.title"),
																				  JOptionPane.ERROR_MESSAGE);
												}
											}
										});
		lblStatus.setText(Strings.getMessage(MapFrame.class, "status.parsingFile",
											 profileFile.getName()));
		setCursor(Cursor.WAIT_CURSOR);
		loader.start();
	}

	private void setCallGraph(CallGraph cg)
	{
		Dimension contentSize = pnlContent.getSize();

		if ( blockView != null )
		{
			synchronized ( blockView )
			{
				blockView.cvsDispose();
				blockView = null;
			}
		}

		if ( blockModel != null )
		{
			blockModel.dispose();
			blockModel = null;
		}
			
		getContentPane().remove(pnlContent);
		
		pnlContent = null;
		System.gc();
			
		blockModel = new BlockDiagramModel(cg);
		blockModel.addListener(new PropertyChangeListener()
			{
				public void propertyChange(PropertyChangeEvent evt)
				{
					if ( BlockDiagramModel.RENDER_CALL_PROPERTY.equals(evt.getPropertyName()) )
					{
						enableControls();
						callDetails.selectedCallChanged(blockModel.getRootRenderState().getRenderCall(),
														blockModel.getSelectedCall());
					}
					else if ( BlockDiagramModel.SELECTED_CALL_PROPERTY.equals(evt.getPropertyName()) )
					{
						Call selectedCall = (Call)evt.getNewValue();
						callDetails.selectedCallChanged(blockModel.getRootRenderState().getRenderCall(),
														selectedCall);
					}
					else if ( BlockDiagramModel.NUM_LEVELS_PROPERTY.equals(evt.getPropertyName()) )
					{
						int levels = ((Integer)evt.getNewValue()).intValue();
						if ( levels > depthSlider.getMaximum() )
							depthSlider.setMaximum(depthSlider.getMaximum() + 5);
						depthSlider.setValue(levels);
					}
				}
			});
		blockModel.setLevels(depthSlider.getValue());

		int diagramWidth = contentSize.width - CallDetailsView.PREFERRED_WIDTH - DIAGRAM_WIDTH_FUDGE_FACTOR;
		int diagramHeight = contentSize.height;
		  
		Dimension diagramSize = new Dimension(diagramWidth, diagramHeight);
		
		System.out.println("size = " + diagramSize);
		blockView = new BlockDiagramView(diagramSize.width, diagramSize.height, blockModel);
		callDetails = new CallDetailsView()
			{
				public void callerSelected(String callerName)
				{
					selectSelectionCaller(callerName);
				}

				public void calleeSelected(String calleeName)
				{
					selectSelectionCallee(calleeName);
				}
			};

		pnlContent = new ContentPanel(blockView, callDetails);

		getContentPane().add(pnlContent, BorderLayout.CENTER);

		// Without this, the block diagram is rendered to the top-left of where it should
		//   be...
		pack();
		blockView.requestFocus();

		enableControls();
	}

	/**
	 * When a caller of the current displayed selection is selected (in the CallDetailsView),
	 * select the caller of the current selection if its name matches the selected parent. Otherwise,
	 * find the new name in the current block diagram.
	 */
	private void selectSelectionCaller(String name)
	{
		Call selectedCall = blockModel.getSelectedCall();
		if ( name == null || selectedCall == null )
			return;

		/*
		 * Add the caller of the selected call
		 * Then add the callers of all the other Calls in the diagram that have the same name
		 *   This will ensure that the selected call is preferred
		 *   Don't consider the caller of the root call
		 */
		List candidates = new ArrayList();
		Call root = blockModel.getRootRenderState().getRenderCall();
		addCaller(candidates, selectedCall);
		for ( Iterator i = blockModel.getCallsByName(selectedCall.getName()).iterator(); i.hasNext(); )
		{
			Call candidate = (Call)i.next();
			if ( candidate != root )
			{
				addCaller(candidates, candidate);
			}
		}
		selectCallByName(candidates, name);
	}

	/**
	 * When a callee of the current displayed selection is selected (in the CallDetailsView),
	 * select a callee of the current selection if there is one whose name matches the selected callee. Otherwise,
	 * find the new name in the current block diagram.
	 */
	private void selectSelectionCallee(String name)
	{
		Call selectedCall = blockModel.getSelectedCall();
		if ( name == null || selectedCall == null )
			return;

		// See comments for selectSelectionCaller
		List candidates = new ArrayList();
		addCallees(candidates, selectedCall);
		for ( Iterator i = blockModel.getCallsByName(selectedCall.getName()).iterator(); i.hasNext(); )
		{
			Call candidate = (Call)i.next();
			addCallees(candidates, candidate);
		}
		selectCallByName(candidates, name);
	}

	private void selectCallByName(List candidates, String name)
	{
		for ( Iterator i = candidates.iterator(); i.hasNext(); )
		{
			Call candidate = (Call)i.next();
			if ( name.equals(candidate.getName()) )
			{
				blockModel.setSelectedCall(candidate);
				return;
			}
		}
		LOG.info("Unable to find call '" + name + "' in diagram");
	}

	private void addCaller(List list, Call call)
	{
		Call parent = call.getParent();
		if ( parent != null )
			list.add(parent);
	}
	
	private void addCallees(List list, Call call)
	{
		for ( Iterator i = call.getChildren().iterator(); i.hasNext(); )
		{
			Call child = (Call)i.next();
			list.add(child);
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

		{
			// Add File menu
			JMenu fileMenu = menuBar.add( new JMenu( Strings.getUILabel(MapFrame.class, "menu.file") ) );
			fileMenu.setMnemonic( KeyEvent.VK_F );
		
			addMenuItem(fileMenu, Strings.getUILabel(MapFrame.class, "menu.file.open"), KeyEvent.VK_O, KeyEvent.VK_O, new ActionListener()
				{
					public void actionPerformed( ActionEvent e ) { controller.doOpen(); }
				});

			try
			{
				SampleProfiles samples = SampleProfiles.load("sampleProfiles.properties");

				JMenuItem mnuSamples = (JMenu)fileMenu.add(new JMenu(Strings.getUILabel(MapFrame.class, "menu.file.samples")));
				mnuSamples.setMnemonic(KeyEvent.VK_M);

				final HashMap profilesByMenuItem = new HashMap();
				for ( Iterator i = samples.getProfileTypes().iterator(); i.hasNext(); )
				{
					SampleProfileType type = (SampleProfileType)i.next();
					JMenu mnuSampleType = (JMenu)mnuSamples.add(new JMenu(type.getName()));
					for ( Iterator j = type.getProfiles().iterator(); j.hasNext(); )
					{
						SampleProfile profile = (SampleProfile)j.next();
						JMenuItem mnuProfile = (JMenuItem)mnuSampleType.add( new JMenuItem( profile.getName() ) );
						profilesByMenuItem.put(mnuProfile, profile);
						mnuProfile.addActionListener(new ActionListener()
							{
								public void actionPerformed( ActionEvent e ) 
								{
									JMenuItem source = (JMenuItem)e.getSource();
									SampleProfile profile = (SampleProfile)profilesByMenuItem.get(source);
									if ( profile != null )
									{
										Log.debug(LOG, "Profile ", profile.getName(), ".docURL = ", profile.getDocURL());
										if ( profile.getDocURL() != null && UIUtil.isShowDocumentPossible() )
										{
											int showDoc =
												JOptionPane.showConfirmDialog(null, 
																						Strings.getMessage(MapFrame.class, "showDocURL.message", profile.getDocURL()),
																						Strings.getUILabel(MapFrame.class, "showDocURL.title"),
																						JOptionPane.YES_NO_OPTION);
											if ( showDoc == JOptionPane.YES_OPTION )
											{
												if ( !UIUtil.showDocument(profile.getDocURL()) )
												{
													LOG.warn("Unable to show profile document");
												}
											}
										}
										else
										{
											LOG.debug("Browser not available");
										}
										loadProfile(profile.getFile());
									}
									else
									{
										LOG.error("No SampleProfile found for menu item " + source);
									}
								}
							});
					}
				}
			}
			catch (IOException x)
			{
				LOG.error("Unable to load sample profiles from sampleProfiles.properties");
			}

			fileMenu.addSeparator();

			addMenuItem(fileMenu, Strings.getUILabel(MapFrame.class, "menu.file.exit"), KeyEvent.VK_X, KeyEvent.VK_X, new ActionListener()
				{
					public void actionPerformed( ActionEvent e ) { controller.doExit(); }
				});
		}

		{
			// Add Help menu
			JMenu helpMenu = menuBar.add( new JMenu( Strings.getUILabel(MapFrame.class, "menu.help") ) );
			helpMenu.setMnemonic( KeyEvent.VK_H );
			
			addMenuItem(helpMenu, Strings.getUILabel(MapFrame.class, "menu.help.doc"), KeyEvent.VK_D, -1, new ActionListener()
				{
					public void actionPerformed( ActionEvent e ) 
					{ 
						UIUtil.showDocument(Strings.getUILabel(MapFrame.class, "menu.help.docURL"));
					}
				});
			addMenuItem(helpMenu, Strings.getUILabel(MapFrame.class, "menu.help.about"), KeyEvent.VK_A, -1, new ActionListener()
				{
					public void actionPerformed( ActionEvent e ) 
					{ 
						AboutBox dlgAbout = new AboutBox(MapFrame.this);
						UIUtil.centerWindow(MapFrame.this, dlgAbout);
						dlgAbout.setVisible(true);
					}
				});
		}
    }

	private void addToolbar()
	{
		toolbar = new JToolBar();
		toolbar.setFloatable(false);

		toolbar.add(backAction);
		toolbar.add(forwardAction);
		toolbar.add(parentAction);
		toolbar.add(rootAction);

		toolbar.addSeparator();

		toolbar.add(createDepthSlider());
		
		getContentPane().add(toolbar, BorderLayout.NORTH);
	}

	private void addComponents()
	{
		/*
		 * Add the 'Status' bar to the bottom of the application
		 */
		pnlStatus = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pnlStatus.add(new JLabel(Strings.getUILabel(MapFrame.class, "status.label")));
		lblStatus = new JLabel("              ");
		pnlStatus.add(lblStatus);

		pnlContent = new JPanel()
			{
				public Dimension getSize() { System.out.println(super.getSize()); return super.getSize(); }
			};
		getContentPane().add(pnlContent, BorderLayout.CENTER);
		
		getContentPane().add(pnlStatus, BorderLayout.SOUTH);
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
		if ( listener != null )
			item.addActionListener(listener);
		if ( acceleratorEvent != -1 )
			item.setAccelerator(KeyStroke.getKeyStroke(acceleratorEvent, Event.CTRL_MASK));
		return item;
	}

	private JFileChooser newFileChooser()
	{
		String startDirectory = workingDirectory;
		if ( startDirectory == null )
		{
			startDirectory = System.getProperty("user.dir");
		}

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
			rootAction.setEnabled(blockModel.getRootRenderState().getRenderCall().getParent() != null);
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
				if ( returnVal == JFileChooser.APPROVE_OPTION &&
					 chooser.getSelectedFile() != null &&
					 !chooser.getSelectedFile().isDirectory() )
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

	/*
	 * ContentPanel is a container for the entire content of the application, which uses
	 *   a GridBagLayout to assign horizontal space proportionally between the block diagram
	 *   and the call details
	 * pnlBlockDiagram is a panel which contains the current BlockDiagramView (blockView)
	 * callDetails is the CallDetailsView
	 * The blockDiagram is weighted 3:1 relative to the callDetails
	 */
	private static class ContentPanel
		extends JPanel
	{
		public ContentPanel(BlockDiagramView blockDiagram, CallDetailsView callDetails)
		{
			/*
			  This crap doesn't work
			  The blockDiagram is not re-sized to fill the available space
			  
			GridBagLayout gbl = new GridBagLayout();
			setLayout(gbl);

			GridBagConstraints gbc = new GridBagConstraints();
			
			// Add pnlBlockDiagram
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			gbl.setConstraints(blockDiagram, gbc);
			add(blockDiagram);
			
			// Add callDetails
			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(callDetails, gbc);
			add(callDetails);
			*/
			setLayout(new BorderLayout());

			add(blockDiagram, BorderLayout.CENTER);
			add(callDetails, BorderLayout.EAST);
		}
	}
}

