package orbit.gui.tower;

import orbit.gui.BlockDiagramModel;
import orbit.model.Call;
import orbit.util.Log;

import org.apache.log4j.Category;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;

public class SearchResultsComponent
	extends AbstractHighlightBlockComponent
{
	public static Category LOG = Category.getInstance(SearchResultsComponent.class);
	
	protected void addListeners()
	{
		this.model.addListener(new PropertyChangeListener()
			{
				public void propertyChange(PropertyChangeEvent evt)
				{
					if ( BlockDiagramModel.RENDER_CALL_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.NUM_LEVELS_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.NAME_SEARCH_STRING_PROPERTY.equals(evt.getPropertyName()) )
					{
						Log.debug(LOG, "Got PropertyChangeEvent ", evt, ". Invalidating SelectedCallsComponent");
						invalidate();
					}
				}
			});
	}
	
	public void paintComponent()
	{
		List callNames = model.getNameSearchNames();
		for ( Iterator i = callNames.iterator(); i.hasNext(); )
		{
			String name = (String)i.next();
 			for ( Iterator j = model.getCallsByName(name).iterator(); j.hasNext(); )
			{
				Call call = (Call)j.next();
				highlightBlock(call, colorModel.getSearchMatchColor());
			}
		}
	}
}
