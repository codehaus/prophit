package orbit.gui.tower;

import orbit.gui.*;
import orbit.util.Log;

import org.apache.log4j.Category;

import gl4java.GLEnum;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Renders the solid view of the tower diagram.
 */
public class TowerDiagramSolid
	extends AbstractDisplayListComponent
	implements GLEnum
{
	public static Category LOG = Category.getInstance(TowerDiagramSolid.class);
	
	protected void addListeners()
	{
		model.addListener(new PropertyChangeListener()
			{
				public void propertyChange(PropertyChangeEvent evt)
				{
					if ( BlockDiagramModel.RENDER_CALL_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.NUM_LEVELS_PROPERTY.equals(evt.getPropertyName()) )
					{
						Log.debug(LOG, "Got PropertyChangeEvent ", evt, ". Invalidating TowerDiagramSolid");
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
		BlockRenderer renderer = new BlockRenderer(gl, Constants.RENDER_SOLID, colorModel);
		layout.setCallback(renderer);

		layout.execute();
		
		model.setGLNameToCallMap(renderer.getGLNameToCallMap());
	}
}
