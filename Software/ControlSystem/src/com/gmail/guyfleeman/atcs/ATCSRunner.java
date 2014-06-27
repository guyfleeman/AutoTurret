package com.gmail.guyfleeman.atcs;

import com.gmail.guyfleeman.atcs.client.CLController;
import com.gmail.guyfleeman.atcs.common.CLIParser;
import com.gmail.guyfleeman.atcs.common.Logger;
import com.gmail.guyfleeman.atcs.turret.ATController;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * @author Will Stuckey
 * @date 5/6/14
 * <p></p>
 */
public class ATCSRunner
{
	public static void main(String[] args)
	{
		Logger systemLogger = new Logger();

		CLIParser parser = new CLIParser(true);
		Options cliOptions = new Options();
		cliOptions.addOption("s", "server-mode", false, "start in server mode");
		cliOptions.addOption("c", "client-mode", false, "start in client mode");

		try
		{
			CommandLine cl = parser.parse(cliOptions, args);

			if (!(cl.hasOption("server-mode") || cl.hasOption("client-mode")))
				throw new ParseException("Failed to parse argument for either server or client initialization.");
			else if (cl.hasOption("server-mode"))
				new ATController(args, systemLogger);
			else if (cl.hasOption("client-mode"))
				new CLController(args, systemLogger);
		}
		catch (ParseException e)
		{
			systemLogger.log(Logger.LogLevel.SEVERE, "Failed to parse arguments: " + e.toString());
		}
	}


}
