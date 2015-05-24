package com.gmail.guyfleeman.atcs.old.turret.heuristics;

import com.gmail.guyfleeman.atcs.old.common.Logger;
import org.apache.commons.lang3.mutable.MutableInt;

import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * @author Will Stuckey
 * @date 5/8/14
 * <p></p>
 */
public class PortWorkerThread implements Runnable
{
	private int port;

	private long startTime;

	private MutableInt portsProcessed;

	private ArrayList<Integer> listeningPorts;
	private InetAddress localAddress;
	private Logger logger;

	public PortWorkerThread(ArrayList<Integer> ports,
	                        InetAddress localAddress,
	                        int port,
	                        Logger logger,
	                        long startTime,
	                        MutableInt portsProcessed)
	{
		this.listeningPorts = ports;
		this.localAddress = localAddress;
		this.port = port;
		this.logger = logger;
		this.startTime = startTime;
		this.portsProcessed = portsProcessed;
	}

	public void run()
	{
		try
		{
			Socket testSocket = new Socket(localAddress, port);
			listeningPorts.add(port);
			testSocket.close();

			logger.log(Logger.LogLevel.VERBOSE, "Found address: " + localAddress + ", listening on port: " + port
					+ " @T " + (System.currentTimeMillis() - startTime) + "ms");
		}
		catch (Exception e)
		{
			logger.log(Logger.LogLevel.VERBOSE, "Inactive port: " + port + " on " + localAddress);
		}
		finally
		{
			portsProcessed.increment();
		}
	}
}
