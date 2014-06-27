package com.gmail.guyfleeman.atcs.client.broadcastlistener;

import com.gmail.guyfleeman.atcs.common.AbstractKillable;
import com.gmail.guyfleeman.atcs.common.Killable;
import com.gmail.guyfleeman.atcs.common.Logger;
import com.gmail.guyfleeman.atcs.common.LoggerInterface;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author guyfleeman
 * @date 6/7/14
 * <p></p>
 */
public class MasterBroadcastListener extends AbstractKillable implements Killable
{
	private int maxQueueLength;
	private long pollInterval;

	private Logger logger;
	private Vector<BroadcasterHandler> listeners = new Vector<BroadcasterHandler>(1, 1);

	private ScheduledExecutorService schedulerExecutor;
	private ScheduledFuture broadcastersReaderScheduler;

 	public MasterBroadcastListener(int maxQueueLength, long pollInterval, Logger logger)
    {
		this.logger = logger;
	    this.maxQueueLength = maxQueueLength;
	    this.pollInterval = pollInterval;

	    logger.log(LoggerInterface.LogLevel.INFO, "Initializing broadcasters listener schedule executor.");
	    schedulerExecutor = Executors.newScheduledThreadPool(1);

	    initBroadcastersReaderScheduler();
    }

	private void initBroadcastersReaderScheduler()
	{
		logger.log(LoggerInterface.LogLevel.VERBOSE, "Initializing broadcasters listeners scheduler.");
		broadcastersReaderScheduler = schedulerExecutor.scheduleAtFixedRate(
				new InputProcessor(listeners, logger),
				0L,
				pollInterval,
				SECONDS);
	}

	public boolean addListenerFromBroadcaster(InetAddress address, int port)
	{
		logger.log(LoggerInterface.LogLevel.VERBOSE, "Stopping broadcasters listeners scheduler.");
		broadcastersReaderScheduler.cancel(true);

		try
		{
			BroadcasterHandler broadcasterHandler = new BroadcasterHandler(address, port, maxQueueLength, logger);
			broadcasterHandler.start();

			listeners.add(broadcasterHandler);
			addKillable(broadcasterHandler);
		}
		catch (IOException e)
		{
			logger.log(LoggerInterface.LogLevel.SEVERE, "Could not add listener from broadcaster. "
					+ e.toString() + " " + e.getCause());

			return false;
		}
		finally
		{
			initBroadcastersReaderScheduler();
		}

		return true;
	}

	public void kill()
	{
		broadcastersReaderScheduler.cancel(false);
		super.kill();
	}

	/**
	 *
	 */
	public static class InputProcessor implements Runnable
	{
		private Logger logger;
		private Vector<BroadcasterHandler> listeners;

		public InputProcessor(Vector<BroadcasterHandler> listeners, Logger logger)
		{
			this.listeners = listeners;
			this.logger = logger;
		}

		public void run()
		{
			logger.log(LoggerInterface.LogLevel.VERBOSE, "Broadcast info gather invoked by scheduler.");

			for (BroadcasterHandler bh : listeners)
			{
				for (Object s : bh.getOutputQueue())
				{
					System.out.println(s);
				}
			}
		}
	}
}
