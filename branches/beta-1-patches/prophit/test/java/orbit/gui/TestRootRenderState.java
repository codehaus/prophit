package orbit.gui;

import util.BasicTestCalls;
import util.TestCall;

import orbit.model.Call;

import junit.framework.*;

public class TestRootRenderState
	extends TestCase
{
	public TestRootRenderState(String name)
	{
		super(name);
	}

	public void testRootRenderState()
	{
		BasicTestCalls calls = new BasicTestCalls();

		class RootClient
			implements RootRenderState.Listener
		{
			boolean invalid = false;

			public void renderCallChanged(Call oldCall, Call newCall)
			{ invalid = true; }
		}

		RootClient client = new RootClient();
		RootRenderState state = new RootRenderState(client, calls.root);

		assertEquals(state.getRenderCall(), calls.root);
		assertTrue(!state.hasNextRenderCall());
		assertTrue(!state.hasPreviousRenderCall());

		// Test parentRenderCall if there is no parent
		assertTrue(!state.hasParentRenderCall());
		state.setRenderCallToParent();
		assertEquals(state.getRenderCall(), calls.root);
		assertTrue(!state.hasNextRenderCall());
		assertTrue(!state.hasPreviousRenderCall());

		// Navigate down to the call 'root.main'
		// previousRenderCall and setRenderCallToParent should both work now
		state.setRenderCall(calls.main);
		assertEquals(state.getRenderCall(), calls.main);
		assertTrue(!state.hasNextRenderCall());
		assertTrue(state.hasParentRenderCall());
		assertTrue(state.hasPreviousRenderCall());

		// Move up to the parent
		state.setRenderCallToParent();
		assertEquals(state.getRenderCall(), calls.root);
		assertTrue(!state.hasNextRenderCall());
		assertTrue(state.hasPreviousRenderCall());
		assertTrue(!state.hasParentRenderCall());
		
		// Move to the previous call (main)
		state.previousRenderCall();
		assertEquals(state.getRenderCall(), calls.main);
		assertTrue(state.hasNextRenderCall());
		assertTrue(state.hasParentRenderCall());
		assertTrue(state.hasPreviousRenderCall());

		// Move to the next call (root)
		state.nextRenderCall();
		assertTrue(!state.hasNextRenderCall());
		assertTrue(!state.hasParentRenderCall());
		assertTrue(state.hasPreviousRenderCall());

		// Move back to the previous, then set the call state to dbOpen
		// Should not be a 'next' any more
		// Parent should be 'main'
		state.previousRenderCall();
		state.setRenderCall(calls.dbOpen);
		assertEquals(state.getRenderCall(), calls.dbOpen);
		assertTrue(!state.hasNextRenderCall());
		assertTrue(state.hasParentRenderCall());
		assertTrue(state.hasPreviousRenderCall());

		// Underflow the previousRenderCall
		state.previousRenderCall();
		state.previousRenderCall();
		state.previousRenderCall();
		state.previousRenderCall();
		assertEquals(state.getRenderCall(), calls.root);
		
		// Overflow the nextRenderCall
		state.nextRenderCall();
		state.nextRenderCall();
		state.nextRenderCall();
		state.nextRenderCall();
		assertEquals(state.getRenderCall(), calls.dbOpen);
	}
}
