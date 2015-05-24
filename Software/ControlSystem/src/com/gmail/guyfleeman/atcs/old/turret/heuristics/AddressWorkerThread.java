package com.gmail.guyfleeman.atcs.old.turret.heuristics;

import com.gmail.guyfleeman.atcs.old.common.Logger;
import org.apache.commons.lang3.mutable.MutableInt;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Will Stuckey
 * @date 5/8/14
 * <p></p>
 */
public class AddressWorkerThread implements Runnable
{
	private int timeout;
	private int queryPoolTimeout;

	private long startTime;

	private MutableInt addressesProcessed;
	private MutableInt portsProcessed;

	private HashMap<InetAddress, ArrayList<Integer>> localAddressPortMap;
	private InetAddress localAddress;
	private Logger logger;


 	public AddressWorkerThread(HashMap<InetAddress, ArrayList<Integer>> localAddressPortMap,
                               InetAddress localAddress,
                               int timeout,
                               int queryPoolTimeout,
                               Logger logger,
                               long startTime,
                               MutableInt addressesProcessed,
                               MutableInt portsProcessed)
    {
		this.localAddressPortMap = localAddressPortMap;
	    this.localAddress = localAddress;
	    this.timeout = timeout;
	    this.queryPoolTimeout = queryPoolTimeout;
	    this.logger = logger;
	    this.startTime = startTime;
	    this.addressesProcessed = addressesProcessed;
	    this.portsProcessed = portsProcessed;
    }

	public void run()
	{
		ExecutorService portExecutor = Executors.newCachedThreadPool();
		ArrayList<Integer> listeningPorts = new ArrayList<Integer>();

		try
		{
			if (localAddress.isReachable(timeout))
			{
				logger.log(Logger.LogLevel.VERBOSE, "Found local host: " + localAddress
						+ " @T" + (System.currentTimeMillis() - startTime) + "ms");

				for (int port = 0; port < 65536; port++)
				{
					portExecutor.execute(new PortWorkerThread(
							listeningPorts,
							localAddress,
							port,
							logger,
							startTime,
							portsProcessed));
				}
			}
			else
			{
				logger.log(Logger.LogLevel.VERBOSE, "Found unresponsive address: " + localAddress
						+ " @T " + (System.currentTimeMillis() - startTime));

				portsProcessed.add(65536);
			}
		}
		catch (Exception e)
		{
			portsProcessed.add(65536);
		}
		finally
		{
			addressesProcessed.increment();
		}

		portExecutor.shutdown();

		try
		{
			portExecutor.awaitTermination(queryPoolTimeout, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		localAddressPortMap.put(localAddress, (ArrayList<Integer>) listeningPorts.clone());

		logger.log(Logger.LogLevel.VERBOSE, "Found local address: " + localAddress + " with listening ports: "
				+ (listeningPorts.size() == 0 ? "none" : listeningPorts.toString()) +
				" @T " + (System.currentTimeMillis() - startTime));
	}
}
