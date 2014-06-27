package com.gmail.guyfleeman.atcs.turret.server.control;

/**
 * @author Will Stuckey
 * @date 5/6/14
 * <p></p>
 */
public class ATCServer
{
	private final int port;

	public ATCServer(int port)
	{
	 	this.port = port;

		//TODO implement SSL/TLS cert framework

		//TODO create socket
		//TODO init input/output handlers
		//TODO init video handler D: ))):

		//TODO implement multithreaded client handler
	}

	public static void exit()
	{
		//TODO exit securely
	}
}
