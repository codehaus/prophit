package orbit.gui.tower;

import orbit.gui.BlockDiagramModel;
import orbit.gui.ColorModel;
import gl4java.GLFunc;
import gl4java.GLUFunc;

import java.awt.Component;

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

	/**
	 * Draw the image component. The component can assume that {@link #initialize} and  <code>GLContext.gljMakeCurrent</code>
	 * have been succesfully invoked.
	 */
	public void render();
}
