package orbit.gui;

public class LevelOfDetail
{
	public static final LevelOfDetail VeryFine   = new LevelOfDetail(Strings.getUILabel(LevelOfDetail.class, "veryFine"), 0);
	public static final LevelOfDetail Fine       = new LevelOfDetail(Strings.getUILabel(LevelOfDetail.class, "fine"), 0.0005);
	public static final LevelOfDetail Standard   = new LevelOfDetail(Strings.getUILabel(LevelOfDetail.class, "standard"), 0.001);
	public static final LevelOfDetail Coarse     = new LevelOfDetail(Strings.getUILabel(LevelOfDetail.class, "coarse"), 0.005);
	public static final LevelOfDetail VeryCoarse = new LevelOfDetail(Strings.getUILabel(LevelOfDetail.class, "veryCoarse"), 0.02);

	public static final LevelOfDetail[] ALL_LEVELS = {
		VeryFine,
		Fine,
		Standard,
		Coarse,
		VeryCoarse
	};

	private final String name;
	private final double threshold;

	/**
	 * In general it is not a good idea to construct new LevelOfDetail objects, use the pre-defined
	 * values instead. This constructor is currently only used by this class itself and by test cases.
	 */
	public LevelOfDetail(String name, double threshold)
	{
		this.name = name;
		this.threshold = threshold;
	}

	public String getName() { return name; }
	public double getThreshold() { return threshold; }
	public String toString() { return name; }

	public static int getIndex(LevelOfDetail lod)
	{
		for ( int i = 0; i < ALL_LEVELS.length; ++i )
		{
			if ( ALL_LEVELS[i] == lod )
				return i;
		}
		throw new IllegalArgumentException("LevelOfDetail '" + lod + "' not found");
	}
}
