package com.gmail.guyfleeman.atcs.turret;

import com.gmail.guyfleeman.atcs.common.AbstractKillable;
import com.gmail.guyfleeman.atcs.common.CLIParser;
import com.gmail.guyfleeman.atcs.common.Killable;
import com.gmail.guyfleeman.atcs.common.Logger;
import com.gmail.guyfleeman.atcs.turret.heuristics.AddressWorkerThread;
import com.gmail.guyfleeman.atcs.turret.heuristics.NetHeuristicsWatcherThread;
import com.gmail.guyfleeman.atcs.turret.server.control.ATCServer;
import com.gmail.guyfleeman.atcs.turret.server.broadcast.BroadcastServerClientGCHandler;
import com.gmail.guyfleeman.atcs.turret.server.broadcast.BroadcastServerThread;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.commons.lang3.mutable.MutableInt;

import java.io.File;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Will Stuckey
 * @date 5/7/14
 * <p></p>
 */
public class ATController extends AbstractKillable
{
	/*
	 * CLI args
	 */
	private static final String disableBroadcastArgName            = "disable-broadcast";
	private static final String allowDuplicateConnectionsArgName   = "allow-duplicate-connections";
	private static final String broadcastServerPortArgName         = "broadcast-server-port";
	private static final String controlServerPortArgName           = "control-server-port";
	private static final String serverCertificateDirectoryArgName  = "server-certificate-directory";
	private static final String disableStartupNetHeuristicsArgName = "disable-startup-net-heuristics";
	private static final String queryReachableTimeoutArgName       = "query-reachable-timeout";
	private static final String queryExecutorPoolTimeoutArgName    = "query-master-timeout";
	private static final String logLevelArgName                    = "log-level";

	/*
	 * logging
	 */
	public final Logger logger;

	/*
	 * Net heuristics control
	 */
	private boolean willRunStartupPortQuery = true;
	private int startupPortQueryTimeout = 5 * 1000; //default 5000ms for isReachable() call
	private int startupMasterQueryTimeout = 5 * 60; //default 5 mins for master timeout
	private InetAddress localAddress;
	private HashMap<InetAddress, ArrayList<Integer>> localAddressPortMap = null;

	/*
	 * broadcast server control
	 */
	private boolean willRunBroadcastServer = true;
	private boolean allowDuplicateConnections = false;
	private int broadcastServerPort = Integer.MIN_VALUE;
	private BroadcastServerThread turretPublicBroadcastServer = null;
	private BroadcastServerClientGCHandler turretBroadcastClientGarbageCollector = null;

	/*
	 * Servers
	 */
	private int controlServerPort = Integer.MIN_VALUE;
	private String controlServerCertificateDirectory = null;
	private ATCServer turretAuthorizedControlServer = null;

	public ATController(String[] args, Logger logger)
	{
		this.logger = logger;
		turretInit(args);
	}

	public void turretInit(String[] args)
	{
		this.logger.forceLogToSyncPrint = true;
		//this.logger.duplicateLogToSystemOut = true;
		this.logger.log(Logger.LogLevel.INFO, "ATCS initialized to AT server mode.");

		/*
		 * Initialize data from args
		 */
		initOptions(initArgumentParser(args));
		this.logger.log(Logger.LogLevel.INFO, "Successfully parsed required information.");

		/*
		 * Get local address
		 */
		this.logger.log(Logger.LogLevel.INFO, "Getting local address...");
		localAddress = findLocalAddress();
		this.logger.log(Logger.LogLevel.INFO, "Found local address: " + localAddress);

		/*
		 * Get local port usages and mappings
		 */
		logger.log(Logger.LogLevel.INFO, "Querying local machines for local port and net conflicts...");
		ArrayList<Integer> localPortsInUse = new ArrayList<Integer>();
		if (willRunStartupPortQuery)
		{
			localAddressPortMap = getLocalAddressPortMap(localAddress,
					startupPortQueryTimeout,
					startupMasterQueryTimeout);

			Set<InetAddress> addressSet = localAddressPortMap.keySet();
			for (InetAddress ad : addressSet)
			{
				if (localAddressPortMap.get(ad).size() > 0)
				{
					logger.log(Logger.LogLevel.INFO, "Address: " + ad + ", ports: "
							+ localAddressPortMap.get(ad).toString());
				}
			}
		}

		//TODO check for port conflicts

		if (willRunBroadcastServer)
		{
			turretPublicBroadcastServer = new BroadcastServerThread(false, broadcastServerPort, logger);
			turretPublicBroadcastServer.start();
			super.addKillable(turretPublicBroadcastServer);
			logger.addExternalLogger(turretPublicBroadcastServer);

			turretBroadcastClientGarbageCollector = new BroadcastServerClientGCHandler(
					turretPublicBroadcastServer,
					60,
					logger);
			super.addKillable(turretBroadcastClientGarbageCollector);
		}

		//TODO check for port conflicts.
	}

