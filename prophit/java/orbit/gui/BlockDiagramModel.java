package orbit.gui;

import orbit.model.Call;

import java.util.ArrayList;
import java.util.Iterator;

public class BlockDiagramModel
{
	private static EyeLocation DEFAULT_EYE_LOCATION = new EyeLocation(2.5, -Math.PI / 4, Math.PI / 4);
	private static double SHIFT_STEP = 0.05;

	private final ArrayList listeners = new ArrayList();
	
	private int levels;
	private double shiftVertical = 0;
	private double shiftHorizontal = 0;
	private EyeLocation eyeLocation;
	private Call mouseOverCall = null;
	private Call selectedCall = null;

	public BlockDiagramModel()
	{
		eyeLocation = (EyeLocation)DEFAULT_EYE_LOCATION.clone();
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
	
	public void moveEye(double yaw, double pitch)
	{
		eyeLocation = eyeLocation.move(0, yaw, pitch);
		repaint();
	}

	public void shiftVertical(boolean up)
	{
		if ( up )
			shiftVertical += SHIFT_STEP;
		else
			shiftVertical -= SHIFT_STEP;
		repaint();
	}

	public void shiftHorizontal(boolean right)
	{
		if ( right )
			shiftHorizontal += SHIFT_STEP;
		else
			shiftHorizontal -= SHIFT_STEP;
		repaint();
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
	
	private static class EyeLocation
	{
		public final double radius;
		public final double eyeYaw;
		public final double eyePitch;
				
		public EyeLocation(double radius, double eyeYaw, double eyePitch)
		{
			this.radius = radius;
			this.eyeYaw = eyeYaw;
			this.eyePitch = eyePitch;
		}

		public Object clone()
		{
			return new EyeLocation(radius, eyeYaw, eyePitch);
		}
		
		public EyeLocation move(double dR, double dYaw, double dPitch)
		{
			double pitch = eyePitch + dPitch;
			if ( pitch > Math.PI / 2 )
				pitch = Math.PI / 2;
			else if ( pitch < 0 )
				pitch = 0;
			
			return new EyeLocation(radius + dR, eyeYaw + dYaw, pitch);
		}

		public double getRadius() { return radius; }
		public double getPitch() { return eyePitch; }
		public double getYaw() { return eyeYaw; }

		public double getX()
		{
			return radius * Math.cos(eyeYaw) * Math.cos(eyePitch);
		}

		public double getY()
		{
			return radius * Math.sin(eyeYaw) * Math.cos(eyePitch);
		}

		public double getZ()
		{
			return radius * Math.sin(eyePitch);
		}

		public String toString()
		{
			return "r = " + radius + ", yaw = " + eyeYaw + ", pitch = " + eyePitch;
		}
	}
}
