package orbit.gui.tower;

import orbit.gui.BlockDiagramModel;
import orbit.gui.Constants;
import orbit.model.Call;
import orbit.model.Traversal;
import orbit.util.Log;

import org.apache.log4j.Category;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a mapping from call names to lists of calls. This algorithm is stateful, it keeps track of when
 * it needs to be re-run. Just call {@link #setModel} whenever the BlockDiagramModel changes to a new instance.
 */
public class NameListAlgorithm
{
	public static Category LOG = Category.getInstance(NameListAlgorithm.class);

	/** Assume a window size of 1000 pixels */
	private static double MIN_TIME_THRESHOLD = Constants.MIN_BLOCK_SIZE_THRESHOLD / 1000;

	private BlockDiagramModel model   = null;
	private boolean           invalid = true;
	
	public void setModel(BlockDiagramModel model)
	{
		this.model = model;

		addListeners();
		invalidate();
	}

	public void execute()
	{
		if ( !invalid )
			return;

		HashMap nameToCallListMap = new HashMap();
		new NameListTraversal(nameToCallListMap).execute(model.getRootRenderState().getRenderCall());
		model.setNameToCallListMap(nameToCallListMap);

		invalid = false;
	}

	private void addListeners()
	{
		model.addListener(new PropertyChangeListener()
			{
				public void propertyChange(PropertyChangeEvent evt)
				{
					if ( BlockDiagramModel.RENDER_CALL_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.NUM_LEVELS_PROPERTY.equals(evt.getPropertyName()) )
					{
						Log.debug(LOG, "Got PropertyChangeEvent ", evt, ". Invalidating NameListAlgorithm");
						invalidate();
					}
				}
			});
	}

	private void invalidate()
	{
		invalid = true;
	}

	private class NameListTraversal
		extends Traversal
	{
		private final Map map;
		private Call  rootCall = null;
		
		public NameListTraversal(Map map)
		{
			this.map = map;
		}
		
		protected boolean beginCall(Call call, int depth)
		{
			Log.debug(LOG, "Mapping ", call, " at depth ", depth);
			
			if ( rootCall == null )
				rootCall = call;
			
			int maxDepth = model.getLevels();
			boolean cont = true;
			if ( maxDepth != -1 && depth > maxDepth )
				cont = false;
			else if ( call.getTime() / rootCall.getTime() < MIN_TIME_THRESHOLD )
				cont = false;
			if ( !cont )
			{
				Log.debug(LOG, "\tEnding traversal");
				return false;
			}

			List list = (List)map.get(call.getName());
			if ( list == null )
			{
				list = new ArrayList(3);
				map.put(call.getName(), list);
			}
			list.add(call);

			return true;
		}

		protected void endCall() { }
	}
}
