package orbit.gui;

import orbit.util.Log;

import org.apache.log4j.Category;

import java.awt.Color;

class BlendedColorModel
	implements BlockDiagramView.ColorModel
{
	public static Category LOG = Category.getInstance(BlendedColorModel.class);

	/*
	public  static final Color background = color("DDDDDD");
	private static final Color text = color("000000");
	private static final Color primaryBlock = color("D8CD88");
	private static final Color blockShade1 = color("D0C271");
	private static final Color blockShade2 = color("CBB445");
	private static final Color blockShade3 = color("917E28");
	private static final Color blockEdge = color("804000");
	private static final Color selectedBlock = color("3366FF");
	private static final Color secondarySelectedBlock = color("9999FF");
	*/

	/*
	private static final Color background = color("DDDDDD");
	private static final Color text = color("333333");
	private static final Color searchMatchColor = color("999933");
	private static final Color primaryBlock = color("E7C592");
	private static final Color blockShade1 = color("E1B777");
	private static final Color blockShade2 = color("E4AF61");
	private static final Color blockShade3 = color("DFA042");
	private static final Color blockEdge = color("004284");
	private static final Color selectedBlock = color("FFFFFF");
	// private static final Color secondarySelectedBlock = color("005CB9");
	private static final Color secondarySelectedBlock = color("66FF99");
	*/

	private static final Color background = color("DDDDDD");
	private static final Color text = color("333333");
	private static final Color searchMatchColor = color("999933");
	private static final Color primaryBlock = color("F1D092");
	// private static final Color blockShade1 = color("EDC274");
	// private static final Color blockShade2 = color("EBBB63");
	// private static final Color blockShade3 = color("E6AB3E");
	private static final Color blockShade1 = color("EBBB63");
	private static final Color blockShade2 = color("E39F50");
	private static final Color blockShade3 = color("E07F40");
	private static final Color blockEdge = color("F2ECCE");
	private static final Color selectedBlock = color("004284");
	private static final Color secondarySelectedBlock = color("006AD5");
	
	private static final Color[] shadeColors = { primaryBlock, blockShade1, blockShade2, blockShade3 };
	
	public Color getLightColor()
	{
		return primaryBlock;
	}

	public Color getBackgroundColor()
	{
		return background;
	}

	public Color getTextColor()
	{
		return text;
	}

	public Color getSearchMatchColor()
	{
		return searchMatchColor;
	}	

	public Color getSelectedCallColor()
	{
		return selectedBlock;
	}
	
	public Color getMatchingSelectedCallColor()
	{
		return secondarySelectedBlock;
	}

	public Color getBlockBorderColor()
	{
		return blockEdge;
	}

	public Color getBlockColor(double minimumValue, double neutralValue, double maximumValue,
							   double value)
	{
		if ( value < neutralValue )
		{
			return primaryBlock;
		}
		else
		{
			/*
			 * range = difference between max value and neutral value
			 * delta = difference between value and neutral value
			 * bucketDistance = range / numBuckets
			 * choose bucket as 
			 */
			double range = maximumValue - neutralValue;
			double delta = value - neutralValue;
			int numBuckets = shadeColors.length - 1;
			double bucketSeparation = range / numBuckets;
			int bucket = (int)( delta / range * numBuckets );
			if ( bucket == numBuckets )
				bucket = numBuckets - 1;
			Log.debug(LOG, "bucket [", new Double(value), "] =  ", new Integer(bucket));
			Color startColor = shadeColors[bucket];
			Color endColor = shadeColors[bucket + 1];
			double shadeFactor = delta / bucketSeparation - bucket;
			Log.debug(LOG, "shadeFactor [", new Double(value), "] =  ", new Double(shadeFactor));
			Log.debug(LOG, "startColor = ", startColor);
			Log.debug(LOG, "endColor = ", endColor);
			int redGap = endColor.getRed() - startColor.getRed();
			int greenGap = endColor.getGreen() - startColor.getGreen();
			int blueGap = endColor.getBlue() - startColor.getBlue();
			int red = (int)( startColor.getRed() + redGap * shadeFactor );
			int green = (int)( startColor.getGreen() + greenGap * shadeFactor );
			int blue = (int)( startColor.getBlue() + blueGap * shadeFactor );
			Color color;
			try
			{
				color = new Color(red, green, blue);
				Log.debug(LOG, "Color [", new Double(value), "] =  ", color);
			}
			catch (IllegalArgumentException x)
			{
				LOG.error(x.toString());
				color = startColor;
			}
			return color;
		}
	}

	// Parse an HTML-style hex color (like 'ccffee') into a Color
	private static Color color(String hex)
	{
		if ( hex.length() != 6 )
			throw new IllegalArgumentException("color.hex must be 6 characters");
		return new Color(Integer.parseInt(hex.substring(0, 2), 16),
						 Integer.parseInt(hex.substring(2, 4), 16),
						 Integer.parseInt(hex.substring(4, 6), 16));
	}
}
 
