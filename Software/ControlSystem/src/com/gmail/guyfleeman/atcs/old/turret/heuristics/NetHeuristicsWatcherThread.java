package com.gmail.guyfleeman.atcs.old.turret.heuristics;

import com.gmail.guyfleeman.atcs.old.common.Killable;
import com.gmail.guyfleeman.atcs.old.common.Logger;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 * @author Will Stuckey
 * @date 5/12/14
 * <p></p>
 */
public class NetHeuristicsWatcherThread extends Thread implements Killable
{
	private boolean running = true;

	private long startTime = 0L;

	private Logger logger;

	private final int totalAddresses;
	private final int totalPorts;
	private final int checkInterval;
	private final int heuristicsTimeout;

	private final MutableInt addressesProcessed;
	private final MutableInt portsProcessed;

	public NetHeuristicsWatcherThread(MutableInt addressesProcessed,
	                                  MutableInt portsProcessed,
	                                  int heuristicsTimeout,
	                                  Logger logger)
	{
		this(255, addressesProcessed, 255 * 65536, portsProcessed, 3, heuristicsTimeout, logger);
	}

	public NetHeuristicsWatcherThread(int totalAddresses,
	                                  MutableInt addressesProcessed,
	                                  int totalPorts,
	                                  MutableInt portsProcessed,
	                                  int checkInterval,
	                                  int heuristicsTimeout,
	                                  Logger logger)
	{
	 	this.totalAddresses = totalAddresses;
		this.totalPorts = totalPorts;
		this.checkInterval = checkInterval * 1000;
		this.heuristicsTimeout = heuristicsTimeout * 1000;

		this.addressesProcessed = addressesProcessed;
		this.portsProcessed = portsProcessed;

		this.logger = logger;
	}

	public void run()
	{
		startTime = System.currentTimeMillis();

		while (running)
		{
			try
			{
				logger.log(Logger.LogLevel.INFO, "NH STDBY: "
						+ "Addresses scanned: "
						+ Math.floor(((float) addressesProcessed.getValue() / (float) totalAddresses) * 100f) + "% "
						+ "(" + addressesProcessed.getValue() + "/" + totalAddresses + "), "
						+ "Ports Scanned: "
						+ Math.floor(((float) portsProcessed.getValue() / (float) totalPorts) * 100f) + "% "
						+ "(" + portsProcessed.getValue() + "/" + totalPorts + "), "
						+ "Time Elapsed: "
						+ Math.floor((float) (((double) (System.currentTimeMillis() - startTime))
								/ (double) heuristicsTimeout) * 100d)
						+ "% "
						+ "(" + (System.currentTimeMillis() - startTime) + ")");

				Thread.sleep(checkInterval);
			}
			catch (InterruptedException e) {}
		}
	}

	public void kill()
	{
		this.running = false;
	}

	public void forceKill()
	{
		kill();
	}
}
