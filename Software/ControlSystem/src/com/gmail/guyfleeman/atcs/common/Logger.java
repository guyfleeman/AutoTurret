package com.gmail.guyfleeman.atcs.common;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

/**
 * @author Will Stuckey
 * @date 2/28/14
 * <p></p>
 */
public class Logger implements LoggerInterface
{
	////////////////////////
	//  Internal logging  //
	////////////////////////

	/**
	 * Is the logger logging internal events (debugging)
	 */
	public static boolean internalLogging = false;

	/**
	 * internal log message header
	 */
	protected static final String loggingHeader = "[IA-LOG] ";

	/////////////////////////
	//  Logger properties  //
	/////////////////////////

	/**
	 * Is the logger actively logging
	 */
	public boolean isLogging = true;

	/**
	 * Is the logger logging to System.out as well
	 */
	public boolean duplicateLogToSystemOut = false;

	/**
	 * Is the logger recording the time stamp of events
	 */
	public boolean isUsingTimeStamp = true;

	/**
	 * Force the logger to sync events to the stream, (e.g. System.out is not thread safe. Should it be synchronized?)
	 */
	public boolean forceLogToSyncPrint = false;

	/**
	 * The object used for synchronization.
	 */
	public final Object logPrintSyncObj = new Object();

	private boolean initializedToSystemDotOut = false;
	private int loggingLevel = 0;

	/////////////////////////////////
	//  Logger internal resources  //
	/////////////////////////////////

	/*
	 * External logging
	 */
	private Vector<LoggerInterface> externalLoggers = new Vector<LoggerInterface>();

	/*
	 * IO stuff
	 */
	private PrintWriter logWriter = null;

	/*
	 * date/time information
	 */
	private Calendar timer = new GregorianCalendar();
	private SimpleDateFormat logTimeFormat = new SimpleDateFormat("hh:mm:ss");

	/**
	 * Default Constructor. Initializes the logger to an unsynchronized, appending System.out.
	 */
	public Logger()
	{
		this(System.out);
	}

	/**
	 * Secondary constructor.Initializes the logger to the unsynchronized PrintWriter given.
	 * @param printStream
	 */
	public Logger(OutputStream printStream)
	{
		setOutputStream(printStream);
	}

	/**
	 * Set output stream for logging
	 * @param printStream
	 */
	public void setOutputStream(OutputStream printStream)
	{
		initializedToSystemDotOut = printStream.equals(System.out);

		this.logWriter = new PrintWriter(printStream);
	}

	/**
	 * Closes the stream and logs the event, before GC finalize call is made.
	 * @throws Throwable
	 */
	public void finalize() throws Throwable
	{
		log(LogLevel.WARNING, "Closing logger stream. GC called on logger.");
		logWriter.close();
		super.finalize();
	}

	/**
	 * Sets the time format of the logger.
	 * @param logTimeFormat
	 */
	public void setLogTimeFormat(SimpleDateFormat logTimeFormat)
	{
		this.logTimeFormat = logTimeFormat;
	}

	/**
	 * Set the minimium level event to log
	 * @param loggingLevel minimum logging level
	 */
	public void setLoggingLevel(LogLevel loggingLevel)
	{
		setLoggingLevel(getComparableLevel(loggingLevel));
	}

	/**
	 * Set the minimium level event to log
	 * @param loggingLevel minimum logging level
	 */
	public void setLoggingLevel(int loggingLevel)
	{
		this.loggingLevel = loggingLevel;
	}

	/**
	 * Gets the vector of external loggers
	 * @return vector of external loggers
	 */
	public synchronized Vector<LoggerInterface> getExternalLoggers()
	{
		return externalLoggers;
	}

	/**
	 * Sets the vector of external loggers
	 * @param externalLoggers vector of external loggers
	 */
	public synchronized void setExternalLoggers(Vector<LoggerInterface> externalLoggers)
	{
		this.externalLoggers = externalLoggers;
	}

	/**
	 * Adds an external logger to the list of external loggers
	 * @param externalLogger
	 */
	public void addExternalLogger(LoggerInterface externalLogger)
	{
		externalLoggers.add(externalLogger);
	}

	/**
	 * Logs a message.
	 * @param message
	 */
	public void log(String message)
	{
		log(LogLevel.INFO, message);
	}

