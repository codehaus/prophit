package orbit.gui;

import orbit.model.Call;
import orbit.model.CallGraph;
import orbit.util.Log;

import org.apache.log4j.Category;

import gl4java.awt.GLCanvas;
import gl4java.swing.GLJPanel;
import gl4java.utils.glut.GLUTEnum;
import gl4java.utils.glut.GLUTFunc;
import gl4java.utils.glut.fonts.GLUTFuncLightImplWithFonts;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.Math;
import java.util.Iterator;
import java.util.Map;

class BlockDiagramView
	// extends GLJPanel
	extends GLCanvas
{
	public static Category LOG = Category.getInstance(BlockDiagramView.class);

	private static double EXTENT = 1.0;

	private static double MOVE_INCREMENT = 0.5;
	private static double DRAG_ROTATE_FACTOR = 5.0;
	private static double LEGEND_SHIFT = 1 / 12.0;

	
	private static int TEXT_OFFSET_FROM_LEFT = 4;
	private static int FONT_HEIGHT = 14;
	private static int TEXT_BORDER = 3;
	private static int FONT_TOTAL_HEIGHT = FONT_HEIGHT + 2 * TEXT_BORDER;
	private static int LEGEND_BLOCK_HEIGHT = FONT_TOTAL_HEIGHT;
	private static int LEGEND_BLOCK_WIDTH = 20;
	
	private float Z_Off = -5.0f;

	// float[] Light_Ambient =  { 0.75f, 0.75f, 0.75f, 1.0f };
	// float[] Light_Diffuse =  { 1.2f, 1.2f, 1.2f, 1.0f }; 
	// float[] Light_Ambient1 =  { 0.75f, 0.75f, 0.75f, 1.0f };
	// float[] Light_Specular1 =  { 0.0f, 0.0f, 0.0f, 1.0f };
	float[] Light_Position1 = { 5.0f, 5.0f, 5.0f, 1.0f };

	// float[] Light_Ambient2 =  { 0.35f, 0.35f, 0.35f, 1.0f };
	float[] Light_Diffuse2 =  { 0.40f, 0.40f, 0.40f, 1.0f };
	float[] Light_Position2 = { -5.0f, -5.0f, 5.0f, 1.0f };
	
	float bevel_mat_shininess[] = { 50.0f };
	float bevel_mat_specular[] = { 1.0f, 1.0f, 1.0f, 1.0f };

	private final ColorModel  colorModel   = new BlendedColorModel();
	private final int[]       selectBuffer = new int[512];
	private final TimeMeasure measure      = TimeMeasure.TotalTime;

	/* All these variables are initialized in setModel */
	private int updateCount;
	private boolean repaint;
	private boolean generateDiagramLists;
	private boolean generateSelectedLists;
	private Point mouseClickPoint;
	private EyeLocation mouseClickLocation;
	private int renderMode;
	private BlockDiagramModel model;

	/* These variables keep their values across calls to setModel */
	private int linesList = -1;
	private int quadsList = -1;
	private int selectedCallsList = -1;
	private GLUTFunc glut = null;

	public BlockDiagramView(int w, int h, BlockDiagramModel blockModel)
	{
		super(w, h, null, null);

		System.out.println("Constructing with dimensions (" + w + ", " + h + ")");

		addListeners();

		setModel(blockModel);
	}

	public synchronized void setModel(BlockDiagramModel blockModel)
	{
		this.updateCount = 0;

		this.repaint = true;
		this.generateDiagramLists = true;
		this.generateSelectedLists = true;

		this.mouseClickPoint = null;
		this.mouseClickLocation = null;

		this.renderMode = BlockRenderer.RENDER_SOLID;

		this.model = blockModel;
		this.model.addListener(new PropertyChangeListener()
			{
				public void propertyChange(PropertyChangeEvent evt)
				{
					if ( BlockDiagramModel.RENDER_CALL_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.NUM_LEVELS_PROPERTY.equals(evt.getPropertyName()) )
					{
						generateDiagramLists = true;
						generateSelectedLists = true;
					}
					if ( BlockDiagramModel.SELECTED_CALL_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.NAME_SEARCH_STRING_PROPERTY.equals(evt.getPropertyName()) )
					{
						generateSelectedLists = true;
					}
					repaint = true;
					checkUpdate();
				}
			});

		repaint();
	}

	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();
		Log.debug(LOG, "preferredSize = ", d);
		return d;
	}
	
	public void removeNotify() 
	{
		super.removeNotify();

		/**
		 * Strictly speaking, it shouldn't be necessary to release references explicitly.
		 * However, there may be a problem with the native code retaining a reference to the BlockDiagramView.
		 * If that is the case, we should try and ensure that as much cleanup happens as possible.
		 */
		model = null;
		glut = null;
	}

	public void preInit() 
	{
	    doubleBuffer = true;
	    stereoView = false; 
	}

	public void init()
	{
		glut = new GLUTFuncLightImplWithFonts(gl, glu);

		// Color to clear color buffer to.
		GLUtils.glClearColor(gl, colorModel.getBackgroundColor());
		
		// Depth to clear depth buffer to; type of test.
		gl.glEnable(GL_CULL_FACE);
		gl.glCullFace(GL_BACK);
		gl.glEnable(GL_DEPTH_TEST);

		gl.glClearDepth(1.0);
		gl.glDepthFunc(GL_LESS); 

		// Enables Smooth Color Shading; try GL_FLAT for (lack of) fun.
		gl.glShadeModel(GL_FLAT);

		gl.glMaterialfv(GL_FRONT, GL_SHININESS, bevel_mat_shininess);
		gl.glMaterialfv(GL_FRONT, GL_SPECULAR, bevel_mat_specular);

		// gl.glColorMaterial(GL_FRONT_AND_BACK, GL_DIFFUSE);
		gl.glColorMaterial(GL_FRONT, GL_DIFFUSE);
		gl.glEnable(GL_COLOR_MATERIAL);
		
		// Set up lights, turn them on.
		// gl.glLightfv(GL_LIGHT0, GL_AMBIENT,  Light_Ambient1);
		Color lightColor = colorModel.getLightColor();
		/*
		gl.glLightfv(GL_LIGHT0,
					 GL_DIFFUSE,
					 new float[]{ lightColor.getRed() / 255.0f,
								  lightColor.getGreen() / 255.0f,
								  lightColor.getBlue() / 255.0f,
								  lightColor.getAlpha() / 255.0f  });
		*/
		gl.glLightfv(GL_LIGHT0,
					 GL_AMBIENT,
					 new float[]{ 0.7f, 0.7f, 0.7f, 1.0f });
		gl.glLightfv(GL_LIGHT0,
					 GL_DIFFUSE,
					 new float[]{ lightColor.getRed() / 255.0f,
								  lightColor.getGreen() / 255.0f,
								  lightColor.getBlue() / 255.0f,
								  lightColor.getAlpha() / 255.0f  });
		gl.glLightfv(GL_LIGHT0, GL_SPECULAR, new float[]{ 0.0f, 0.0f, 0.0f, 1.0f });
		gl.glLightfv(GL_LIGHT0, GL_POSITION, Light_Position1);

		// gl.glLightfv(GL_LIGHT1, GL_AMBIENT,  Light_Ambient2);
		gl.glLightfv(GL_LIGHT1, GL_DIFFUSE,  Light_Diffuse2);
		gl.glLightfv(GL_LIGHT1, GL_POSITION, Light_Position2);

		gl.glEnable(GL_LIGHT0); 
		// gl.glEnable(GL_LIGHT1); 
		gl.glEnable(GL_LIGHTING);
		
		reshape(getSize().width, getSize().height);
	}

	public void reshape(int width, int height)
	{
		super.reshape(width, height);

		gl.glMatrixMode(GL_PROJECTION);
		// gl.glOrtho(-EXTENT * 0.1, EXTENT * 1.1, -EXTENT * 0.1, EXTENT * 1.1, -1.0, 100.0);
		gl.glLoadIdentity();

		createPerspectiveTransform(width, height);
		
		gl.glMatrixMode(GL_MODELVIEW);
		glj.gljCheckGL();
	}

	public synchronized void display()
	{
		/* Standard GL4Java Init */
		if( !glj.gljMakeCurrent() ) 
		{
			return;
		}

		compileDisplayLists();
		
		// Clear the color and depth buffers.
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		gl.glMatrixMode(GL_MODELVIEW);
		gl.glLoadIdentity();

		// System.out.println(eyeLocation);

		gl.glTranslated(LEGEND_SHIFT, LEGEND_SHIFT, 0);
		gl.glTranslated(model.getShiftHorizontal(), model.getShiftVertical(), 0);
		glu.gluLookAt(model.getEye().getX(), model.getEye().getY(), model.getEye().getZ(), 
						  0, 0, 0, 0, 0, 1);
		gl.glTranslated(-0.5, -0.5, -0.5);

		if ( renderMode == BlockRenderer.RENDER_WIREFRAME )
		{
			gl.glDisable(GL_DITHER);
			gl.glDisable(GL_LIGHTING);
			gl.glDisable(GL_CULL_FACE);
			
			gl.glCallList(linesList);
		}
		else if ( renderMode == BlockRenderer.RENDER_SOLID )
		{			
			gl.glEnable(GL_DITHER);
			gl.glEnable(GL_LIGHTING);
			gl.glEnable(GL_CULL_FACE);
			
			gl.glCallList(quadsList);
		}

		gl.glCallList(selectedCallsList);

		drawDiagramRoot();
		drawMouseOverCall();
		drawLegend();
		
		// All done drawing.  Let's show it.
		glj.gljSwap();
		glj.gljCheckGL();
		glj.gljFree();
	}

	private void addListeners()
	{
		addKeyListener(new KeyAdapter()
			{
				public void keyPressed(KeyEvent e)
				{
					boolean processedChar = true;
					boolean processedCode = true;
					beginUpdate();
					try
					{
						// System.out.println(e);
						switch ( e.getKeyChar() )
						{
						case '+':
							model.addLevel();
							break;
						case '-':
							model.removeLevel();
							break;
						case 'r':
							model.getRootRenderState().setRenderCall(model.getCallGraph());
							break;
						case 'p':
							model.getRootRenderState().setRenderCallToParent();
							break;
						default:
							processedChar = false;
						}
						switch ( e.getKeyCode() )
						{
						case KeyEvent.VK_UP:	
							model.shiftVertical(true);
							break;
						case KeyEvent.VK_DOWN:
							model.shiftVertical(false);
							break;
						case KeyEvent.VK_RIGHT:
							model.shiftHorizontal(true);
							break;
						case KeyEvent.VK_LEFT:
							model.shiftHorizontal(false);
							break;
						default:
							processedCode = false;
						}
					}
					finally
					{
						endUpdate();
						if ( !processedCode && !processedChar )
						{
							propagateKeyEvent(e);
						}
					}
				}
			});
		
		addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					beginUpdate();
					try
					{
						// System.out.println(e);
						if ( e.getModifiers() == MouseEvent.BUTTON1_MASK )
						{
							Call selectedCall = pick(e.getPoint());
							Log.debug(LOG, "Mouse selected call ", selectedCall);
							model.setMouseOverCall(selectedCall);
							model.setSelectedCall(selectedCall);
							if ( selectedCall != null && e.getClickCount() == 2 )
							{
								model.getRootRenderState().setRenderCall(selectedCall);
							}
						}
					}
					finally
					{						
						endUpdate();
					}
				}

				public void mousePressed(MouseEvent e)
				{
					// System.out.println(e);
					if ( e.getClickCount() == 1 && e.getModifiers() == MouseEvent.BUTTON1_MASK )
					{
						mouseClickPoint = e.getPoint();
						mouseClickLocation = (EyeLocation)model.getEye().clone();
						renderMode = BlockRenderer.RENDER_WIREFRAME;
					}
				}

				public void mouseReleased(MouseEvent e)
				{
					renderMode = BlockRenderer.RENDER_SOLID;
					mouseClickPoint = null;
					BlockDiagramView.this.repaint();
				}
			});

		addMouseMotionListener(new MouseMotionAdapter()
			{
				public void mouseMoved(MouseEvent e)
				{
					try
					{
						beginUpdate();
						
						Call mouseOverCall = pick(e.getPoint());
						model.setMouseOverCall(mouseOverCall);
					}
					finally
					{
						endUpdate();
					}
				}

				public void mouseDragged(MouseEvent e)
				{
					beginUpdate();

					// System.out.println(e);
					if ( mouseClickPoint != null )
					{
						Point dragToPoint = e.getPoint();
						int dx = dragToPoint.x - mouseClickPoint.x;
						int dy = dragToPoint.y - mouseClickPoint.y;
						double width = getSize().getWidth();
						double height = getSize().getHeight();
						double xFraction = -dx / width / 2 / DRAG_ROTATE_FACTOR;
						double yFraction = dy / height / 2 / DRAG_ROTATE_FACTOR;
						model.moveEye(xFraction, yFraction);
					}

					endUpdate();
				}
			});
	}

	private void propagateKeyEvent(KeyEvent e)
	{
		getParent().dispatchEvent(e);
	}
	
	private void createPerspectiveTransform(int width, int height)
	{
		double aspectRatio = width / (double)height;
		glu.gluPerspective(45, aspectRatio, 0.1, 100);
	}

	private void textBegin()
	{
		int width = getSize().width;
		int height = getSize().height;

		gl.glMatrixMode(GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		glu.gluOrtho2D(0, width, 0, height);

		gl.glMatrixMode(GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		
		gl.glDisable(GL_LIGHTING);
		gl.glEnable(GL_DITHER);
	}

	private void textEnd()
	{
		gl.glPopMatrix();

		gl.glMatrixMode(GL_PROJECTION);
		gl.glPopMatrix();
		
		glj.gljCheckGL();
	}

	/*
	 * Draw 4 color bands with the header label 'Exclusive Time'
	 * At the bottom and top of the legend should be the inclusive time value
	 *   which corresponsd to the color
	 */
	void drawLegend()
	{
		int width = getSize().width;
		int height = getSize().height;

		textBegin();

		gl.glTranslatef(TEXT_OFFSET_FROM_LEFT, height - FONT_HEIGHT - TEXT_BORDER, 0);

		gl.glDisable(GL_LIGHTING);
		gl.glDisable(GL_DEPTH_TEST); 
		
		GLUtils.glColor(gl, colorModel.getTextColor());
		gl.glRasterPos2i(0, 2);
		printString(GLUTEnum.GLUT_BITMAP_HELVETICA_12, "Exclusive Time");
		
		gl.glTranslatef(0, -( FONT_HEIGHT + TEXT_BORDER), 0);
		gl.glRasterPos2i(0, 2);
		printString(GLUTEnum.GLUT_BITMAP_HELVETICA_12, "(% of Inclusive Time)");

		gl.glTranslatef(0, -( TEXT_BORDER * 2 ), 0);

		int numBlocks = 3;
		for ( int i = numBlocks; i >= 0; --i )
		{
			gl.glTranslatef(0, -LEGEND_BLOCK_HEIGHT, 0);

			// Scale value from FRACTION_THRESHOLD to 1
			double value = BlockRenderer.FRACTION_THRESHOLD + ( i * ( ( 1 - BlockRenderer.FRACTION_THRESHOLD ) / (double)numBlocks ) );
			GLUtils.glColor(gl, colorModel.getBlockColor(0, BlockRenderer.FRACTION_THRESHOLD, 1, value));
														 
			gl.glBegin(GL_QUADS);
			gl.glVertex2i(LEGEND_BLOCK_WIDTH, 0);
			gl.glVertex2i(LEGEND_BLOCK_WIDTH, LEGEND_BLOCK_HEIGHT);
			gl.glVertex2i(0, LEGEND_BLOCK_HEIGHT);
			gl.glVertex2i(0, 0);
			gl.glEnd();
			
			GLUtils.glColor(gl, colorModel.getTextColor());
			gl.glRasterPos2i(LEGEND_BLOCK_WIDTH + TEXT_BORDER, TEXT_BORDER * 2);
			printString(GLUTEnum.GLUT_BITMAP_HELVETICA_12, UIUtil.formatPercent(value));
		}
		
		gl.glEnable(GL_LIGHTING);
		gl.glEnable(GL_DEPTH_TEST); 
		
		textEnd();
	}

	void drawSearchedForCalls()
	{
		java.util.List callNames = model.getNameSearchNames();
		for ( Iterator i = callNames.iterator(); i.hasNext(); )
		{
			String name = (String)i.next();
			for ( Iterator j = model.getCallsByName(name).iterator(); j.hasNext(); )
			{
				Call call = (Call)j.next();
				drawSelected(call, colorModel.getSearchMatchColor());
			}
		}
	}

	void drawSelectedCalls()
	{
		Call selectedCall = model.getSelectedCall();
		Log.debug(LOG, "selectedCall is ", selectedCall);
		if ( selectedCall == null )
			return;

		if ( !drawSelected(selectedCall, colorModel.getSelectedCallColor()) )
		{
			model.setSelectedCall(null);
			return;
		}

		for ( Iterator i = model.getCallsByName(selectedCall.getName()).iterator(); i.hasNext(); )
		{
			Call call = (Call)i.next();
			if ( !call.equals(selectedCall) )
			{
				drawSelected(call, colorModel.getMatchingSelectedCallColor());
			}
		}
	}

	/**
	 * @return true if the selectedCall has a rectangle on the screen.
	 */
	boolean drawSelected(Call selectedCall, Color color)
	{
		ComputeCallLocation computeLocation = new ComputeCallLocation(selectedCall, model.getRootRenderState().getRenderCall());
		computeLocation.execute();

		Rectangle2D.Double rectangle = computeLocation.getRectangle();

		if ( rectangle == null )
		{
			Log.debug(LOG, "selectedCall ", selectedCall, " not found in diagram");
			return false;
		}
		
		int depth = computeLocation.getRenderDepth();
		double bottomZ = ( depth ) * BlockRenderer.HEIGHT;
		double midZ = ( depth + 0.5 ) * BlockRenderer.HEIGHT;
		double topZ = ( depth + 1.0 ) * BlockRenderer.HEIGHT;

		Log.debug(LOG, "Rendering selectedCall ", selectedCall, " at ", rectangle);

		gl.glDisable(GL_LIGHTING);
		
		GLUtils.glColor(gl, color);

		// Wrap the selected rectangle up like a christmas present
		gl.glBegin(GL_LINE_LOOP);
		gl.glVertex3d(rectangle.x, rectangle.y, midZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, midZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, midZ);
		gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, midZ);
		gl.glEnd();

		gl.glBegin(GL_LINE_LOOP);
		gl.glVertex3d(rectangle.x + rectangle.width / 2, rectangle.y, bottomZ);
		gl.glVertex3d(rectangle.x + rectangle.width / 2, rectangle.y, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width / 2, rectangle.y + rectangle.height, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width / 2, rectangle.y + rectangle.height, bottomZ);
		gl.glEnd();

		gl.glBegin(GL_LINE_LOOP);
		gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height / 2, bottomZ);
		gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height / 2, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height / 2, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height / 2, bottomZ);
		gl.glEnd();
		
		gl.glEnable(GL_LIGHTING);

		return true;
	}
	
	void drawMouseOverCall()
	{
		String text = "Mouse over  : ";
		if ( model.getMouseOverCall() != null )
		{
			text = text + model.getMouseOverCall().getName();
		}

		drawText(FONT_TOTAL_HEIGHT - TEXT_BORDER, text);
	}
	
	void drawDiagramRoot()
	{
		CallAdapter call = new CallAdapter(model.getRootRenderState().getRenderCall());
		String text = "Diagram root : " + UIUtil.getShortName(call.getName()) + " [ time = " +
			UIUtil.formatTime(call.getInclusiveTime(measure)) + ", " +
			UIUtil.formatPercent( call.getInclusiveTime(measure) / model.getCallGraph().getTime() ) + " of total ]";
		
		drawText(0, text);
	}

	private void drawText(int y, String text)
	{
		int width = getSize().width;
		int height = getSize().height;

		textBegin();

		gl.glTranslatef(0, y, 0);
		
		// Make sure we can read the FPS section by first placing a 
		// dark, opaque backdrop rectangle.
		GLUtils.glColor(gl, colorModel.getBackgroundColor() );

		gl.glDisable(GL_LIGHTING);
		gl.glDisable(GL_DEPTH_TEST); 
		
		gl.glBegin(GL_QUADS);
		gl.glVertex2i(width, 0);
		gl.glVertex2i(width, FONT_HEIGHT);
		gl.glVertex2i(0, FONT_HEIGHT);
		gl.glVertex2i(0, 0);
		gl.glEnd();

		GLUtils.glColor(gl, colorModel.getTextColor());
		gl.glRasterPos2i(TEXT_OFFSET_FROM_LEFT, TEXT_BORDER);
		
		printString(GLUTEnum.GLUT_BITMAP_HELVETICA_12, text);

		gl.glEnable(GL_LIGHTING);
		gl.glEnable(GL_DEPTH_TEST);
		
		textEnd();
	}

	/*
	 * Find the Call which is rendered <code>screenPoint</code> and set the mouseOverCall
	 * variable to that Call (or to null).
	 */
	synchronized Call pick(Point screenPoint)
	{
		/* Standard GL4Java Init */
		if( glj == null || !glj.gljMakeCurrent() ) 
		{
			return null;
		}

		compileDisplayLists();

		int[] viewport = new int[4];
		gl.glGetIntegerv(GL_VIEWPORT, viewport);

		/*
		System.out.print("Viewport = ( ");
		for ( int i = 0; i < viewport.length; ++i )
		{
			System.out.print(viewport[i]);
		}
		System.out.println(" )");
		*/
		
		gl.glSelectBuffer(selectBuffer.length, selectBuffer);

		gl.glRenderMode(GL_SELECT);

		gl.glInitNames();

		gl.glMatrixMode(GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		glu.gluPickMatrix(screenPoint.x, viewport[3] - screenPoint.y, 1, 1, viewport);

		createPerspectiveTransform(getSize().width, getSize().height);
		
		gl.glCallList(quadsList);
		
		int hits = gl.glRenderMode(GL_RENDER);
		// System.out.println("hits : " + hits);

		gl.glPopMatrix();
		gl.glMatrixMode(GL_MODELVIEW);

		glj.gljCheckGL();

		int closestName = -1;
		int closestZ = Integer.MAX_VALUE;
		for ( int count = 0, i = 0; count < hits; ++count )
		{
			int numNames = selectBuffer[i++];
			if ( numNames != 1 ) 
				System.out.println("Expecting numNames = 1, found " + numNames);
			int z1 = selectBuffer[i++];
			int z2 = selectBuffer[i++];
			int name = selectBuffer[i++];
			if ( z1 < closestZ )
			{
				closestZ = z2;
				closestName = name;
			}
		}

		Call mouseOverCall = null;
		if ( closestName != -1 )
		{
			mouseOverCall = model.getCallByGLName(closestName);
		}
		
		glj.gljFree();

		return mouseOverCall;
	}
	
	synchronized void compileDisplayLists()
	{
		if ( generateDiagramLists )
		{
			generateDiagramLists = false;

			if ( linesList == -1 )
			{
				linesList = gl.glGenLists(2);
				quadsList = linesList + 1;
			}
			else
			{
				gl.glDeleteLists(linesList, 2);
			}
			
			Rectangle2D.Double rootRectangle = new Rectangle2D.Double(0, 0, EXTENT, EXTENT);
			CallLayoutAlgorithm layout = new CallLayoutAlgorithm(new CallAdapter(model.getRootRenderState().getRenderCall()),
																				  measure,
																				  model.getLevels(),
																				  rootRectangle);
			
			BlockRenderer renderer;

			// Render as wire frame
			{
				renderer = new BlockRenderer(gl, BlockRenderer.RENDER_WIREFRAME, colorModel);
				layout.setCallback(renderer);
				
				gl.glNewList(linesList, GL_COMPILE);
				
				layout.execute();
			
				gl.glEndList();
			}

			// Render as solid
			{
				renderer = new BlockRenderer(gl, BlockRenderer.RENDER_SOLID, colorModel);
				layout.setCallback(renderer);
				
				gl.glNewList(quadsList, GL_COMPILE);
				
				layout.execute();
				
				gl.glEndList();

				model.setGLNameToCallMap(renderer.getGLNameToCallMap());
				model.setNameToCallListMap(renderer.getNameToCallListMap());
			}
		}

		if ( generateSelectedLists )
		{
			generateSelectedLists = false;
			
			if ( selectedCallsList == -1 )
			{
				selectedCallsList = gl.glGenLists(1);
			}
			else
			{
				gl.glDeleteLists(selectedCallsList, 1);
			}

			gl.glNewList(selectedCallsList, GL_COMPILE);
			
			drawSelectedCalls();
			drawSearchedForCalls();
			
			gl.glEndList();
		}
	}
	
	protected void beginUpdate()
	{
		++updateCount;
	}
	
	protected void endUpdate()
	{
		--updateCount;
		if ( updateCount < 0 )
			updateCount = 0;
		checkUpdate();
	}

	private void checkUpdate()
	{
		if ( updateCount == 0 )
		{
			if ( repaint )
			{
				repaint = false;
				repaint();
			}
		}
	}
	
	void printString(int font, String str)
	{
		//for ( int i = 0; i < str.length(); i++ )
		//	glut.glutBitmapCharacter(font,str.charAt(i));
		glut.glutBitmapString(font, str);
	}

	public interface ColorModel
	{
		public Color getLightColor();

		public Color getBackgroundColor();

		public Color getSearchMatchColor();

		public Color getTextColor();

		public Color getSelectedCallColor();

		public Color getMatchingSelectedCallColor();

		public Color getBlockColor(double minimumValue, double neutralValue, double maximumValue,
								   double actualValue);
		
		public Color getBlockBorderColor();
	}
}
