package orbit.gui.tower;

import orbit.gui.BlockDiagramModel;
import orbit.model.Call;
import orbit.util.Log;

import org.apache.log4j.Category;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;

/**
 * Draws a highlight on each block which is a search result.
 * Drawing the search result blocks as solid blocks is handled by TowerDiagramWireFrame.
 */
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
		for ( Iterator i = model.getAllSearchResultCalls().iterator(); i.hasNext(); )
		{
			Call call = (Call)i.next();
			highlightBlock(call, colorModel.getSearchMatchColor());
		}
	}
}
