package orbit.gui;

import orbit.model.Call;
import orbit.model.CallGraph;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;

public class BlockDiagramModel
{
	public static final String RENDER_CALL_PROPERTY = "renderCall";
	public static final String NUM_LEVELS_PROPERTY = "numLevels";

	public static final String SELECTED_CALL_PROPERTY = "selectedCall";
	public static final String MOUSEOVER_CALL_PROPERTY = "mouseOverCall";

	public static final String SHIFT_HORIZONTAL_PROPERTY = "shiftHorizontal";
	public static final String SHIFT_VERTICAL_PROPERTY = "shiftVertical";
	public static final String EYE_POSITION_PROPERTY = "eyePosition";

	public static int DEFAULT_LEVELS = 5;
	
	private static EyeLocation DEFAULT_EYE_LOCATION = new EyeLocation(2.5, -Math.PI / 4, Math.PI / 4);
	private static double SHIFT_STEP = 0.05;

	private final ArrayList listeners = new ArrayList();
	private final CallGraph cg;

	private RootRenderState rootState;

	private PropertyChangeSupport changeSupport;

	private int levels = DEFAULT_LEVELS;
	private double shiftVertical = 0;
	private double shiftHorizontal = 0;
	private EyeLocation eyeLocation;
	private Call mouseOverCall = null;
	private Call selectedCall = null;

	public BlockDiagramModel(CallGraph cg)
	{
		this.eyeLocation = (EyeLocation)DEFAULT_EYE_LOCATION.clone();

		this.changeSupport = new PropertyChangeSupport(this);
		this.cg = cg;
		this.rootState = new RootRenderState(new RootRenderState.Listener()
			{
				public void renderCallChanged(Call oldCall, Call newCall)
				{
					changeSupport.firePropertyChange(RENDER_CALL_PROPERTY, oldCall, newCall);
				}
			}, 
			cg);
	}

	/**
	 * Clean up all references
	 */
	public void dispose()
	{
		listeners.clear();
		rootState = null;
		eyeLocation = null;
		mouseOverCall = null;
		selectedCall = null;
	}
	
	public synchronized void addListener(PropertyChangeListener listener)
	{
		synchronized ( listeners )
		{
			changeSupport.addPropertyChangeListener(listener);
		}
	}
	
	public synchronized void removeListener(PropertyChangeListener listener)
	{
		synchronized ( listeners )
		{
			changeSupport.removePropertyChangeListener(listener);
		}
	}

	public CallGraph getCallGraph()
	{
		return cg;
	}

	public RootRenderState getRootRenderState()
	{
		return rootState;
	}
	
	public void setSelectedCall(Call call)
	{
		Call oldSelectedCall = selectedCall;
		if ( call != oldSelectedCall )
		{
			selectedCall = call;
			changeSupport.firePropertyChange(SELECTED_CALL_PROPERTY, oldSelectedCall, selectedCall);
		}
	}

	public Call getSelectedCall()
	{
		return selectedCall;
	}

	public void setMouseOverCall(Call call)
	{
		Call oldCall = mouseOverCall;
		if ( call != oldCall )
		{
			mouseOverCall = call;
			changeSupport.firePropertyChange(MOUSEOVER_CALL_PROPERTY, oldCall, mouseOverCall);
		}
	}

	public Call getMouseOverCall()
	{
		return mouseOverCall;
	}

	public void moveEye(double yaw, double pitch)
	{
		EyeLocation old = eyeLocation;
		eyeLocation = eyeLocation.move(0, yaw, pitch);
		changeSupport.firePropertyChange(EYE_POSITION_PROPERTY, old, eyeLocation);
	}

	public EyeLocation getEye()
	{
		return eyeLocation;
	}

	public void shiftVertical(boolean up)
	{
		double old = shiftVertical;
		if ( up )
			shiftVertical += SHIFT_STEP;
		else
			shiftVertical -= SHIFT_STEP;
		changeSupport.firePropertyChange(SHIFT_VERTICAL_PROPERTY, new Double(old), new Double(shiftVertical));
	}

	public double getShiftVertical()
	{
		return shiftVertical;
	}

	public void shiftHorizontal(boolean right)
	{
		double old = shiftHorizontal;
		if ( right )
			shiftHorizontal += SHIFT_STEP;
		else
			shiftHorizontal -= SHIFT_STEP;
		changeSupport.firePropertyChange(SHIFT_HORIZONTAL_PROPERTY, new Double(old), new Double(shiftHorizontal));
	}

	public double getShiftHorizontal()
	{
		return shiftHorizontal;
	}

	public void setLevels(int levels)
	{
		int old = this.levels;
		if ( levels != old )
		{
			this.levels = levels;
			changeSupport.firePropertyChange(NUM_LEVELS_PROPERTY, old, levels);
		}
	}

	public int getLevels()
	{
		return levels;
	}
	
	public void addLevel()
	{
		setLevels(levels + 1);
	}

	public void removeLevel()
	{
		if ( levels > 0 )
			setLevels(levels - 1);
	}
}
