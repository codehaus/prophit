package orbit.gui;

import java.awt.Component;
import java.awt.Dimension;

class UIUtil
{
	public static void centerWindow(Component window)
    {
        // Center the window on the screen
		Dimension screenSize = window.getToolkit().getScreenSize();
		Dimension size = window.getSize();
		window.setLocation( (screenSize.width - size.width) / 2, (screenSize.height - size.height) / 2 );
    }
}
