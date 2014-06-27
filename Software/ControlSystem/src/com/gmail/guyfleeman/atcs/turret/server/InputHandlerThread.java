package com.gmail.guyfleeman.atcs.turret.server;

/**
 * @author Will Stuckey
 * @date 5/6/14
 * <p></p>
 */
public class InputHandlerThread extends Thread
{
	private boolean running = true;

	public InputHandlerThread()
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
