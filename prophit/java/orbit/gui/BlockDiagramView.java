package orbit.gui;

import orbit.model.Call;
import orbit.model.CallGraph;

import gl4java.awt.GLCanvas;
import gl4java.utils.glut.GLUTEnum;
import gl4java.utils.glut.GLUTFunc;
import gl4java.utils.glut.fonts.GLUTFuncLightImplWithFonts;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.event.*;
import java.lang.Math;
import java.util.Iterator;
import java.util.Map;

class BlockDiagramView
	extends GLCanvas
	implements BlockDiagramModel.Listener
{
	private static double EXTENT = 1.0;

	private static double MOVE_INCREMENT = 0.5;
	private static double DRAG_ROTATE_FACTOR = 5.0;

	private float Z_Off = -5.0f;

	float[] Light_Ambient =  { 0.75f, 0.75f, 0.75f, 1.0f };
	float[] Light_Diffuse =  { 1.2f, 1.2f, 1.2f, 1.0f }; 
	float[] Light_Position = { 5.0f, 5.0f, 5.0f, 1.0f };

	float bevel_mat_shininess[] = { 50.0f };
	float bevel_mat_specular[] = { 1.0f, 1.0f, 1.0f, 1.0f };

	private int[] selectBuffer = new int[512];
	
	private TimeMeasure measure = TimeMeasure.TotalTime;

	private Map nameToCallMap = null;

	private int updateCount = 0;

	private boolean repaint = false;
	private boolean generateLists = true;

	private int linesList = -1;
	private int quadsList = -1;

	private Point mouseClickPoint = null;
	private EyeLocation mouseClickLocation = null;

	private GLUTFunc glut = null;

	private int   renderMode = BlockRenderer.RENDER_SOLID;
	private final BlockDiagramModel model;

	public BlockDiagramView(int w, int h, BlockDiagramModel blockModel)
	{
		super(w, h, null, null);

		this.model = blockModel;
		model.addListener(this);
		
		addKeyListener(new KeyAdapter()
			{
				public void keyPressed(KeyEvent e)
				{
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
							model.setRenderCall(model.getCallGraph());
							break;
						case 'p':
							Call parent = model.getRenderCall().getParent();
							if ( parent != null )
							{
								model.setRenderCall(parent);
							}
							break;
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
						}
					}
					finally
					{
						endUpdate();
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
							model.setMouseOverCall(selectedCall);
							model.setSelectedCall(selectedCall);
							if ( selectedCall != null && e.getClickCount() == 2 )
							{
								model.setRenderCall(selectedCall);
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

	public void preInit() 
	{
	    doubleBuffer = true;
	    stereoView = false; 
	}

	public void init()
	{
	    glut = new GLUTFuncLightImplWithFonts(gl, glu);

		// Color to clear color buffer to.
		gl.glClearColor(0.1f, 0.1f, 0.1f, 0.0f);
		
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

		gl.glColorMaterial(GL_FRONT_AND_BACK, GL_DIFFUSE);
		gl.glEnable(GL_COLOR_MATERIAL);
		
		// Set up a light, turn it on.
		gl.glLightfv(GL_LIGHT0, GL_AMBIENT,  Light_Ambient);
		gl.glLightfv(GL_LIGHT0, GL_DIFFUSE,  Light_Diffuse);
		gl.glLightfv(GL_LIGHT0, GL_POSITION, Light_Position);
		gl.glEnable(GL_LIGHT0); 
		gl.glEnable(GL_LIGHTING);
		
		// A handy trick -- have surface material mirror the color.
		// gl.glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
		// gl.glEnable(GL_COLOR_MATERIAL);

		gl.glMatrixMode(GL_PROJECTION);
		// gl.glOrtho(-EXTENT * 0.1, EXTENT * 1.1, -EXTENT * 0.1, EXTENT * 1.1, -1.0, 100.0);
		glu.gluPerspective(45, 800 / 600.0, 0.1, 100);
		gl.glMatrixMode(GL_MODELVIEW);
		glj.gljCheckGL();
	}

	public void display()
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

		drawStats();
		drawName();
		
		// All done drawing.  Let's show it.
		glj.gljSwap();
		glj.gljCheckGL();
		glj.gljFree();
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
	
	void drawName()
	{
		int width = getSize().width;
		int height = getSize().height;

		textBegin();

		gl.glTranslatef(0, height - 14, 0);

		// Make sure we can read the FPS section by first placing a 
		// dark, opaque backdrop rectangle.
		gl.glColor3d( 0, 0, 0);

		gl.glDisable(GL_LIGHTING);
		gl.glDisable(GL_DEPTH_TEST); 
		
		gl.glBegin(GL_QUADS);
		gl.glVertex2i(width, -2);
		gl.glVertex2i(width, 14);
		gl.glVertex2i(0, 14);
		gl.glVertex2i(0, -2);
		gl.glEnd();

		if ( model.getMouseOverCall() != null )
		{
			gl.glColor3d(1.0, 1.0, 1.0);
			gl.glRasterPos2i(6, 2);
			printString(GLUTEnum.GLUT_BITMAP_HELVETICA_12, model.getMouseOverCall().getName());
		}

		gl.glEnable(GL_LIGHTING);
		gl.glEnable(GL_DEPTH_TEST); 
		
		textEnd();
	}
	
	void drawStats()
	{
		int width = getSize().width;
		int height = getSize().height;

		textBegin();

		// Make sure we can read the FPS section by first placing a 
		// dark, opaque backdrop rectangle.
		gl.glColor3d( 0, 0, 0);
			
		gl.glBegin(GL_QUADS);
		gl.glVertex2i(width, 0);
		gl.glVertex2i(width, 16);
		gl.glVertex2i(0, 16);
		gl.glVertex2i(0, 0);
		gl.glEnd();

		gl.glDisable(GL_LIGHTING);
		gl.glDisable(GL_DEPTH_TEST); 
		gl.glColor3d(1.0, 1.0, 1.0);
		gl.glRasterPos2i(6, 2);
		CallAdapter call = new CallAdapter(model.getRenderCall());
		printString(GLUTEnum.GLUT_BITMAP_HELVETICA_12, "Root = " + call.getName() + ", time = " + call.getInclusiveTime(measure) + ", " + ( call.getInclusiveTime(measure) / model.getCallGraph().getTime() * 100.0 ) + "% of total");
		gl.glEnable(GL_LIGHTING);
		gl.glEnable(GL_DEPTH_TEST); 
		textEnd();
	}
	
	/*
	 * Find the Call which is rendered <code>screenPoint</code> and set the mouseOverCall
	 * variable to that Call (or to null).
	 */
	Call pick(Point screenPoint)
	{
		// FIXME:
		/*
		if ( updateCount != -11 )
			return null;
		*/

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
		glu.gluPerspective(45, 800 / 600.0, 0.1, 100);
		
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
			if ( nameToCallMap != null )
				mouseOverCall = (Call)nameToCallMap.get(new Integer(closestName));
			else
				System.out.println("No nameToCallMap in pick()");
		}
		
		glj.gljFree();

		return mouseOverCall;
	}
	
	synchronized void compileDisplayLists()
	{
		if ( generateLists )
		{
			generateLists = false;

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
			CallLayoutAlgorithm layout = new CallLayoutAlgorithm(new CallAdapter(model.getRenderCall()),
																				  measure,
																				  model.getLevels(),
																				  rootRectangle);
			
			BlockRenderer renderer;

			// Render as wire frame
			{
				renderer = new BlockRenderer(gl, BlockRenderer.RENDER_WIREFRAME);
				layout.setCallback(renderer);
				
				gl.glNewList(linesList, GL_COMPILE);
				
				layout.execute();
			
				gl.glEndList();
			}

			// Render as solid
			{
				renderer = new BlockRenderer(gl, BlockRenderer.RENDER_SOLID);
				layout.setCallback(renderer);
				
				gl.glNewList(quadsList, GL_COMPILE);
				
				layout.execute();
				
				gl.glEndList();

				nameToCallMap = renderer.getNameToCallMap();
			}
		}
	}
	
	public void modelInvalidated(BlockDiagramModel model)
	{
		generateLists = true;
		repaint = true;

		checkUpdate();
	}

	public void requestRepaint(BlockDiagramModel model)
	{
		repaint = true;

		checkUpdate();
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
}
