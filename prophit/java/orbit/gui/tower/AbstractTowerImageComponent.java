package orbit.gui.tower;

import orbit.gui.ColorModel;
import orbit.gui.BlockDiagramModel;

import gl4java.GLFunc;
import gl4java.GLUFunc;

public abstract class AbstractTowerImageComponent
	implements TowerImageComponent
{
	protected ColorModel        colorModel;
	protected BlockDiagramModel model;
	protected GLFunc            gl;
	protected GLUFunc           glu;

	public void setColorModel(ColorModel cm)
	{
		this.colorModel = cm;
	}
	
	public void setModel(BlockDiagramModel model)
	{
		this.model = model;
		addListeners();
	}
	
	public void initialize(GLFunc gl, GLUFunc glu)
	{
		this.gl = gl;
		this.glu = glu;
	}

	/**
	 * Override this method to draw the component using the supplied graphical APIs. Assume that
	 * {@link #initialize} has already been called. 
	 */
	protected abstract void paintComponent();

	/** Override this method to add listeners to the BlockDiagramModel. */
	protected abstract void addListeners();
}
