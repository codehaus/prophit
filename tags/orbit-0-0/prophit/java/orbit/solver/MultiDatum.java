package orbit.solver;

import java.util.ArrayList;
import java.util.Iterator;

public class MultiDatum
	extends Datum
{
	private final ArrayList data = new ArrayList();

	public void add(Datum datum)
	{
		data.add(datum);
	}

	public int getSize()
	{
		int size = 0;
		for ( Iterator i = data.iterator(); i.hasNext(); )
		{
			Datum datum = (Datum)i.next();
			size += datum.getSize();
		}
		return size;
	}

	public String getText()
	{
		StringBuffer sb = new StringBuffer();
		for ( Iterator i = data.iterator(); i.hasNext(); )
		{
			Datum datum = (Datum)i.next();
			sb.append(datum.getText());
		}
		return sb.toString();
	}
}
