package orbit.gui;

public class TimeMeasure
{
	public static final int TOTAL_TIME = 0;
	public static final int AVERAGE_TIME = 1;
	
	public static final TimeMeasure TotalTime = new TimeMeasure(TOTAL_TIME, "TotalTime");
	public static final TimeMeasure AverageTime = new TimeMeasure(AVERAGE_TIME, "AverageTime");

	public final int code;
	public final String description;
	
	private TimeMeasure(int code, String description)
	{
		this.code = code;
		this.description = description;
	}

	public String toString() { return description; }
}
