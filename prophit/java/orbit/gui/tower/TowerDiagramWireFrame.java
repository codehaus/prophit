package orbit.gui.tower;

import orbit.gui.*;
import orbit.util.Log;

import org.apache.log4j.Category;

import gl4java.GLEnum;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Renders the wire-frame view of the tower diagram. If the BlockDiagramModel is in wireframe mode, then
 * blocks which are selected or search-results are rendered as solid. This makes them stand out from the
 * rest of the diagram.
 */
public class TowerDiagramWireFrame
	extends AbstractDisplayListComponent
	implements GLEnum
{
	public static Category LOG = Category.getInstance(TowerDiagramWireFrame.class);
	
	protected void addListeners()
	{
		model.addListener(new PropertyChangeListener()
			{
				public void propertyChange(PropertyChangeEvent evt)
				{
					if ( BlockDiagramModel.RENDER_CALL_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.NUM_LEVELS_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.WIREFRAME_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.SELECTED_CALL_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.NAME_SEARCH_STRING_PROPERTY.equals(evt.getPropertyName()) )
					{
						Log.debug(LOG, "Got PropertyChangeEvent ", evt, ". Invalidating TowerDiagramWireFrame");
						invalidate();
					}
				}
			});
	}

	public void paintComponent()
	{
		Rectangle2D.Double rootRectangle = new Rectangle2D.Double(0, 0, Constants.DIAGRAM_EXTENT, Constants.DIAGRAM_EXTENT);
		CallLayoutAlgorithm layout = new CallLayoutAlgorithm(new CallAdapter(model.getRootRenderState().getRenderCall()),
															 TimeMeasure.TotalTime,
															 model.getLevels(),
															 rootRectangle);
		BlockRenderer renderer = new BlockRenderer(gl, Constants.RENDER_WIREFRAME, colorModel);
		layout.setCallback(renderer);

		/*
		 * If the wireframe checkbox is checked, render all the search-result
		 *   and selected calls as solid blocks.
		 */
		if ( model.isWireframe() )
		{
			renderer.addSolidBlocks(model.getAllSelectedCalls());
			renderer.addSolidBlocks(model.getAllSearchResultCalls());
		}

		layout.execute();
	}
}
