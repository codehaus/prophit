package orbit.launcher;

import java.security.Permission;


public class DenyExitSecurityManager extends SecurityManager
{
	boolean denyExit = true;

	SecurityManager sm;

	public DenyExitSecurityManager(SecurityManager sm)
	{
		super();
		this.sm = sm;
	}

	public void setDenyExit(boolean flag)
	{
		denyExit = flag;
	}

	public void checkExit(int status)
	{
		if (denyExit)
		{
			boolean exitAllowed = true;

			// If we are executing in a launcher process threadgroup,
			// then deny exit
			ThreadGroup procTG = Thread.currentThread().getThreadGroup();
			while(true)
			{
				String name = procTG.getName();
				if (name.startsWith(BaseProcess.THREADGROUP_NAME_PREFIX))
				{
					exitAllowed = false;
					break;
				}
				ThreadGroup parent = procTG.getParent();
				if (parent == null)
					break;
				else
					procTG = parent;
			}
			
			if (! exitAllowed)
				throw new DenyExitSecurityException("Exits disabled while launcher processes/consoles are still active", status);
		}
		if (sm != null)
			sm.checkExit(status);
	}


	public void checkPermission(Permission param1)
	{
		// Do nothing here to allow everything
		if (sm != null)
			sm.checkPermission(param1);
	}

}
