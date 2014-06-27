package com.gmail.guyfleeman.atcs.client.broadcastlistener;

import com.gmail.guyfleeman.atcs.common.Killable;
import com.gmail.guyfleeman.atcs.common.Logger;
import com.gmail.guyfleeman.atcs.common.LoggerInterface;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;

/**
 * @author guyfleeman
 * @date 6/9/14
 * <p></p>
 */
public class BroadcasterHandler extends Thread implements Killable
{
	private boolean running = true;
	private int maxQueueLength = 100;

	private Logger logger;
	private Socket broadcasterSocket;
	private InputStream broadcasterInputStream;
	private LinkedList<String> outputQueue = new LinkedList<String>();

	public BroadcasterHandler(InetAddress address, int port, int maxQueueLength, Logger logger) throws IOException
	{
		logger.log(LoggerInterface.LogLevel.INFO, "Starting client thread");

		/*
		 * Initialize a socket to the broadcaster and open its input stream
		 */
		broadcasterSocket = new Socket(address, port);
		broadcasterInputStream = broadcasterSocket.getInputStream();

		/*
		 * Set the max queue size (prevent memory errors if the stream or queue isn't read/emptied)
		 */
		if (maxQueueLength > 0)
			this.maxQueueLength = maxQueueLength;

		this.logger = logger;
	}

	public void run()
	{
		int i;
		String data;

		/*
		 * While the thread is running and connected, take input from the broadcaster
		 */
		while (running && broadcasterSocket.isConnected())
		{
			data = "";

			try
			{
				/*
				 * Wait for bytes to be sent then read them
				 */
				while ((i = broadcasterInputStream.read()) != -1)
				{
					/*
					 * For each byte, cast to a char and add it to the data String
					 */
					System.out.print((char) i);
					data += (char) i;
				}

				/*
				 * Keep queue size in check
				 */
				logger.log(LoggerInterface.LogLevel.VERBOSE, "Queue size: " + outputQueue.size());
				while (outputQueue.size() > maxQueueLength)
				{
					outputQueue.remove();
				}

				/*
				 * Add the new bytes to the queue as a string
				 */
				if (data != null && data.length() > 0)
				{
					outputQueue.add("[" + broadcasterSocket.getInetAddress() + "] " + data);
				}
			}
			catch (IOException e)
			{
				logger.log(LoggerInterface.LogLevel.WARNING, "Broadcaster read threw IOException. Attempting to " +
						"continue. Is this an isolated event?");
			}
		}
	}

	public LinkedList getOutputQueue()
	{
		return (LinkedList) outputQueue.clone();
	}

	public void kill()
	{
		try
		{
			broadcasterInputStream.close();
			broadcasterInputStream = null;
		}
		catch (IOException e)
		{
			broadcasterInputStream = null;
			logger.log(LoggerInterface.LogLevel.INFO, "Error on closing resources for broadcaster handler.");
		}

		try
		{
			broadcasterSocket.close();
			broadcasterSocket = null;
		}
		catch (IOException e)
		{
			broadcasterSocket = null;
			logger.log(LoggerInterface.LogLevel.INFO, "Error on closing resources for broadcaster handler.");
		}

		this.running = false;
	}

	public void forceKill() {}
}
