package orbit.gui;

import orbit.model.Call;
import orbit.model.CallGraph;

import java.util.ArrayList;
import java.util.Iterator;

public class BlockDiagramModel
{
	public static int DEFAULT_LEVELS = 5;
	
	private static EyeLocation DEFAULT_EYE_LOCATION = new EyeLocation(2.5, -Math.PI / 4, Math.PI / 4);
	private static double SHIFT_STEP = 0.05;

	private final ArrayList listeners = new ArrayList();
	private final CallGraph cg;
	
	private Call rootRenderCall;

	private int levels = DEFAULT_LEVELS;
	private double shiftVertical = 0;
	private double shiftHorizontal = 0;
	private EyeLocation eyeLocation;
	private Call mouseOverCall = null;
	private Call selectedCall = null;

	public BlockDiagramModel(CallGraph cg)
	{
		eyeLocation = (EyeLocation)DEFAULT_EYE_LOCATION.clone();

		this.cg = cg;
		this.rootRenderCall = cg;
	}

	public synchronized void addListener(Listener listener)
	{
		synchronized ( listeners )
		{
			listeners.add(listener);
		}
	}
	
	public synchronized void removeListener(Listener listener)
	{
		synchronized ( listeners )
		{
			listeners.remove(listener);
		}
	}

	public CallGraph getCallGraph()
	{
		return cg;
	}
	
	public void setRenderCall(Call call)
	{
		rootRenderCall = call;
		invalidate();
	}

	public Call getRenderCall()
	{
		return rootRenderCall;
	}

	public void setSelectedCall(Call call)
	{
		selectedCall = call;
		repaint();
	}

	public Call getSelectedCall()
	{
		return selectedCall;
	}

	public void setMouseOverCall(Call call)
	{
		mouseOverCall = call;
		repaint();
	}

	public Call getMouseOverCall()
	{
		return mouseOverCall;
	}

	public void moveEye(double yaw, double pitch)
	{
		eyeLocation = eyeLocation.move(0, yaw, pitch);
		repaint();
	}

	public EyeLocation getEye()
	{
		return eyeLocation;
	}

	public void shiftVertical(boolean up)
	{
		if ( up )
			shiftVertical += SHIFT_STEP;
		else
			shiftVertical -= SHIFT_STEP;
		repaint();
	}

	public double getShiftVertical()
	{
		return shiftVertical;
	}

	public void shiftHorizontal(boolean right)
	{
		if ( right )
			shiftHorizontal += SHIFT_STEP;
		else
			shiftHorizontal -= SHIFT_STEP;
		repaint();
	}

	public double getShiftHorizontal()
	{
		return shiftHorizontal;
	}

	public void setLevels(int levels)
	{
		this.levels = levels;
		invalidate();
	}

	public int getLevels()
	{
		return levels;
	}
	
	public void addLevel()
	{
		++levels;
		invalidate();
	}

	public void removeLevel()
	{
		--levels;
		if ( levels < 1 )
			levels = 0;
		invalidate();
	}

	protected void invalidate()
	{
		synchronized ( listeners )
		{
			for ( Iterator i = listeners.iterator(); i.hasNext(); )
			{
				((Listener)i.next()).modelInvalidated(this);
			}
		}
	}

	protected void repaint()
	{
		synchronized ( listeners )
		{
			for ( Iterator i = listeners.iterator(); i.hasNext(); )
			{
				((Listener)i.next()).requestRepaint(this);
			}
		}
	}

	public interface Listener
	{
		public void modelInvalidated(BlockDiagramModel model);

		public void requestRepaint(BlockDiagramModel model);
	}
}
