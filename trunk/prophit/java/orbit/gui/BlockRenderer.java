package orbit.gui;

import gl4java.GLFunc;
import gl4java.GLEnum;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

class BlockRenderer
	implements CallLayoutAlgorithm.Callback, GLEnum
{
	public static final int RENDER_SOLID = 0;
	public static final int RENDER_WIREFRAME = 1;

	private static final double HEIGHT = 0.05;

	private static final double SIZE_THRESHOLD = 3.0;
	// If a function makes up more than this amount of time of the parent call, its coloring tends towards red
	// If less, it tends towards blue
	private static final double FRACTION_THRESHOLD = 0.30;

	private final GLFunc            gl;
	private final int               renderMode;
	private final int[]             viewport = new int[4];
	// TODO: can use Call keys as GL names
	private final HashMap nameToCallMap = new HashMap();

	private int nextName = 0;
	private double brightness = 1.3;
	private TimeMeasure measure = TimeMeasure.TotalTime;

	/**
	 * The BlockRenderer uses the model to understand how the diagram should be drawn.
	 * It can draw the diagram either as solid shaded quads, or as a wire frame.
	 *
	 * @param gl the interface to OpenGL
	 * @param renderMode one of RENDER_SOLID or RENDER_WIREFRAME
	 */
	public BlockRenderer(GLFunc gl, int renderMode)
	{
		this.gl = gl;
		this.renderMode = renderMode;

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
	public Map getNameToCallMap()
	{
		return nameToCallMap;
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
		if ( rectangle.width * viewport[2] < SIZE_THRESHOLD ||
			  rectangle.height * viewport[3] < SIZE_THRESHOLD )
		{
			return false;
		}

		/*
		if ( renderMode == RENDER_WIREFRAME )
		{
			double fractionOfRoot = call.getInclusiveTime(measure) / new CallAdapter(rootRenderCall).getInclusiveTime(measure);
			if ( fractionOfRoot < LINES_MINIMUM_TIME_FRACTION )
				return null;
		}
		*/

		// Used to shade the block according to whether it is a 'hotspot'. This shading can be made configurable
		//   in the future
		double expense = call.getExclusiveTime(measure) / call.getInclusiveTime(measure);

		// Neutral color is roughly 0.5, 0.5, 0.5 (though not really)
		// More than 50% of parent time skews towards Red
		// Less than 50% of parent time skews towards Blue
		// Overall, the colors are lightened to make them stand out from the black background.
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

		// green = 160;
		green = 0;
		red = blue = 128;
		red += adjustment;
		blue -= adjustment;

		// Brighten the colors a bit
		red = (int)( red * brightness );
		blue = (int)( blue * brightness );

		// Be sure that they are legal color values
		red = GLUtils.clamp(red);
		blue = GLUtils.clamp(blue);

		Color color = new Color(red, green, blue);

		if ( renderMode == RENDER_SOLID )
		{
			int name = nextName++;
			nameToCallMap.put(new Integer(name), call);
			gl.glPushName(name);

			renderAsQuads(call, rectangle, depth, color);

			gl.glPopName();
		}
		else if ( renderMode == RENDER_WIREFRAME )
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
		double bottomZ = depth * HEIGHT;
		double topZ = ( depth + 1 ) * HEIGHT;

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

		gl.glDisable(GL_DITHER);
		gl.glDisable(GL_LIGHTING);

		gl.glBegin(GL_LINE_LOOP);
			
		GLUtils.glColor(gl, Color.black);
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
		double bottomZ = depth * HEIGHT;
		double topZ = ( depth + 1 ) * HEIGHT;

		// Coordinate frame is OpenGL coordinates, with the X-axis heading East, the
		//   Y-axis heading North, and the Z-axis heading towards the observer
		gl.glBegin(GL_LINE_LOOP);
		GLUtils.glColor(gl, color);
		double z = depth * HEIGHT;
		gl.glVertex3d(rectangle.x, rectangle.y, topZ);
		gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, topZ);
		gl.glEnd();
	}
}
