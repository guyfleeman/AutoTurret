package com.gmail.guyfleeman.atcs.turret.server.broadcast;

import java.io.OutputStream;
import java.net.Socket;

/**
 * @author willstuckey
 * @date 5/22/14
 * <p></p>
 */
public interface BroadcastClientInterface
{
	public OutputStream getClientOutputStream();

	public Socket getClientSocket();

	public boolean hasConnectionToClient();

	public String toString();
}
