package orbit.gui;

public interface Constants
{
	/**
	 * Extent of the diagram rendering space. This roughly translates to the size of the
	 * base of the diagram
	 */
	public static double DIAGRAM_EXTENT = 1.0;

	/**
	 * Extent of the entire screen. This could be calculated from the projection matrix
	 *   but this constant should work well enough for now.
	 */
	public static double SCREEN_EXTENT = 1.5;

	public static final int    RENDER_SOLID     = 0;
	public static final int    RENDER_WIREFRAME = 1;

	/** The height of each block */
	public static final double BLOCK_HEIGHT           = 0.05;

	/**
	 * If a function makes up more than this amount of time of the parent call, its coloring is shaded.
	 * If less, it is rendered in the base block color
	 */ 
	public static final double FRACTION_THRESHOLD = 0.30;

	// TODO: move into separate text-drawing class
	public static int TEXT_OFFSET_FROM_LEFT = 4;
	public static int FONT_HEIGHT = 14;
	public static int TEXT_BORDER = 3;
}
