package com.gmail.guyfleeman.atcs.old.turret.server;

/**
 * @author Will Stuckey
 * @date 5/6/14
 * <p></p>
 */
public class ClientHandlerThread extends Thread
{
	public boolean running = true;

	public ClientHandlerThread()
	{

	}

	public void run()
	{
		while (running)
		{

		}
	}

	public void kill()
	{
		this.running = false;
	}
}
