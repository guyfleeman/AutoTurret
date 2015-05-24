package com.gmail.guyfleeman.atcs.old.turret.server.broadcast;

import com.gmail.guyfleeman.atcs.old.common.Killable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author Will Stuckey
 * @date 5/7/14
 * <p></p>
 */
public class BroadcastClientHandler implements Killable, BroadcastClientInterface
{
	public static final String termMessage = "Server closing. Disconnecting...";

	private Socket clientSocket;
	private OutputStream clientOutputStream;

	public BroadcastClientHandler(Socket clientSocket) throws IOException
	{
		this.clientSocket = clientSocket;
		this.clientOutputStream = clientSocket.getOutputStream();
	}

	public Socket getClientSocket()
	{
		return clientSocket;
	}

	public OutputStream getClientOutputStream()
	{
		return this.clientOutputStream;
	}

	public boolean hasConnectionToClient()
	{
		try
		{
			clientOutputStream.write((char) 0x0000);
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}

	public void kill()
	{
		try
		{
			clientSocket.getOutputStream().write(termMessage.getBytes());
			clientOutputStream.flush();
			clientOutputStream.close();
			clientOutputStream = null;
		}
		catch (IOException e)
		{
			clientOutputStream = null;
		}

		try
		{
			clientSocket.close();
			clientSocket = null;
		}
		catch (IOException e)
		{
			clientSocket = null;
		}
	}

	public void forceKill()
	{
		clientOutputStream = null;
		clientSocket = null;
	}

	public String toString()
	{
		return clientSocket.toString() + " con: " + hasConnectionToClient();
	}
}
