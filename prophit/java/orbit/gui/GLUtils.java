package orbit.gui;

import gl4java.GLFunc;
import gl4java.utils.glut.GLUTEnum;
import gl4java.utils.glut.GLUTFunc;

import java.awt.Color;

public class GLUtils
{
	/**
	 * Assumes that the modelview and projection matrices have been set up already, probably to
	 * an orthogonal projection
	 */
	public static void drawText(GLFunc gl, GLUTFunc glut, int x, int y, String text, Color color)
	{
		drawText(gl, glut, x, y, text, color, false);
	}

	/** @param getWidth whether to compute and return the width of the bitmap string */
	public static int drawText(GLFunc gl, GLUTFunc glut, int x, int y, String text, Color color, boolean getWidth)
	{
		GLUtils.glColor(gl, color);
		gl.glRasterPos2i(x, y);
		glut.glutBitmapString(GLUTEnum.GLUT_BITMAP_HELVETICA_12, text);
		if ( getWidth )
			return glut.glutBitmapLength(GLUTEnum.GLUT_BITMAP_HELVETICA_12, text);
		else
			return 0;
	}

	public static void glColor(GLFunc gl, Color color)
	{
		gl.glColor4f(( color.getRed() / 255.0f ), ( color.getGreen() / 255.0f ),
					 ( color.getBlue() / 255.0f ), ( color.getAlpha() / 255.0f ));
	}

	public static void glClearColor(GLFunc gl, Color color)
	{
		gl.glClearColor(color.getRed() / 255.0f, color.getGreen() / 255.0f,
						color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
	}

	public static int clamp(int color)
	{
		if ( color < 0 )
			return 0;
		else if ( color > 255 )
			return 255;
		else
			return color;
	}
}
