package orbit.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class PathUtil
{
	private static String[] EMPTY_ARRAY = new String[0];

	// Tokenize a string into a String[], respecting the use of quotes
	// (", ') to delimit a single arg
	public static String[] tokenizeArgs(String args)
	{
		if (args == null)
			return EMPTY_ARRAY;
			
		String quotes = "\"'";
		ArrayList argList = new ArrayList();
		boolean quoting = false;
		StringTokenizer st = new StringTokenizer(args, " \t\n\r\f", true);
		while (st.hasMoreTokens())
		{
			String each = st.nextToken();
			if (each.trim().length() == 0)
			{
				if (quoting)
				{
					argList.set(argList.size() - 1, ((String) argList.get(argList.size() - 1)) + each);
				}
				continue;
			}

			if (quoting)
			{
				if (quotes.indexOf(each.charAt(each.length() - 1)) != -1)
				{
					quoting = false;
					each = each.substring(0, each.length() - 1);
				}
				argList.set(argList.size() - 1, ((String) argList.get(argList.size() - 1)) + each);
			}
			else
			{
				if (quotes.indexOf(each.charAt(0)) != -1)
				{
					if (quotes.indexOf(each.charAt(each.length() - 1)) != -1)
					{
						quoting = false;
						each = each.substring(1, each.length() - 1);
					}
					else
					{
						quoting = true;
						each = each.substring(1);
					}

				}
				argList.add(each);
			}
		}
		return (String[]) argList.toArray(new String[argList.size()]);
	}


	
	// Utility method for converting a list of paths into a string
	public static String joinPath(List path)
	{
		StringBuffer result = new StringBuffer();
		for (Iterator iter = path.iterator(); iter.hasNext();)
		{
			result.append(iter.next());
			result.append(File.pathSeparatorChar);
		}
		if (result.length() > 0)
			result.deleteCharAt(result.length() - 1);
		return result.toString();
	}


	// Utility method for converting a string path to a List
	public static List splitPath(String path)
	{
		List result = new ArrayList();
		StringTokenizer stok = new StringTokenizer(path, File.pathSeparator);
		while (stok.hasMoreTokens())
		{
			result.add(stok.nextToken());
		}
		return result;
	}


	private PathUtil()
	{
	}

}
