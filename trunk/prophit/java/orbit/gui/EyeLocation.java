package orbit.gui;
	
class EyeLocation
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
