package orbit.gui.tower;

import orbit.gui.BlockDiagramModel;
import orbit.gui.CallAdapter;
import orbit.gui.CallLayoutAlgorithm;
import orbit.gui.Constants;
import orbit.gui.ColorModel;
import orbit.gui.TimeMeasure;
import orbit.util.Log;

import org.apache.log4j.Category;

import gl4java.GLEnum;
import gl4java.GLFunc;
import gl4java.GLUFunc;
import gl4java.awt.GLCanvas;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;

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
			for ( Iterator i = model.getNameSearchNames().iterator(); i.hasNext(); )
			{
				String name = (String)i.next();
				renderer.addSolidBlocks(model.getCallsByName(name));
			}
			if ( model.getSelectedCall() != null )
			{
				renderer.addSolidBlock(model.getSelectedCall());
				renderer.addSolidBlocks(model.getCallsByName(model.getSelectedCall().getName()));
			}
		}

		gl.glDisable(GL_DITHER);
		gl.glDisable(GL_LIGHTING);
		gl.glDisable(GL_CULL_FACE);

		layout.execute();
	}
}
