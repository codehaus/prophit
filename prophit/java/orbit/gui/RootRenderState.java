package orbit.gui;

import orbit.model.Call;

import java.util.ArrayList;

/**
 * Stores the root call of the block diagram. Implements browser-like navigation ability,
 * including going 'back' and 'forward' between roots, and going 'up' to the root's parent.
 */
public class RootRenderState
{
	private final Client    client;
	private final ArrayList calls = new ArrayList();
	private int       index = -1;
	
	public RootRenderState(Client client, Call root)
	{
		this.client = client;

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
		return (Call)calls.get(index);
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
		++index;
		invalidate();
	}

	public boolean hasPreviousRenderCall()
	{
		return index > 0;
	}

	public void previousRenderCall()
	{
		if ( !hasPreviousRenderCall() )
			return;
		--index;
		invalidate();
	}

	protected void invalidate()
	{
		client.invalidate();
	}

	public interface Client
	{
		public void invalidate();
	}
}
