package orbit.launcher;

// Interface for redirecting access to objects based on the
// threadgroup of the thread they run in.
//
public interface Redirector
{
	public void addMap(ThreadGroup tgKey, Object resource);
	public void removeMap(ThreadGroup tgKey);
	public Object getResource();
	public Object getDefaultResource();
	public boolean isGroupCurrentResourceOwner(ThreadGroup group);
}
