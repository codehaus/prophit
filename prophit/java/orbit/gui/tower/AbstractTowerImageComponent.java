package orbit.gui.tower;

import orbit.gui.ColorModel;
import orbit.gui.Constants;
import orbit.gui.BlockDiagramModel;

import gl4java.GLFunc;
import gl4java.GLUFunc;

import java.awt.Component;
import java.awt.event.ComponentEvent;

public abstract class AbstractTowerImageComponent
	implements TowerImageComponent, Constants
{
	protected ColorModel        colorModel;
	protected BlockDiagramModel model;
	protected Component         canvas;
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
	
	public void initialize(Component canvas, GLFunc gl, GLUFunc glu)
	{
		this.canvas = canvas;
		this.gl = gl;
		this.glu = glu;
	}

	/** Notify the TowerImageComponent that the bounds of the component have changed */
	public void componentResized(ComponentEvent e)
	{
	}
	
	/**
	 * Override this method to draw the component using the supplied graphical APIs. Assume that
	 * {@link #initialize} has already been called. 
	 */
	protected abstract void paintComponent();

	/** Override this method to add listeners to the BlockDiagramModel. */
	protected abstract void addListeners();
}
