package orbit.gui.tower;

import orbit.gui.Constants;
import orbit.gui.GLUtils;
import orbit.gui.TimeMeasure;
import orbit.model.Call;
import orbit.util.Log;
import orbit.util.Util;

import org.apache.log4j.Category;

import gl4java.GLEnum;
import gl4java.GLUEnum;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * Abstract class for drawing blocks which are highlighted. This behavior is used for selected and
 * search-result blocks.
 */
public abstract class AbstractHighlightBlockComponent
	extends AbstractDisplayListComponent
{
	public static Category LOG = Category.getInstance(AbstractHighlightBlockComponent.class);

	private static double BORDER_WIDTH = 3.0;
	
	/**
	 * @return true if the selectedCall has a rectangle on the screen.
	 */
	protected boolean highlightBlock(Call call, Color color)
	{
		return highlightBlock(call, color, false);
	}

	/**
	 * @param secondary true if the call is a secondary result. For instance, a block that a user
	 * clicks on is the selected block. Blocks which are other instances of the same function call
	 * are secondary selected blocks.
	 */
	protected boolean highlightBlock(Call call, Color color, boolean secondary)
	{
		ComputeCallLocation computeLocation = new ComputeCallLocation(call, model.getRootRenderState().getRenderCall());
		computeLocation.execute();

		Rectangle2D.Double rectangle = computeLocation.getRectangle();

		if ( rectangle == null )
		{
			Log.debug(LOG, "call ", call, " not found in diagram");
			return false;
		}
		
		int depth = computeLocation.getRenderDepth();

		// drawDots(rectangle, depth, call, color);
		drawPackageWrapping(rectangle, depth, call, color, secondary);
		// drawBorder(rectangle, depth, call, color);

		return true;
	}
	
	/**
	 * Draw 3 dots: one on the top of the block, and one on each of the two faces which are closest
	 * to the viewer (closest Z-coordinate in window space)
	 */
	private void drawDots(Rectangle2D.Double rectangle, int depth, Call call, Color color)
	{
		Log.debug(LOG, "drawDots for call ", call, " at ", rectangle);

		// Top dot
		double topZ = ( depth + 1.0 ) * Constants.BLOCK_HEIGHT;
		double x = rectangle.x + rectangle.width / 2;
		double y = rectangle.y + rectangle.height / 2;

		double radius = Math.min(rectangle.width, rectangle.height) / 3;
		
		long quadric = glu.gluNewQuadric();

		GLUtils.glColor(gl, color);

		gl.glPushMatrix();
		
		gl.glTranslated(rectangle.x + rectangle.width / 2, rectangle.y + rectangle.height / 2, topZ);
		glu.gluQuadricDrawStyle(quadric, GLUEnum.GLU_FILL);
		glu.gluQuadricNormals(quadric, GLUEnum.GLU_SMOOTH);
		glu.gluDisk(quadric, 0, radius, 16, 8);
		
		glu.gluDeleteQuadric(quadric);

		gl.glPopMatrix();
	}

	/**
	 * Draws a thickened border around the top and nearest 2 sides of each block.
	 */
	private void drawBorder(Rectangle2D.Double rectangle, int depth, Call call, Color color)
	{
		double topZ = ( depth + 1.0 ) * Constants.BLOCK_HEIGHT;

		Log.debug(LOG, "drawBorder for call ", call, " at ", rectangle);

		/*
		 * Draw 4 quads, one along each edge of the top of the block
		 * The thickness of the quad is about 3 pixels, as determined by comparing the
		 *   scale of the diagram to the width of the screen
		 */

		// The width of the base of the diagram is always 1.0 unit
		double pixelWidth = Constants.SCREEN_EXTENT / canvas.getSize().width;
		double lineWidth = pixelWidth * BORDER_WIDTH;
				
		GLUtils.glColor(gl, color);

		// Draw the top border
		gl.glPushMatrix();
		gl.glTranslated(rectangle.x, rectangle.y, topZ);
		drawBorderXY(rectangle.width, rectangle.height, lineWidth);
		gl.glPopMatrix();

		// Determine whether the front or back of the block is closer to the viewer
		//   The front is y = 0, the back is y = 1

		double[] front = { 0, 0, 0 };
		double[] back  = { 0, 1, 0 };
		
		int[] viewport = new int[4];
		double[] mvMatrix = new double[16], projMatrix = new double[16], frontWinCoords = new double[3], backWinCoords = new double[3];
		gl.glGetIntegerv(GLEnum.GL_VIEWPORT, viewport);
		gl.glGetDoublev(GLEnum.GL_MODELVIEW_MATRIX, mvMatrix);
		gl.glGetDoublev(GLEnum.GL_PROJECTION_MATRIX, projMatrix);
		
		glu.gluProject(front, mvMatrix, projMatrix, viewport, frontWinCoords);
		glu.gluProject(back, mvMatrix, projMatrix, viewport, backWinCoords);

		System.out.println("Front : " + Util.asList(frontWinCoords));
		System.out.println("Back : " + Util.asList(backWinCoords));
		if ( frontWinCoords[2] <= backWinCoords[2] )
		{
			System.out.print("\tFront ");
		}
		else
		{
			System.out.print("\tBack ");
		}
		System.out.println("is closer");
		
		// Determine whether the left or right of the block is closer to the viewer
		//   The left is x = 0, the right is x = 1
	}

	private void drawBorderXY(double width, double height, double lineWidth)
	{
		gl.glBegin(GLEnum.GL_QUADS);

		// Top
		gl.glVertex2d(0, height - lineWidth);
		gl.glVertex2d(0, height);
		gl.glVertex2d(width, height);
		gl.glVertex2d(width, height - lineWidth);

		// Bottom
		gl.glVertex2d(0, 0);
		gl.glVertex2d(0, lineWidth);
		gl.glVertex2d(width, lineWidth);
		gl.glVertex2d(width, 0);

		// Left
		gl.glVertex2d(0, lineWidth);
		gl.glVertex2d(0, height - lineWidth);
		gl.glVertex2d(lineWidth, height - lineWidth);
		gl.glVertex2d(lineWidth, lineWidth);

		// Right
		gl.glVertex2d(width - lineWidth, lineWidth);
		gl.glVertex2d(width - lineWidth, height - lineWidth);
		gl.glVertex2d(width, height - lineWidth);
		gl.glVertex2d(width, lineWidth);
		
		gl.glEnd();
	}
	
	/**
	 * Draws lines around a block which resemble the wrapping on a present.
	 */
	private void drawPackageWrapping(Rectangle2D.Double rectangle, int depth, Call call, Color color, boolean secondary)
	{
		double bottomZ = ( depth ) * Constants.BLOCK_HEIGHT;
		double midZ = ( depth + 0.5 ) * Constants.BLOCK_HEIGHT;
		double topZ = ( depth + 1.0 ) * Constants.BLOCK_HEIGHT;

		Log.debug(LOG, "drawPackageWrapping for call ", call, " at ", rectangle);

		gl.glDisable(GLEnum.GL_DITHER);
		gl.glDisable(GLEnum.GL_LIGHTING);
		gl.glDisable(GLEnum.GL_CULL_FACE);
		
		GLUtils.glColor(gl, color);

		gl.glPushAttrib(GLEnum.GL_LINE_BIT);
		if ( !secondary )
		{
			gl.glLineWidth(3);
		}
		else
		{
			/*
			// See page 52 of the OpenGL Red Book
			short pattern = (short)0xAAAA;
			gl.glEnable(GLEnum.GL_LINE_STIPPLE);
			gl.glLineStipple(4, pattern);
			*/
			gl.glLineWidth(2);
		}
		
		// Wrap the selected rectangle up like a christmas present
		gl.glBegin(GLEnum.GL_LINE_LOOP);
		gl.glVertex3d(rectangle.x, rectangle.y, midZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y, midZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height, midZ);
		gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height, midZ);
		gl.glEnd();

		gl.glBegin(GLEnum.GL_LINE_LOOP);
		gl.glVertex3d(rectangle.x + rectangle.width / 2, rectangle.y, bottomZ);
		gl.glVertex3d(rectangle.x + rectangle.width / 2, rectangle.y, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width / 2, rectangle.y + rectangle.height, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width / 2, rectangle.y + rectangle.height, bottomZ);
		gl.glEnd();

		gl.glBegin(GLEnum.GL_LINE_LOOP);
		gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height / 2, bottomZ);
		gl.glVertex3d(rectangle.x, rectangle.y + rectangle.height / 2, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height / 2, topZ);
		gl.glVertex3d(rectangle.x + rectangle.width, rectangle.y + rectangle.height / 2, bottomZ);
		gl.glEnd();

		gl.glPopAttrib();
	}
}
