package orbit.gui.tower;

import orbit.gui.CallAdapter;
import orbit.gui.CallLayoutAlgorithm;
import orbit.gui.ColorModel;
import orbit.gui.Constants;
import orbit.gui.GLUtils;
import orbit.gui.TimeMeasure;
import orbit.model.Call;

import gl4java.GLFunc;
import gl4java.GLEnum;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The BlockRenderer uses the model to understand how the diagram should be drawn.
 * It can draw the diagram either as solid shaded quads, or as a wire frame.
 */
class BlockRenderer
	implements CallLayoutAlgorithm.Callback, GLEnum, Constants
{
	private final GLFunc                      gl;
	private final int                         renderMode;
	private final ColorModel                  colorModel;
	private final int[]                       viewport = new int[4];
	// TODO: can use Call keys as GL names
	private final HashMap glNameToCallMap = new HashMap();

	/** If empty, all calls should be rendered as wireframe in wireframe mode */
	private HashSet solidCalls = null;

	private int    nextName   = 0;
	private TimeMeasure measure = TimeMeasure.TotalTime;

	/**
	 * @param gl the interface to OpenGL
	 * @param renderMode one of RENDER_SOLID or RENDER_WIREFRAME
	 */
	public BlockRenderer(GLFunc gl, int renderMode, ColorModel colorModel)
	{
		this.gl = gl;
		this.renderMode = renderMode;
		this.colorModel = colorModel;

		gl.glGetIntegerv(GL_VIEWPORT, viewport);
		
		if ( renderMode != RENDER_SOLID && renderMode != RENDER_WIREFRAME )
			throw new IllegalArgumentException("Invalid renderMode : " + renderMode);
	}
	
	/**
	 * If the digram is rendered solidly, the BlockRenderer returns a map of OpenGL 'names' (integers)
	 * to {@link CallAdapter}s. This map can be used to map from a mouse selection back to a CallAdapter.
	 *
	 * @see #RENDER_SOLID
	 */
	public Map getGLNameToCallMap()
	{
		return glNameToCallMap;
	}

	/**
	 * Add a list of {@link Call}s which should always be rendered as solid.
	 */
	public void addSolidBlocks(List calls)
	{
		for ( Iterator i = calls.iterator(); i.hasNext(); )
		{
			Call call = (Call)i.next();
			addSolidBlock(call);
		}
	}
		
	/**
	 * Add a {@link Call} which should always be rendered as solid.
	 */
	public synchronized void addSolidBlock(Call call)
	{
		if ( solidCalls == null )
		{
			solidCalls = new HashSet();
		}
		solidCalls.add(call);
	}	
	
	/**
	 * Renders a {@link CallAdapter} at the specified depth (relative to the root rendered block, not the root
	 * of the call graph), inside the specified rectangle.
	 * 
	 * @return whether the child calls should be rendered. This can be false if the render depth is exceeded, or
	 * if the size of the block falls below a size threshold.
	 */
	public boolean beginCall(CallAdapter call, Rectangle2D.Double rectangle, int depth)
	{
		if ( rectangle.width * viewport[2] < MIN_BLOCK_SIZE_THRESHOLD ||
			  rectangle.height * viewport[3] < MIN_BLOCK_SIZE_THRESHOLD )
		{
			return false;
		}

		// Used to shade the block according to whether it is a 'hotspot'. This shading can be made configurable
		//   in the future
		double expense = call.getExclusiveTime(measure) / call.getInclusiveTime(measure);

		Color color = colorModel.getBlockColor(0, FRACTION_THRESHOLD, 1, expense);

		/** Render specifically solidified calls as solid even in WIREFRAME mode */
		if ( renderMode == RENDER_SOLID ||
			 ( solidCalls != null && solidCalls.contains(call.getCall()) ) )
		{
			int name = nextName++;
			glNameToCallMap.put(new Integer(name), call.getCall());
			gl.glPushName(name);

			renderAsQuads(call, rectangle, depth, color);

			gl.glPopName();
		}
		else // renderMode == RENDER_WIREFRAME
		{
			renderAsLines(call, rectangle, depth, color);
		}
		return true;
	}

	public void endCall(CallAdapter call)
	{
	}

	protected void renderAsQuads(CallAdapter call, Rectangle2D.Double rectangle, int depth, Color color)
	{
		double bottomZ = depth * BLOCK_HEIGHT;
		double topZ = ( depth + 1 ) * BLOCK_HEIGHT;

		// Pushes the polygons back a little in the z-buffer so that the hi-lite lines
		//   will stand out
		gl.glEnable(GL_POLYGON_OFFSET_FILL);
		gl.glPolygonOffset(1.0f, 1.0f);

		// Top
		gl.glBegin(GL_QUADS);
		GLUtils.glColor(gl, color);
			
		gl.glNormal3d(0, 0, 1);
		gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, topZ);
		gl.glVertex3d(rectangle.x, rectangle.y, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, topZ);
		gl.glEnd();

		gl.glBegin(GL_QUADS);
		// ( x, y ) to ( x, y + h )
		gl.glNormal3d(-1, 0, 0);
		// GLUtils.glColor(parentColor);
		gl.glVertex3d(rectangle.x, rectangle.y, bottomZ);
		// GLUtils.glColor(color);
		gl.glVertex3d(rectangle.x, rectangle.y, topZ);
		gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, topZ);
		// GLUtils.glColor(parentColor);
		gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, bottomZ);

			// ( x, y + h ) to ( x + w, y + h )
		gl.glNormal3d(0, 1, 0);
		// GLUtils.glColor(parentColor);
		gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, bottomZ);
		// GLUtils.glColor(color);
		gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, topZ);
		// GLUtils.glColor(parentColor);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, bottomZ);

			// ( x + w, y + h ) to ( x + w, y )
		gl.glNormal3d(1, 0, 0);
		// GLUtils.glColor(parentColor);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, bottomZ);
		// GLUtils.glColor(color);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, topZ);
		// GLUtils.glColor(parentColor);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, bottomZ);

		// ( x + w, y  ) to ( x, y )
		gl.glNormal3d(0, -1, 0);
		// GLUtils.glColor(parentColor);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, bottomZ);
		// GLUtils.glColor(color);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, topZ);
		gl.glVertex3d(rectangle.x, rectangle.y, topZ);
		// GLUtils.glColor(parentColor);
		gl.glVertex3d(rectangle.x, rectangle.y, bottomZ);
		
		gl.glEnd();

		gl.glDisable(GL_POLYGON_OFFSET_FILL);

		// Draw the hi-lite lines
		gl.glDisable(GL_DITHER);
		gl.glDisable(GL_LIGHTING);

		gl.glBegin(GL_LINE_LOOP);
			
		GLUtils.glColor(gl, colorModel.getBlockBorderColor());
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
		gl.glEnable(GL_DITHER);
	}

	protected void renderAsLines(CallAdapter call, Rectangle2D.Double rectangle, int depth, Color color)
	{
		double bottomZ = depth * BLOCK_HEIGHT;
		double topZ = ( depth + 1 ) * BLOCK_HEIGHT;

		// Coordinate frame is OpenGL coordinates, with the X-axis heading East, the
		//   Y-axis heading North, and the Z-axis heading towards the observer
		gl.glBegin(GL_LINE_LOOP);
		GLUtils.glColor(gl, color);
		double z = depth * BLOCK_HEIGHT;
		gl.glVertex3d(rectangle.x, rectangle.y, topZ);
		gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, topZ);
		gl.glEnd();
	}
}