	/**
	 * Attempts to find local hosts and the ports they are using to identify port forwarding and network conflicts.
	 * @param localHost local address
	 * @return a map of local machines and the ports they are using
	 */
	public HashMap<InetAddress, ArrayList<Integer>> getLocalAddressPortMap(InetAddress localHost,
	                                                                       int queryTimeout,
	                                                                       int queryPoolTimeout)
	{
		if (localHost == null || queryTimeout < 5)
		{
			return null;
		}

		byte[] localIP = localHost.getAddress();
		long startTime = System.currentTimeMillis();
		MutableInt addressesProcessed = new MutableInt(0);
		MutableInt portsProcessed = new MutableInt(0);
		HashMap<InetAddress, ArrayList<Integer>> localAddressPortMap = new HashMap<InetAddress, ArrayList<Integer>>();
		NetHeuristicsWatcherThread netHeuristicsWatcherThread = new NetHeuristicsWatcherThread(
				addressesProcessed,
				portsProcessed,
				startupMasterQueryTimeout,
				logger);
		ExecutorService addressExecutor = Executors.newCachedThreadPool();

		logger.log(Logger.LogLevel.INFO, "Launching startup port query for address: " + localHost +
				" with timeout: " + queryTimeout);

		netHeuristicsWatcherThread.start();

		/*
		 * Get all local addresses
		 */
		for (int fourthIPByte = 0; fourthIPByte < 255; fourthIPByte++)
		{
			localIP[3] = (byte) fourthIPByte;

			/*
			 * Add the address worker to the thread pool
			 */
			try
			{
				InetAddress localAddress = InetAddress.getByAddress(localIP);
				addressExecutor.execute(new AddressWorkerThread(
						localAddressPortMap,
						localAddress,
						queryTimeout,
						queryPoolTimeout,
						logger,
						startTime,
						addressesProcessed,
						portsProcessed));
			}
			catch (Exception e) {}
		}

		/*
		 * init thread pool close
		 */
		addressExecutor.shutdown();

		/*
		 * Wait for thread pool close or timeout
		 */
		try
		{
			addressExecutor.awaitTermination(queryPoolTimeout, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		netHeuristicsWatcherThread.kill();

		logger.log(Logger.LogLevel.INFO, "Finished local address port query. Took: " +
				(System.currentTimeMillis() - startTime) + "ms");

		return localAddressPortMap;
	}

	/**
	 * Try to find a DHCP given or non site local loopback address.
	 *
	 * This method based on methodology described by the apache foundation.
	 * @return local address
	 */
	public InetAddress findLocalAddress()
	{
		try
		{
			InetAddress candidateAddress = null;

			/*
			 * Process each NIC
			 */
			for (Enumeration interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements(); )
			{
				NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();
				/*
				 * Check every address for the net interface
				 */
				for (Enumeration inetAddresses = networkInterface.getInetAddresses(); inetAddresses.hasMoreElements(); )
				{
					InetAddress address = (InetAddress) inetAddresses.nextElement();

					/*
					 * Handle loopbacks and potential site local addresses
					 */
					if (!address.isLoopbackAddress())
					{
						if (address.isSiteLocalAddress())
						{
							return address;
						}
						else if (candidateAddress == null)
						{
							candidateAddress = address;
						}
					}
				}
			}

			if (candidateAddress != null)
			{
				return candidateAddress;
			}

			/*
			 * Crap -_- we're here
			 */
			InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
			if (jdkSuppliedAddress == null)
			{
				throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
			}

			logger.log(Logger.LogLevel.SEVERE, "Unable to verify local address attempting to continue.");
			return jdkSuppliedAddress;
		}
		catch (Exception e)
		{
			UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
			unknownHostException.initCause(e);
			logger.log(Logger.LogLevel.FATAL, "Unable to determine *any* local address. Please check net cfg.");
			exit(-4, null);
			return null;
		}
	}

	public void initFromArgs(String[] args)
	{

	}

	public void initFromProperties(File propertiesFile)
	{

	}

	/**
	 * Initializes ATCS system based on argument values.
	 * @param cl command line
	 */
	@Deprecated
	public void initOptions(CommandLine cl)
	{
		allowDuplicateConnections = cl.hasOption(allowDuplicateConnectionsArgName);

		/*
		 * Handle broadcast server options
		 */
		if (cl.hasOption(disableBroadcastArgName))
		{
			willRunBroadcastServer = false;
		}
		else
		{
			try
			{
				broadcastServerPort = Integer.parseInt(cl.getOptionValue(broadcastServerPortArgName));
				if (broadcastServerPort > 65536 || broadcastServerPort <= 0)
				{
					throw new NumberFormatException("Port range/value not valid.");
				}
			}
			catch (NumberFormatException e)
			{
				logger.log(Logger.LogLevel.SEVERE, "Broadcast server port not valid: " + e.toString());
				logger.log(Logger.LogLevel.SEVERE, "Broadcast server has been disabled by ATCS init!");
				willRunBroadcastServer = false;
			}
		}

		/*
		 * Handle control server port options
		 */
		try
		{
			controlServerPort = Integer.parseInt(cl.getOptionValue(controlServerPortArgName));

			if (controlServerPort > 65536 || controlServerPort <= 0)
			{
				throw new NumberFormatException("Port range/value not valid.");
			}
		}
		catch (NumberFormatException e)
		{
			logger.log(Logger.LogLevel.FATAL, "Control server port not valid: " + e.toString());
			exit(-5, null);
		}

		/*
		 * Handle control server certificate directory options
		 */
		controlServerCertificateDirectory = cl.getOptionValue(serverCertificateDirectoryArgName);

		/*
		 * Handler debug level options
		 */
		if (cl.hasOption(logLevelArgName))
		{
			try
			{
				int loggingLevel = Integer.parseInt(cl.getOptionValue(logLevelArgName));

				if (loggingLevel < 0 || loggingLevel > 4)
				{
					throw new NumberFormatException("Log level not in valid range. Staying at last known good value.");
				}

				logger.setLoggingLevel(loggingLevel);
			}
			catch (NumberFormatException e)
			{
				logger.log(Logger.LogLevel.WARNING, "Unable to set log level from argument: " + e.toString());
			}
		}

		if (cl.hasOption(disableStartupNetHeuristicsArgName))
		{
			willRunStartupPortQuery = false;
			logger.log(Logger.LogLevel.INFO, "Startup port query disabled by argument");
		}
		else
		{
			if (cl.hasOption(queryReachableTimeoutArgName))
			{
				try
				{
					int queryTimeout = Integer.parseInt(cl.getOptionValue(queryReachableTimeoutArgName));

					if (queryTimeout < 5)
					{
						willRunStartupPortQuery = false;
						logger.log(Logger.LogLevel.INFO, "Startup port query disabled by value: " + queryTimeout);
					}

					startupPortQueryTimeout = queryTimeout;
					logger.log(Logger.LogLevel.INFO, "Startup port query timeout set to: " + queryTimeout);
				}
				catch (NumberFormatException e)
				{
					logger.log(Logger.LogLevel.WARNING, "Unable to set query timeout from argument: " + e.toString());
				}
			}

			if (cl.hasOption(queryExecutorPoolTimeoutArgName))
			{
				try
				{
					int threadPoolTimeout = Integer.parseInt(cl.getOptionValue(queryExecutorPoolTimeoutArgName));

					if (threadPoolTimeout < 0)
					{
						throw new NumberFormatException("Thread pool timeout cannot be negative.");
					}

					startupMasterQueryTimeout = threadPoolTimeout;
					logger.log(Logger.LogLevel.INFO, "Startup port query thread pool time out set to: "
							+ startupMasterQueryTimeout);
				}
				catch (NumberFormatException e)
				{
					logger.log(Logger.LogLevel.WARNING, "Unable to set query thread pool timeout from argument: "
							+ e.toString());
				}
			}
		}

	}

	/**
	 * Initializes and checks the CLI program args
	 * @param args the program arguments
	 * @return the parsed and validated command line
	 */
	@Deprecated
	public CommandLine initArgumentParser(String[] args)
	{
		logger.log(Logger.LogLevel.VERBOSE, "Initializing CLI parser...");

		CLIParser parser = new CLIParser(true);
		Options cliOptions = new Options();

		/*
		 * Debug level option
		 */
		OptionBuilder.withLongOpt(logLevelArgName);
		OptionBuilder.withDescription("sets log level");
		OptionBuilder.hasArg(true);
		cliOptions.addOption(OptionBuilder.create());

		cliOptions.addOption("q", disableStartupNetHeuristicsArgName, false, "");

		/*
		 * query reachable timeout and disable option
		 */
		OptionBuilder.withLongOpt(queryReachableTimeoutArgName);
		OptionBuilder.withDescription("timeout for startup local port scan queries in ms. Set <5 for disable. " +
				"Recommended value >4000.");
		OptionBuilder.hasArg(true);
		cliOptions.addOption(OptionBuilder.create());

		/*
		 * query thread pool execution timeout
		 */
		OptionBuilder.withLongOpt(queryExecutorPoolTimeoutArgName);
		OptionBuilder.withDescription("timeout for startup local port scan thread queue termination");
		OptionBuilder.hasArg(true);
		cliOptions.addOption(OptionBuilder.create());

		/*
		 * Disable broadcast option
		 */
		cliOptions.addOption("B", disableBroadcastArgName, false, "disable public broadcast server");

		/*
		 * Allows duplicate connections from an ip
		 */
		cliOptions.addOption("D", allowDuplicateConnectionsArgName, false, "allow duplicate connections");

		/*
		 * broadcast server port option
		 */
		OptionBuilder.withLongOpt(broadcastServerPortArgName);
		OptionBuilder.withDescription("port for the public broadcast server");
		OptionBuilder.hasArg(true);
		OptionBuilder.isRequired(false);
		cliOptions.addOption(OptionBuilder.create());

		/*
		 * control server port option
		 */
		OptionBuilder.withLongOpt(controlServerPortArgName);
		OptionBuilder.withDescription("port for the control server");
		OptionBuilder.hasArg(true);
		OptionBuilder.isRequired(true);
		cliOptions.addOption(OptionBuilder.create());

		/*
		 * control server certificate directory option
		 */
		OptionBuilder.withLongOpt(serverCertificateDirectoryArgName);
		OptionBuilder.withDescription("directory of the X509 TLS certificate for the control server");
		OptionBuilder.hasArg(true);
		OptionBuilder.isRequired(true);
		cliOptions.addOption(OptionBuilder.create());

		logger.log(Logger.LogLevel.VERBOSE, "Initialized CLI parser.");

		/*
		 * try to parse the args
		 */
		CommandLine cl;
		try
		{
			cl = parser.parse(cliOptions, args);
		}
		catch (ParseException e)
		{
			logger.log(Logger.LogLevel.FATAL, "Failed to parse critical CLI information: " + e.toString());
			exit(-1, null);

			//unreachable
			return null;
		}


		if (!cl.hasOption(disableBroadcastArgName) && !cl.hasOption(broadcastServerPortArgName))
		{
			logger.log(Logger.LogLevel.FATAL, "Broadcast server was not disabled and port was not provided.");
			exit(-2, null);
		}
		else if (cl.hasOption(disableBroadcastArgName) && cl.hasOption(broadcastServerPortArgName))
		{
			logger.log(Logger.LogLevel.WARNING, "Found conflicting broadcast server arguments. " +
					"Disabled arg overrides.");
		}

		if (!(new File(cl.getOptionValue(serverCertificateDirectoryArgName)).isFile()))
		{
			logger.log(Logger.LogLevel.FATAL, "ATCServer certificate is not a file.");
			exit(-3, null);
		}

		logger.log(Logger.LogLevel.VERBOSE, "CL arguments passed initial validity check.");

		return cl;
	}

	/**
	 * Properly quits the ATCServer
	 * @param exitCode exit code, Integer.MIN_VALUE forces immediate close with no resource clean up
	 * @param resources any objects that need to be destroyed properly upon exit
	 */
	public static void exit(int exitCode, ArrayList<Killable> resources)
	{
		/*
		 * Force immediate exit
		 */
		if (exitCode == Integer.MIN_VALUE)
			System.exit(exitCode);

		/*
		 * Properly kill threaded resources
		 */
		if (resources != null)
		{
			for (Killable thread : resources)
			{
				if (thread != null)
				{
					thread.kill();
				}
			}
		}

		//exit
		System.exit(exitCode);
	}
}