	/**
	 * Logs message.
	 * @param logLevel The notification level
	 * @param message The message
	 */
	public void log(LogLevel logLevel, String message)
	{
		log(logLevel, message, false);
	}

	/**
	 * Logs message with a given message notification level.
	 * @param logLevel The notification level
	 * @param message The message
	 * @param overrideLoggingStatus Override the isLogging status
	 */
	public void log(LogLevel logLevel, String message, boolean overrideLoggingStatus)
	{
		if (internalLogging)
		{
			System.out.println(loggingHeader + "Updating timer.");
		}
		updateTimer();

		if (internalLogging)
		{
			System.out.println(loggingHeader + "Creating log string.");
		}
		String log =
				isUsingTimeStamp
						? getLogLevelName(logLevel) + "[" + logTimeFormat.format(timer.getTime()) + "] ###" + message
						: getLogLevelName(logLevel) + " ###" + message;

		/*
		 * if active logging and verbosity check OR override
		 */
		if ((isLogging && getComparableLevel(logLevel) >= loggingLevel) || overrideLoggingStatus)
		{
			if (internalLogging)
			{
				System.out.println(loggingHeader + "Will log.");
			}

			/*
			 * if the logger is forced to synchronize (streams not thread safe)
			 */
			if (forceLogToSyncPrint)
			{
				if (internalLogging)
				{
					System.out.println(loggingHeader + "Forcing sync.");
				}

				/*
				 * sync on sync obj
				 */
				synchronized (logPrintSyncObj)
				{
					if (internalLogging)
					{
						System.out.println(loggingHeader + "Logging to std stm.");
					}

					/*
					 * log to standard stream
					 */
					logWriter.println(log);
					logWriter.flush();

					if (internalLogging)
					{
						System.out.println(loggingHeader + "logging to ext sources.");
					}

					/*
					 * log to external loggers
					 */
					for (LoggerInterface el : externalLoggers)
					{
						if (internalLogging)
						{
							System.out.println("Logging to external logger");
						}

						el.log(log);
					}

					/*
					 * duplicate to console if requested
					 */
					if (duplicateLogToSystemOut && !initializedToSystemDotOut)
					{
						if (internalLogging)
						{
							System.out.println(loggingHeader + "Duplicating log to sys.out.");
						}

						System.out.println(log);
					}
				}
			}
			else
			{
				if (internalLogging)
				{
					System.out.println(loggingHeader + "Logging to std stm.");
				}

				/*
				 * log to standard stream
				 */
				logWriter.println(log);
				logWriter.flush();

				if (internalLogging)
				{
					System.out.println(loggingHeader + "logging to ext sources.");
				}

				/*
				 * log to external loggers
				 */
				for (LoggerInterface el : externalLoggers)
				{
					if (internalLogging)
					{
						System.out.println(loggingHeader + "Duplicating log to sys.out.");
					}

					el.log(log);
				}

				/*
				 * duplicate to console if requested
				 */
				if (duplicateLogToSystemOut)
				{
					if (internalLogging)
					{
						System.out.println(loggingHeader + "Duplicating log to sys.out.");
					}

					System.out.println(log);
				}
			}
		}
	}

	/**
	 * Converts enum to comparable integer value
	 * @param logLevel log level
	 * @return comparable log level
	 */
	protected static int getComparableLevel(LogLevel logLevel)
	{
		switch (logLevel)
		{
			case VERBOSE: return 0;
			case INFO: return 1;
			case WARNING: return 2;
			case SEVERE: return 3;
			case FATAL: return 4;
			default: return 1;
		}
	}

	/**
	 * Creates the line header.
	 * @param logLevel level header
	 * @return string of the line header
	 */
	protected static String getLogLevelName(LogLevel logLevel)
	{
		switch (logLevel)
		{
			case VERBOSE: return "[VERBOSE] ";
			case INFO:    return "[INFO]    ";
			case WARNING: return "[WARNING] ";
			case SEVERE:  return "[SEVERE]  ";
			case FATAL:   return "[FATAL]   ";
			default:      return "[INFO]    ";
		}
	}

	/**
	 * Updates the calendar used as a timer.
	 */
	private void updateTimer()
	{
		timer = new GregorianCalendar();
	}
}
