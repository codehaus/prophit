package orbit.gui;

import orbit.model.Call;
import orbit.model.CallGraph;

import gl4java.GLContext;
import gl4java.awt.GLCanvas;
import gl4java.utils.glut.GLUTEnum;
import gl4java.utils.glut.GLUTFunc;
import gl4java.utils.glut.fonts.GLUTFuncLightImplWithFonts;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.event.*;
import java.lang.Math;
import java.util.Iterator;
import java.util.HashMap;

class MapCanvas extends GLCanvas
{
	private static double EXTENT = 1.0;
	private static double HEIGHT = 0.05;
	private static double MOVE_INCREMENT = 0.5;
	private static double DRAG_ROTATE_FACTOR = 5.0;
	// If a function makes up more than this amount of time of the parent call, its coloring tends towards red
	// If less, it tends towards blue
	private static double FRACTION_THRESHOLD = 0.20;
	private static double LINES_MINIMUM_TIME_FRACTION = 0.1;
	// Object smaller than this (in pixels) are not drawn
	private static double SIZE_THRESHOLD = 2.0;
	private static double SHIFT_STEP = 0.05;

	public static int DEFAULT_LEVELS = 5;

	private static EyeLocation DEFAULT_EYE_LOCATION = new EyeLocation(3.0, -Math.PI / 4, Math.PI / 4);
	
	private float Z_Off = -5.0f;

	float[] Light_Ambient =  { 0.75f, 0.75f, 0.75f, 1.0f };
	float[] Light_Diffuse =  { 1.2f, 1.2f, 1.2f, 1.0f }; 
	float[] Light_Position = { 5.0f, 5.0f, 5.0f, 1.0f };

	float bevel_mat_shininess[] = { 50.0f };
	float bevel_mat_specular[] = { 1.0f, 1.0f, 1.0f, 1.0f };

	private int[] selectBuffer = new int[512];
	
	private TimeMeasure measure = TimeMeasure.TotalTime;
	private int levels = DEFAULT_LEVELS;
	private double shiftUp = 0;
	private double shiftRight = 0;

	private int renderMode = GL_QUADS;

	private int names = 0;
	private HashMap nameToCallMap = new HashMap();
	private Call mouseOverCall = null;
	private Call selectedCall = null;

	private boolean generateLists = true;
	private int linesList = -1;
	private int quadsList = -1;

	private Point mouseClickPoint = null;
	private EyeLocation mouseClickLocation = null;

	private EyeLocation eyeLocation = DEFAULT_EYE_LOCATION;
	
	private GLUTFunc glut = null;
	private Call rootRenderCall;

	private final CallGraph cg;
	private final RectangleLayout rectangleLayout = new RectangleLayout(measure);

	public MapCanvas(int w, int h, CallGraph cg)
	{
		super(w, h, null, null);

		this.cg = cg;
		this.rootRenderCall = cg;
		
		addKeyListener(new KeyAdapter()
			{
				public void keyPressed(KeyEvent e)
				{
					// System.out.println(e);
					boolean repaint = false;
					switch ( e.getKeyChar() )
					{
					case '+':
						addLevel();
						repaint = true;
						break;
					case '-':
						removeLevel();
						repaint = true;
						break;
					case 'r':
						setRenderCall(MapCanvas.this.cg);
						repaint = true;
						break;
					case 'p':
						Call parent = MapCanvas.this.rootRenderCall.getParent();
						if ( parent != null )
						{
							setRenderCall(parent);
							repaint = true;
						}
						break;
					}
					switch ( e.getKeyCode() )
					{
					case KeyEvent.VK_UP:
						shiftUp += SHIFT_STEP;
						repaint = true;
						break;
					case KeyEvent.VK_DOWN:
						shiftUp -= SHIFT_STEP;
						repaint = true;
						break;
					case KeyEvent.VK_RIGHT:
						shiftRight += SHIFT_STEP;
						repaint = true;
						break;
					case KeyEvent.VK_LEFT:
						shiftRight -= SHIFT_STEP;
						repaint = true;
						break;
					}
					if ( repaint )
						repaint();
				}
			});
		
		addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					// System.out.println(e);
					if ( e.getModifiers() == MouseEvent.BUTTON1_MASK )
					{
						pick(e.getPoint());
						selectedCall = mouseOverCall;
						if ( selectedCall != null && e.getClickCount() == 2 )
						{
							setRenderCall(selectedCall);
							MapCanvas.this.repaint();
						}
					}
				}

