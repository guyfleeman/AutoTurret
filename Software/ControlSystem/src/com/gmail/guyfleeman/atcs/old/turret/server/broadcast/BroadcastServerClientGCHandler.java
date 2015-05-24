package com.gmail.guyfleeman.atcs.old.turret.server.broadcast;

import com.gmail.guyfleeman.atcs.old.common.Killable;
import com.gmail.guyfleeman.atcs.old.common.Logger;
import com.gmail.guyfleeman.atcs.old.common.LoggerInterface;

import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.*;

/**
 * @author willstuckey
 * @date 5/22/14
 * <p></p>
 */
public class BroadcastServerClientGCHandler implements Killable
{
	private ScheduledFuture connectedClientsGCHandler;

	public BroadcastServerClientGCHandler(BroadcastServerInterface broadcastServer, long interval, Logger logger)
	{
		logger.log(LoggerInterface.LogLevel.INFO, "Creating Broadcast GC STP");
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		connectedClientsGCHandler =
				scheduler.scheduleAtFixedRate(
						new ConnectedClientsGCRunnable(broadcastServer, logger),
						0L,
						interval,
						SECONDS);
	}

	public void kill()
	{
	 	connectedClientsGCHandler.cancel(false);
	}

	public void forceKill()
	{
	 	connectedClientsGCHandler.cancel(true);
	}

	public static class ConnectedClientsGCRunnable implements Runnable
	{
		private BroadcastServerInterface broadcastServer;
		private Logger logger;

		public ConnectedClientsGCRunnable(BroadcastServerInterface broadcastServer, Logger logger)
		{
			this.broadcastServer = broadcastServer;
			this.logger = logger;
		}

		public void run()
		{
			int removedClients = 0;
			logger.log(LoggerInterface.LogLevel.INFO, "Broadcast GC check called.");

			if (broadcastServer.getConnectedClients().size() == 0)
			{
				logger.log(LoggerInterface.LogLevel.INFO, "Broadcast GC call found no connected clients to cross " +
						"check.");
			}

			if (broadcastServer != null)
			{
				Vector<BroadcastClientInterface> connectedClients = broadcastServer.getConnectedClients();

				for (BroadcastClientInterface bci : connectedClients)
				{
					System.out.println(bci);
				}

				if (connectedClients != null)
				{
					for (int i = 0; i < connectedClients.size(); i++)
					{
						assert(connectedClients.get(i) instanceof Killable);

						if (connectedClients.get(i) == null)
						{
							connectedClients.remove(i);
							removedClients++;
						}
						else if (!connectedClients.get(i).hasConnectionToClient())
						{
							String addrName = connectedClients.get(i).getClientSocket().getInetAddress().toString();

							((Killable) connectedClients.get(i)).kill();
							((Killable) connectedClients.get(i)).forceKill();
							connectedClients.remove(i);
							removedClients++;

							logger.log(LoggerInterface.LogLevel.INFO, "Found adn removed disconnected thread from: "
									+ addrName);

						}
					}
				}
			}

			logger.log(LoggerInterface.LogLevel.INFO, "Broadcast GC call removed " + removedClients
					+ " null or disconnected clients.");
		}
	}
}
