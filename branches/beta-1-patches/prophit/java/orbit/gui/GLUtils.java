package orbit.gui;

import gl4java.GLFunc;

import java.awt.Color;

public class GLUtils
{
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
