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
		GLUtils.glColor(gl, color);
		gl.glRasterPos2i(x, y);
		glut.glutBitmapString(GLUTEnum.GLUT_BITMAP_HELVETICA_12, text);
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
