package orbit.gui;

import java.awt.Color;

public interface ColorModel
{
	public Color getLightColor();

	public Color getBackgroundColor();

	public Color getSearchMatchColor();

	public Color getTextColor();

	public Color getSelectedCallColor();

	public Color getMatchingSelectedCallColor();

	public Color getBlockColor(double minimumValue, double neutralValue, double maximumValue,
							   double actualValue);
		
	public Color getBlockBorderColor();
}
