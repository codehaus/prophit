package orbit.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.text.NumberFormat;

class UIUtil
{
	private static NumberFormat TIME_FORMAT;
	private static NumberFormat TIME_PERCENT_FORMAT;

	static
	{
		TIME_FORMAT = NumberFormat.getInstance();
		TIME_FORMAT.setMaximumFractionDigits(2);
		TIME_PERCENT_FORMAT = NumberFormat.getPercentInstance();
		TIME_PERCENT_FORMAT.setMaximumFractionDigits(2);
	}

	public static String formatTime(double time)
	{
		return TIME_FORMAT.format(time);
	}

	public static String formatPercent(double percent)
	{
		return TIME_PERCENT_FORMAT.format(percent);
	}

	/**
	 * Format function call descriptions into a short format.
	 * <table>
	 * <tr><th>Input</th><th>Output</th></tr>
	 * <tr><td>some.package.Class.method</td><td>Class.method</td></tr>
	 * <tr><td>some.package.Class.method(Class.java)</td><td>Class.method(Class.java)</td></tr>
	 * <tr><td>some.package.Class.method(I)Ljava/lang/String;</td><td>Class.method</td></tr>
	 * </table>
	 */
	public static String getShortName(String name)
	{
		/*
		 * See if the method String contains the type signature, e.g.
		 * <code>(ILjava/lang/String;)</code>. The type signature is detected by looking
		 * at the contents of the last parenthesized region. If there is no occurance of
		 * a '.', a '&lt;', or a ':', then it is assumed to be a type signature.
		 */
		int lastParenCloseIndex = name.lastIndexOf(')');
		if ( lastParenCloseIndex != -1 )
		{
			int lastParenOpenIndex = name.lastIndexOf('(');
			String parenContents = name.substring(lastParenOpenIndex, lastParenCloseIndex);
			if ( parenContents.indexOf('.') == -1 &&
				 parenContents.indexOf('<') == -1 &&
				 parenContents.indexOf(':') == -1 )
			{
				name = name.substring(0, lastParenOpenIndex);
			}
		}
		
		int parens = 0;
		int dotCount = 0;
		StringBuffer sb = new StringBuffer();
		for ( int i = name.length() - 1; i >= 0; --i )
		{
			char c = name.charAt(i);
			if ( c == ')' )
				++parens;
			else if ( c == '(' )
				--parens;
			
			if ( parens == 0 && ( c == '.' || c == '\\' || c == '/' ) )
				++dotCount;
			// The second dot encountered will be the dot after the last token in the package name
			if ( dotCount == 2 )
				break;
			sb.append(c);
		}
		return sb.reverse().toString();
	}

	public static void centerWindow(Component window)
    {
        // Center the window on the screen
		Dimension screenSize = window.getToolkit().getScreenSize();
		Dimension size = window.getSize();
		window.setLocation( (screenSize.width - size.width) / 2, (screenSize.height - size.height) / 2 );
    }
}
