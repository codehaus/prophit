package orbit.gui.tower;

import orbit.gui.BlockDiagramModel;
import gl4java.GLEnum;

public abstract class AbstractDisplayListComponent
	extends AbstractTowerImageComponent
{
	/** true if the display list needs to be re-computed */
	private boolean invalid = true;
	/** The display list allocated by OpenGL */
	private int     list    = -1;

	/** Calls the superclass method and then {@link #invalidate} */
	public synchronized void setModel(BlockDiagramModel model)
	{		
		super.setModel(model);
		
		invalidate();
	}

	/**
	 * If the component is {@link #invalid}, the display list is re-built by calling
	 * {@link #paintComponent}. Then the the display list is rendered using {@link #renderDisplayList}.
	 */
	public synchronized void render()
	{
		if ( invalid )
		{
			if ( list == -1 )
			{
				list = gl.glGenLists(1);
			}
			else
			{
				gl.glDeleteLists(list, 1);
			}
			
			gl.glNewList(list, GLEnum.GL_COMPILE);
			paintComponent();
			gl.glEndList();
			invalid = false;
		}
		renderDisplayList();
	}

	/** Default implementation just calls <code>gl.glCallList(list)</code>. */
	protected void renderDisplayList()
	{
		gl.glCallList(list);
	}
	
	/** Ensure that the display list will be re-created before the next rendering */
	protected synchronized void invalidate()
	{
		invalid = true;
	}
}
