package orbit.gui.tower;

import orbit.gui.ColorModel;
import orbit.gui.BlockDiagramModel;

import gl4java.GLFunc;
import gl4java.GLUFunc;

import java.awt.Component;
import java.awt.event.ComponentEvent;

/**
 * Defines the interface for a GL-rendered graphical component that uses the ColorModel for
 * coloring information and the BlockDiagramModel as the data source.
 */
public interface TowerImageComponent
{
	public void setColorModel(ColorModel cm);
	
	public void setModel(BlockDiagramModel model);

	/**
	 * This method is called before {@link #render}.
	 */
	public void initialize(Component canvas, GLFunc gl, GLUFunc glu);

	/** Notify the TowerImageComponent that the bounds of the component have changed */
	public void componentResized(ComponentEvent e);
	
	/**
	 * Draw the image component. The component can assume that {@link #initialize} and  <code>GLContext.gljMakeCurrent</code>
	 * have been succesfully invoked.
	 */
	public void render();
}
