package orbit.ampl;

public class StringDatum
	extends Datum
{
	private final String str;

	public StringDatum(String str)
	{
		this.str = str;
	}

	public int getSize()
	{
		return str.length();
	}

	public String getText()
	{
		return str;
	}
}
