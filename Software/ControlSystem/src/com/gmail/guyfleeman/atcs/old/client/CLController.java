package com.gmail.guyfleeman.atcs.old.client;

import com.gmail.guyfleeman.atcs.old.client.broadcastlistener.MasterBroadcastListener;
import com.gmail.guyfleeman.atcs.old.common.Logger;
import com.gmail.guyfleeman.atcs.old.common.LoggerInterface;

import java.io.IOException;
import java.net.InetAddress;

/**
 * @author Will Stuckey
 * @date 5/7/14
 * <p></p>
 */
public class CLController
{
	public final Logger logger;

	public CLController(String[] args, Logger logger)
	{
		this.logger = logger;

		logger.log(LoggerInterface.LogLevel.INFO, "Initialized to AT client mode.");


		MasterBroadcastListener mbl = new MasterBroadcastListener(100, 1L, logger);
		try
		{
			mbl.addListenerFromBroadcaster(InetAddress.getByName("192.168.1.105"), 4343);
		}
		catch (IOException e)
		{
			e.getCause();
			e.printStackTrace();
		}
	}
}
