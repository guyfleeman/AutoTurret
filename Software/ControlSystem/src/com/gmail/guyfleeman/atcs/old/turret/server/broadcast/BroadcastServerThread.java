package com.gmail.guyfleeman.atcs.old.turret.server.broadcast;

import com.gmail.guyfleeman.atcs.old.common.AbstractKillable;
import com.gmail.guyfleeman.atcs.old.common.Killable;
import com.gmail.guyfleeman.atcs.old.common.Logger;
import com.gmail.guyfleeman.atcs.old.common.LoggerInterface;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Vector;

/**
 * @author Will Stuckey
 * @date 5/7/14
 * <p></p>
 */
public class BroadcastServerThread extends AbstractKillable implements  Killable, LoggerInterface, BroadcastServerInterface
{
	public boolean running = true;

	private boolean allowDuplicateConnections;
	private int port;

	private Logger logger;

	private ServerSocket broadcastServerSocket;
	private Vector<BroadcastClientInterface> connectedClients = new Vector<BroadcastClientInterface>(10, 1);

	public BroadcastServerThread(boolean allowDuplicateConnections,
	                             int port,
	                             Logger logger)
	{
		this.allowDuplicateConnections = allowDuplicateConnections;
	 	this.port = port;
		this.logger = logger;
	}

	public void run()
	{
		while (running)
		{
			try
			{
				//TODO possible exception badness
				broadcastServerSocket = new ServerSocket(port);

				while (running)
				{
					try
					{
						BroadcastClientHandler connectedClient =
								new BroadcastClientHandler(broadcastServerSocket.accept());

						if (!allowDuplicateConnections)
						{
							boolean foundClientFromSameAddress = false;

							if (connectedClients.size() > 0)
							{
								for (BroadcastClientInterface client : connectedClients)
								{
									System.out.println(connectedClient.getClientSocket().getInetAddress()
											+ ", " + client.getClientSocket().getInetAddress());

									if (connectedClient.getClientSocket().getInetAddress()
											.equals(client.getClientSocket().getInetAddress()))
									{
										foundClientFromSameAddress = true;
										break;
									}
								}
							}

							if (!foundClientFromSameAddress)
							{
								logger.log(Logger.LogLevel.INFO, "Client connecting from: "
										+ connectedClient.getClientSocket().getInetAddress());

								super.addKillable(connectedClient);
								connectedClients.add(connectedClient);
							}
							else
							{
								logger.log(Logger.LogLevel.INFO, "Non-primary client attempting to connect " +
										"from: " + connectedClient.getClientSocket().getInetAddress());

								connectedClient.kill();
							}

						}
						else
						{
							connectedClients.add(connectedClient);
						}
					}
					catch (IOException e) {}
				}
			}
			catch (Exception e)
			{
				logger.log(Logger.LogLevel.SEVERE, "Failed to establish broadcast server on: " + port);
				logger.log(Logger.LogLevel.INFO, "Waiting to reestablish broadcast server...");
				try { Thread.sleep(15000); } catch (InterruptedException iEx) {}
				logger.log(Logger.LogLevel.INFO, "Reestablishing...");
			}
		}
	}

	/**
	 * Logs a message to all connected broadcast clients with the given severity.
	 * @param logLevel severity
	 * @param message message to broadcast
	 */
	public void log(LogLevel logLevel, String message)
	{
		log(message);
	}

	/**
	 * Logs a message to all connected broadcast clients.
	 * @param message message to broadcast
	 */
	public void log(String message)
	{
		if (connectedClients.size() == 0)
		{
			return;
		}

		for (BroadcastClientInterface connectedClient : connectedClients)
		{
			try
			{
				connectedClient.getClientOutputStream().write(message.getBytes());
			}
			catch (IOException e)
			{
				logger.log(LogLevel.WARNING, "Could not log to a client. Is broadcast GC out of sync with logger?");
			}
		}
	}

	/**
	 * Returns the server socket
	 * @return broadcast server socket
	 */
	public ServerSocket getBroadcastServerSocket()
	{
		return broadcastServerSocket;
	}

	/**
	 * Returns the vector of the connected clients
	 * @return connected clients
	 */
	public synchronized Vector<BroadcastClientInterface> getConnectedClients()
	{
		return connectedClients;
	}

	/**
	 * Kills the broadcast server thread
	 */
	public void kill()
	{
		super.kill();
		this.running = false;
	}

	/**
	 * Force kills the broadcast server thread
	 */
	public void forceKill()	{}
}
