package orbit.solver;

public abstract class Datum
{
	public abstract int getSize();

	public abstract String getText();

	public String toString()
	{
		return "[" + getSize() + "]" + getText();
	}
}
