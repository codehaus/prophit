package orbit.model;

/**
 * Enumerated type representing Inclusive and Exclusive timing data.
 */
public class TimeData
{
	public static final TimeData Inclusive = new TimeData("Inclusive");
	public static final TimeData Exclusive = new TimeData("Exclusive");
	
	private final String name;
	
	private TimeData(String name) { this.name = name; }
	
	public  String toString()     { return name; }
}
	
