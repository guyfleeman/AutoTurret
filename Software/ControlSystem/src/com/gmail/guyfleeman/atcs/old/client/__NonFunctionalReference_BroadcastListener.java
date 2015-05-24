package com.gmail.guyfleeman.atcs.old.client;

import com.gmail.guyfleeman.atcs.old.common.Killable;
import com.gmail.guyfleeman.atcs.old.common.Logger;
import com.gmail.guyfleeman.atcs.old.common.LoggerInterface;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author guyfleeman
 * @date 6/7/14
 * <p></p>
 */
public class __NonFunctionalReference_BroadcastListener extends Thread implements Killable
{
	private boolean running = true;
	private boolean initialized = false;

	private Logger logger;

	private Selector broadcastersSelector = Selector.open();
	private SocketChannel broadcastersHandler;

	public __NonFunctionalReference_BroadcastListener(Logger logger) throws IOException
	{
		this.logger = logger;
	 	broadcastersHandler = initBroadcastersHandler(broadcastersSelector);
	}

	public __NonFunctionalReference_BroadcastListener(InetAddress address, int port, Logger logger) throws IOException
	{
		this(logger);
		listenFrom(address, port);
	}

	protected SocketChannel initBroadcastersHandler(Selector selector) throws IOException
	{
		logger.log(LoggerInterface.LogLevel.INFO, "Broadcast listener initializing channel and selector.");

		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_READ);

		logger.log(LoggerInterface.LogLevel.INFO, "DONE.");

		return channel;
	}

	public boolean listenFrom(InetAddress address, int port)
	{
		return listenFrom(new InetSocketAddress(address, port));
	}

	public boolean listenFrom(InetSocketAddress address)
	{
		try
		{
			broadcastersHandler.connect(address);
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}

	public void run()
	{
		if (broadcastersHandler == null || broadcastersSelector == null)
		{
			return;
		}


		while (running)
		{
			try
			{
				broadcastersSelector.select();
			}
			catch (IOException e)
			{
				return;
			}

			Set serverKeys = broadcastersSelector.selectedKeys();
			Iterator keysIterator = serverKeys.iterator();

			while (keysIterator.hasNext())
			{
				SelectionKey key = (SelectionKey) keysIterator.next();
				keysIterator.remove();

				if (key.isReadable())
				{
					System.out.println("Readable");
					//System.out.println(key.attachment().getClass());
				}
			}
		}
	}

	public void kill()
	{
		try
		{
			broadcastersSelector.close();
			broadcastersSelector = null;

			broadcastersHandler.close();
			broadcastersHandler = null;
		}
		catch (IOException e)
		{
			broadcastersSelector = null;
			broadcastersHandler = null;
		}

	 	running = false;
	}

	public void forceKill()
	{
	 	running = false;
	}
}
