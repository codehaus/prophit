package orbit.gui;

import gl4java.GLFunc;

import java.awt.Color;

public class GLUtils
{
	public static void glColor(GLFunc gl, Color color)
	{
		gl.glColor3b((byte)( color.getRed() - 128 ), (byte)( color.getGreen() - 128 ), (byte)( color.getBlue() - 128 ));
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