				public void mousePressed(MouseEvent e)
				{
					// System.out.println(e);
					if ( e.getClickCount() == 1 && e.getModifiers() == MouseEvent.BUTTON1_MASK )
					{
						mouseClickPoint = e.getPoint();
						mouseClickLocation = (EyeLocation)eyeLocation.clone();
						renderMode = GL_LINES;
					}
				}

				public void mouseReleased(MouseEvent e)
				{
					renderMode = GL_QUADS;
					mouseClickPoint = null;
					MapCanvas.this.repaint();
				}
			});

		addMouseMotionListener(new MouseMotionAdapter()
			{
				public void mouseMoved(MouseEvent e)
				{
					pick(e.getPoint());
				}

				public void mouseDragged(MouseEvent e)
				{
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
						eyeLocation = eyeLocation.move(0, xFraction, yFraction);
						MapCanvas.this.repaint();
					}
				}
			});

	}

	public void setLevels(int levels)
	{
		this.levels = levels;
		clearCallLists();
		repaint();
	}
	
	public void addLevel()
	{
		++levels;
		clearCallLists();
	}

	public void removeLevel()
	{
		--levels;
		if ( levels < 1 )
			levels = 0;
		clearCallLists();
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
		
		gl.glTranslated(shiftRight, shiftUp, 0);
		glu.gluLookAt(eyeLocation.getX(), eyeLocation.getY(), eyeLocation.getZ(), 0, 0, 0, 0, 0, 1);
		gl.glTranslated(-0.5, -0.5, -0.5);

		if ( renderMode == GL_LINES )
		{
			gl.glDisable(GL_DITHER);
			gl.glDisable(GL_LIGHTING);
			gl.glDisable(GL_CULL_FACE);
			
			gl.glCallList(linesList);
		}
		else if ( renderMode == GL_QUADS )
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

		if ( mouseOverCall != null )
		{
			gl.glColor3d(1.0, 1.0, 1.0);
			gl.glRasterPos2i(6, 2);
			printString(GLUTEnum.GLUT_BITMAP_HELVETICA_12, mouseOverCall.getName());
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
		CallAdapter call = new CallAdapter(rootRenderCall);
		printString(GLUTEnum.GLUT_BITMAP_HELVETICA_12, "Root = " + call.getName() + ", time = " + call.getTime(measure) + ", " + ( call.getTime(measure) / cg.getTime() * 100.0 ) + "% of total");
		gl.glEnable(GL_LIGHTING);
		gl.glEnable(GL_DEPTH_TEST); 
		textEnd();
	}
	
	/*
	 * Find the Call which is rendered <code>screenPoint</code> and set the mouseOverCall
	 * variable to that Call (or to null).
	 */
	void pick(Point screenPoint)
	{
		/* Standard GL4Java Init */
		if( glj == null || !glj.gljMakeCurrent() ) 
		{
			return;
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

		int picks = gl.glRenderMode(GL_SELECT);
		gl.glDisable(GL_DITHER);

		gl.glInitNames();

		gl.glMatrixMode(GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		glu.gluPickMatrix(screenPoint.x, viewport[3] - screenPoint.y, 1, 1, viewport);
		glu.gluPerspective(45, 800 / 600.0, 0.1, 100);
		
		gl.glCallList(quadsList);
		
		int hits = gl.glRenderMode(GL_RENDER);
		// System.out.println("hits : " + hits);

		gl.glEnable(GL_DITHER);
		
		gl.glPopMatrix();
		gl.glMatrixMode(GL_MODELVIEW);

		glj.gljCheckGL();

		int closestName = -1;
		int closestZ = Integer.MAX_VALUE;
		for ( int count = 0, i = 0; count < hits; ++count )
		{
			int numNames = selectBuffer[i++];
			if ( numNames != 1 ) throw new RuntimeException("Expecting numNames = 1");
			int z1 = selectBuffer[i++];
			int z2 = selectBuffer[i++];
			int name = selectBuffer[i++];
			if ( z1 < closestZ )
			{
				closestZ = z2;
				closestName = name;
			}
		}
		
		if ( closestName != -1 )
		{
			mouseOverCall = (Call)nameToCallMap.get(new Integer(closestName));
			// System.out.println("mouseOverCall = " + mouseOverCall);
		}
		
		// drawName();
		// This is causing all that nasty flickering
		// glj.gljSwap();
		glj.gljFree();
	}
	
	synchronized void compileDisplayLists()
	{
		if ( generateLists )
		{
			generateLists = false;

			// TODO: can use Call keys as GL names
			nameToCallMap.clear();
			names = 0;

			if ( linesList == -1 )
			{
				linesList = gl.glGenLists(2);
				quadsList = linesList + 1;
			}
			else
			{
				gl.glDeleteLists(linesList, 2);
			}
			
			gl.glNewList(linesList, GL_COMPILE);

			renderMode = GL_LINES;
			
			// System.out.println("renderAsLines");
			
			render(new CallAdapter(rootRenderCall), new Rectangle2D.Double(0, 0, EXTENT, EXTENT),
				   new Rectangle2D.Double(0, 0, EXTENT, EXTENT), 0, Color.red);

			gl.glEndList();

			gl.glNewList(quadsList, GL_COMPILE);

			renderMode = GL_QUADS;

			// System.out.println("renderAsQuads");
			render(new CallAdapter(rootRenderCall), new Rectangle2D.Double(0, 0, EXTENT, EXTENT),
				   new Rectangle2D.Double(0, 0, EXTENT, EXTENT), 0, Color.red);

			gl.glEndList();
		}
	}
	
	Rectangle2D.Double render(CallAdapter call, Rectangle2D.Double parentRectangle, Rectangle2D.Double renderRectangle,
							  int depth, Color parentColor)
	{
		if ( depth > levels )
			return null;

		/*
		if ( renderMode == GL_LINES )
		{
			double fractionOfRoot = call.getTime(measure) / new CallAdapter(rootRenderCall).getTime(measure);
			if ( fractionOfRoot < LINES_MINIMUM_TIME_FRACTION )
				return null;
		}
		*/

		// double expense = call.getSelfFractionOfParentTime(measure);
		double expense = call.getTimeInSelf(measure) / call.getTime(measure);

		// Neutral color is 0.5, 0.5, 0.5
		// More than 50% of parent time skews towards Red
		// Less than 50% of parent time skews towards Blue
		int red, green, blue;
		int adjustment;
		double positiveScale = 1 - FRACTION_THRESHOLD;
		double negativeScale = FRACTION_THRESHOLD;
		if ( expense >= FRACTION_THRESHOLD )
		{
			adjustment = (int)( ( expense - FRACTION_THRESHOLD ) / positiveScale * 127.0 );
		}
		else
		{
			adjustment = -(int)( ( FRACTION_THRESHOLD - expense ) / negativeScale * 127.0 );
		}

		green = 160;
		red = blue = 128;
		red += adjustment;
		blue -= adjustment;

		// Brighten the colors a bit
		red = (int)( red * 1.3 );
		blue = (int)( blue * 1.3 );

		red = clamp(red);
		blue = clamp(blue);


		Color color;
		try 
		{
			color = new Color(red, green, blue);
		}
		catch (IllegalArgumentException x) 
		{
			System.out.println("call = " + call);
			for ( Iterator i = call.getChildren().iterator(); i.hasNext(); )
			{
				System.out.println("\t" + i.next());
			}
			throw x;
		}

		rectangleLayout.initialize(call, parentRectangle, renderRectangle);
		Rectangle2D.Double extent = rectangleLayout.getExtent();
		Rectangle2D.Double rectangle = rectangleLayout.getRectangle(extent);
		Rectangle2D.Double remainder = rectangleLayout.getRemainderExtent(extent);

		// Trim surfaces that take up negligible space
		if ( rectangle.width * getSize().width < SIZE_THRESHOLD ||
			 rectangle.height * getSize().height < SIZE_THRESHOLD )
		{
			return null;
		}
		
		if ( renderMode == GL_QUADS )
		{
			int name = names++;
			nameToCallMap.put(new Integer(name), call);
			gl.glPushName(name);
		}
		
		/*
		for ( int i = 0; i < depth; ++i )
			System.out.print("  ");
		System.out.println("Rendering " + call + " at " + extent);
		*/
		
		double bottomZ = depth * HEIGHT;
		double topZ = ( depth + 1 ) * HEIGHT;
		if ( renderMode == GL_LINES )
		{
			// Coordinate frame is OpenGL coordinates, with the X-axis heading East, the
			//   Y-axis heading North, and the Z-axis heading towards the observer
			gl.glBegin(GL_LINE_LOOP);
			glColor(color);
			double z = depth * HEIGHT;
			gl.glVertex3d(rectangle.x, rectangle.y, topZ);
			gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, topZ);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, topZ);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, topZ);
			gl.glEnd();
		}
		else if ( renderMode == GL_QUADS )
		{
			// Pushes the polygons back a little in the z-buffer so that the hi-lite lines
			//   will stand out
			gl.glEnable(GL_POLYGON_OFFSET_FILL);
			gl.glPolygonOffset(1.0f, 1.0f);

			// Top
			gl.glBegin(GL_QUADS);
			glColor(color);
			
			gl.glNormal3d(0, 0, 1);
			gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, topZ);
			gl.glVertex3d(rectangle.x, rectangle.y, topZ);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, topZ);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, topZ);
			gl.glEnd();

			gl.glBegin(GL_QUADS);
			// ( x, y ) to ( x, y + h )
			gl.glNormal3d(-1, 0, 0);
			// glColor(parentColor);
			gl.glVertex3d(rectangle.x, rectangle.y, bottomZ);
			// glColor(color);
			gl.glVertex3d(rectangle.x, rectangle.y, topZ);
			gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, topZ);
			// glColor(parentColor);
			gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, bottomZ);

			// ( x, y + h ) to ( x + w, y + h )
			gl.glNormal3d(0, 1, 0);
			// glColor(parentColor);
			gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, bottomZ);
			// glColor(color);
			gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, topZ);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, topZ);
			// glColor(parentColor);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, bottomZ);

			// ( x + w, y + h ) to ( x + w, y )
			gl.glNormal3d(1, 0, 0);
			// glColor(parentColor);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, bottomZ);
			// glColor(color);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, topZ);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, topZ);
			// glColor(parentColor);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, bottomZ);

			// ( x + w, y  ) to ( x, y )
			gl.glNormal3d(0, -1, 0);
			// glColor(parentColor);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, bottomZ);
			// glColor(color);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, topZ);
			gl.glVertex3d(rectangle.x, rectangle.y, topZ);
			// glColor(parentColor);
			gl.glVertex3d(rectangle.x, rectangle.y, bottomZ);
		
			gl.glEnd();

			gl.glDisable(GL_POLYGON_OFFSET_FILL);

			gl.glDisable(GL_DITHER);
			gl.glDisable(GL_LIGHTING);
			gl.glBegin(GL_LINE_LOOP);
			
			glColor(Color.black);
			gl.glVertex3d(rectangle.x, rectangle.y, topZ);
			gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, topZ);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, topZ);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, topZ);
			
			gl.glEnd();
			
			gl.glBegin(GL_LINES);
			gl.glVertex3d(rectangle.x, rectangle.y, topZ);
			gl.glVertex3d(rectangle.x, rectangle.y, bottomZ);
			gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, topZ);
			gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, bottomZ);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, topZ);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, bottomZ);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, topZ);
			gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, bottomZ);
			gl.glEnd();
			
			gl.glEnable(GL_LIGHTING);

			/*
			  Draws line edging around everything
			  This actually looks pretty bad b/c there are too many lines...
			gl.glBegin(GL_LINES);
			gl.glVertex3d(rectangle.x - offset, rectangle.y - offset, bottomZ);
			gl.glVertex3d(rectangle.x - offset, rectangle.y - offset, topZ);
			gl.glVertex3d(rectangle.x - offset, rectangle.y + rectangle.height + offset, bottomZ);
			gl.glVertex3d(rectangle.x - offset, rectangle.y + rectangle.height + offset, topZ);
			gl.glVertex3d(rectangle.x + rectangle.width + offset, rectangle.y + rectangle.height + offset, bottomZ);
			gl.glVertex3d(rectangle.x + rectangle.width + offset, rectangle.y + rectangle.height + offset, topZ);
			gl.glVertex3d(rectangle.x + rectangle.width + offset, rectangle.y - offset, bottomZ);
			gl.glVertex3d(rectangle.x + rectangle.width + offset, rectangle.y - offset, topZ);
			gl.glEnd();
			*/
			
			gl.glEnable(GL_DITHER);
		}
		else
		{
			System.err.println("Invalid renderMode : " + renderMode);
		}

		if ( renderMode == GL_QUADS )
		{
			gl.glPopName();
		}

		Rectangle2D.Double nextParentExtent = rectangle;
		Rectangle2D.Double nextRemainderExtent = nextParentExtent;
		for ( Iterator i = call.getChildren().iterator(); i.hasNext(); )
		{
			Call child = (Call)i.next();
			Rectangle2D.Double nextComputedExtent = render(new CallAdapter(child), nextParentExtent,
														   nextRemainderExtent, depth + 1, color);
			if ( nextComputedExtent != null )
				nextRemainderExtent = nextComputedExtent;
		}
		
		return remainder;
	}

	private int clamp(int color)
	{
		if ( color < 0 )
			return 0;
		else if ( color > 255 )
			return 255;
		else
			return color;
	}
	
	private void setRenderCall(Call call)
	{
		rootRenderCall = call;
		clearCallLists();
	}

	private void clearCallLists()
	{
		generateLists = true;
	}
	
	private void glColor(Color color)
	{
		gl.glColor3b((byte)( color.getRed() - 128 ), (byte)( color.getGreen() - 128 ), (byte)( color.getBlue() - 128 ));
	}
	
	void printString(int font, String str)
	{
		//for ( int i = 0; i < str.length(); i++ )
		//	glut.glutBitmapCharacter(font,str.charAt(i));
		glut.glutBitmapString(font, str);
	}

	
	private static class EyeLocation
	{
		public final double radius;
		public final double eyeYaw;
		public final double eyePitch;
				
		public EyeLocation(double radius, double eyeYaw, double eyePitch)
		{
			this.radius = radius;
			this.eyeYaw = eyeYaw;
			this.eyePitch = eyePitch;
		}

		public Object clone()
		{
			return new EyeLocation(radius, eyeYaw, eyePitch);
		}
		
		public EyeLocation move(double dR, double dYaw, double dPitch)
		{
			double pitch = eyePitch + dPitch;
			if ( pitch > Math.PI / 2 )
				pitch = Math.PI / 2;
			else if ( pitch < 0 )
				pitch = 0;
			
			return new EyeLocation(radius + dR, eyeYaw + dYaw, pitch);
		}

		public double getRadius() { return radius; }
		public double getPitch() { return eyePitch; }
		public double getYaw() { return eyeYaw; }

		public double getX()
		{
			return radius * Math.cos(eyeYaw) * Math.cos(eyePitch);
		}

		public double getY()
		{
			return radius * Math.sin(eyeYaw) * Math.cos(eyePitch);
		}

		public double getZ()
		{
			return radius * Math.sin(eyePitch);
		}

		public String toString()
		{
			return "r = " + radius + ", yaw = " + eyeYaw + ", pitch = " + eyePitch;
		}
	}
}
