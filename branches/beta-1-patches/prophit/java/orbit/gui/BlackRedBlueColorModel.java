package orbit.gui;

import java.awt.Color;

class BlackRedBlueColorModel
	implements BlockDiagramView.ColorModel
{
	private double brightness = 1.3;

	public Color getLightColor()
	{
		return new Color(1.0f, 1.0f, 1.0f, 1.0f );
	}

	public Color getBackgroundColor()
	{
		return new Color(0.1f, 0.1f, 0.1f);
	}

	public Color getTextColor()
	{
		return Color.white;
	}

	public Color getSelectedCallColor()
	{
		return Color.white;
	}

	public Color getMatchingSelectedCallColor()
	{
		return Color.yellow;
	}

	public Color getSearchMatchColor()
	{
		return Color.green;
	}	

	public Color getBlockColor(double minimumValue, double neutralValue, double maximumValue,
							   double value)
	{
		// Neutral color is roughly 0.5, 0.5, 0.5 (though not really)
		// More than 50% of parent time skews towards Red
		// Less than 50% of parent time skews towards Blue
		// Overall, the colors are lightened to make them stand out from the black background.
		int red, green, blue;
		int adjustment;
		double positiveScale = maximumValue - neutralValue;
		double negativeScale = neutralValue - minimumValue;
		if ( value >= neutralValue )
		{
			adjustment = (int)( ( value - neutralValue ) / positiveScale * 127.0 );
		}
		else
		{
			adjustment = -(int)( ( neutralValue - value ) / negativeScale * 127.0 );
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
		
		return new Color(red, green, blue);
	}

	public Color getBlockBorderColor()
	{
		return Color.black;
	}
}
