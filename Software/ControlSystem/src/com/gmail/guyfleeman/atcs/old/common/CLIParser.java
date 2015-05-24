package com.gmail.guyfleeman.atcs.old.common;

import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;

import java.util.ListIterator;

/**
 * @author Will Stuckey
 * @date 5/7/14
 * <p></p>
 */
public class CLIParser extends GnuParser
{
	private boolean ignoreExtraArgs;

	public CLIParser(boolean ignoreExtraArgs)
	{
		this.ignoreExtraArgs = ignoreExtraArgs;
	}

	public boolean isIgnoreExtraArgs()
	{
		return ignoreExtraArgs;
	}

	public void setIgnoreExtraArgs(boolean ignoreExtraArgs)
	{
		this.ignoreExtraArgs = ignoreExtraArgs;
	}

	@Override
	protected void processOption(final String arg, final ListIterator iter) throws ParseException
	{
		if (getOptions().hasOption(arg) || !ignoreExtraArgs)
			super.processOption(arg, iter);
	}
}
