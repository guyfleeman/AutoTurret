package com.gmail.guyfleeman.atcs.turret.server.broadcast;

import java.util.Vector;

/**
 * @author willstuckey
 * @date 5/22/14
 * <p></p>
 */
public interface BroadcastServerInterface
{
	public Vector<BroadcastClientInterface> getConnectedClients();
}
