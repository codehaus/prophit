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
	 * Given a String that looks like 'some.package.Class(Class.java)' return
	 * 'Class(Class.java)'.
	 */
	public static String getShortName(String name)
	{
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
