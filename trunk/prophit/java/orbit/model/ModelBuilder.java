package orbit.model;

import java.util.List;

/**
 * ModelBuilder is a simple interface that can be used to load a profile into the
 * prophIt data model.
 */
public interface ModelBuilder
{
	/**
	 * @param td whether the timing information in the profile data file is
	 * {@link TimeData#Inclusive inclusive} or {@link TimeData#Exclusive exclusive}.
	 */
	public void initialize(TimeData td);

	/**
	 * @param stackArray array of method names that make up the stack trace. Line numbers
	 * should generally not be part of the stack trace. For example:
	 * <pre>
	 * java.lang.StringBuffer.<init>(StringBuffer.java)
	 * java.io.DataInputStream.readUTF(DataInputStream.java)
	 * java.io.DataInputStream.readUTF(DataInputStream.java)
	 * org.hsqldb.Column.readData(Column.java)
	 * </pre>
	 */
	public ID newStackTrace(String[] stackArray);

	/**
	 * Use this method to construct a new recorded call, which is the combination
	 * of a stack trace with the number of calls and the time spent in the stack trace.
	 * The time should be either inclusive or exclusive time, according to the
	 * {@link TimeData} argument used in {@link #initialize}.
	 */
	public void newRecordedCall(ID stackTraceID, int nCalls, long time);

	/**
	 * Invoke this method when call construction is complete.
	 */
	public void end();
	
	/**
	 * @return a list of {@link CallID}s after the model construction has been completed.
	 */
	public List getCallIDs();

	public interface ID
	{
		public int     hashCode();
		public boolean equals(Object other);
		public String  toString();
	}
}
