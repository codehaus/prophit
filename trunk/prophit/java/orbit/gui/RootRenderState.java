package orbit.gui;

import orbit.model.Call;

import java.util.ArrayList;

/**
 * Stores the root call of the block diagram. Implements browser-like navigation ability,
 * including going 'back' and 'forward' between roots, and going 'up' to the root's parent.
 */
public class RootRenderState
{
	private final Listener  listener;
	private final ArrayList calls = new ArrayList();
	private int       index = -1;
	
	public RootRenderState(Listener listener, Call root)
	{
		this.listener = listener;

		setRenderCall(root);
	}

	public void setRenderCall(Call call)
	{
		for ( int i = calls.size() - 1; i > index; --i )
		{
			calls.remove(i);
		}
		calls.add(call);
		nextRenderCall();
	}

	public Call getRenderCall()
	{
		if ( index >= 0 )
			return (Call)calls.get(index);
		else
			return null;
	}

	public boolean hasParentRenderCall()
	{
		return getRenderCall().getParent() != null;
	}
	
	public void setRenderCallToParent()
	{
		Call parent = getRenderCall().getParent();
		if ( parent != null )
			setRenderCall(parent);
	}
											
	public boolean hasNextRenderCall()
	{
		return calls.size() - 1 > index;
	}

	public void nextRenderCall()
	{
		if ( !hasNextRenderCall() )
			return;
		Call oldCall = getRenderCall();
		++index;
		callChanged(oldCall);
	}

	public boolean hasPreviousRenderCall()
	{
		return index > 0;
	}

	public void previousRenderCall()
	{
		if ( !hasPreviousRenderCall() )
			return;
		Call oldCall = getRenderCall();
		--index;
		callChanged(oldCall);
	}

	protected void callChanged(Call oldCall)
	{
		if ( !getRenderCall().equals(oldCall) )
			listener.renderCallChanged(oldCall, getRenderCall());
	}

	public interface Listener
	{
		public void renderCallChanged(Call oldCall, Call newCall);
	}
}
