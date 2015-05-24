package com.gmail.guyfleeman.atcs.old.common;

/**
 * @author guyfleeman
 * @date 6/6/14
 * <p></p>
 */
public interface LoggerInterface
{
	/**
	 * Log levels.
	 */
	public enum LogLevel
	{
		VERBOSE,
		INFO,
		WARNING,
		SEVERE,
		FATAL
	}

	public void log(LogLevel logLevel, String message);

	public void log(String message);
}
