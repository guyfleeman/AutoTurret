package com.gmail.guyfleeman.atcs.old.common;

import java.util.Vector;

/**
 * @author guyfleeman
 * @date 5/31/14
 * <p></p>
 */
public abstract class AbstractKillable extends Thread implements Killable
{
	private Vector<Killable> subKillables = new Vector<Killable>(1, 1);

	public AbstractKillable() {}

	public Vector<Killable> getSubKillables()
	{
		return subKillables;
	}

	public void addKillable(Killable killable)
	{
		subKillables.add(killable);
	}

	public void removeKillable(Killable killable)
	{
		for (int i = 0; i < subKillables.size(); i++)
		{
			if (subKillables.get(i).equals(killable))
			{
				subKillables.remove(i);
			}
		}
	}

	public void killSubKillable(Killable killable)
	{
		for (int i = 0; i < subKillables.size(); i++)
		{
			if (subKillables.get(i).equals(killable))
			{
				subKillables.get(i).kill();
				subKillables.get(i).forceKill();
				subKillables.remove(i);
			}
		}
	}

	public void kill()
	{
		for (Killable k : subKillables)
		{
			k.kill();
		}
	}

	public void forceKill()
	{
		for (Killable k : subKillables)
		{
			k.forceKill();
		}

		subKillables = null;
	}
}
