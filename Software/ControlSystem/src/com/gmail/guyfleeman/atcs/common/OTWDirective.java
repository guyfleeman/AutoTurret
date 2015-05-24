package com.gmail.guyfleeman.atcs.common;

import java.io.Serializable;

/**
 * @author willstuckey
 * @date 9/18/14 <p></p>
 */
public class OTWDirective implements Serializable
{
	public final OperationalCodes primaryDirective;
	public final OperationalCodes secondaryDirective;
	public final byte             directiveComponents;
	public final ComponentCodes[] directiveComponentOpCodes;
	public final byte[]           directiveComponentData;

	public OTWDirective(final OperationalCodes primaryDirective) throws DirectiveMisconfigurationException
	{
		this(primaryDirective,
		     OperationalCodes.NO_OP);
	}

	public OTWDirective(final OperationalCodes primaryDirective,
	                    final OperationalCodes secondaryDirective) throws DirectiveMisconfigurationException
	{
		this(primaryDirective,
		     secondaryDirective,
		     new ComponentCodes[0],
		     new byte[0]);
	}

	public OTWDirective(final OperationalCodes primaryDirective,
	                    final OperationalCodes secondaryDirective,
	                    final ComponentCodes[] directiveComponentOpCodes,
	                    final byte[] directiveComponentData) throws DirectiveMisconfigurationException
	{
		if (isValidPrimaryOpCode(primaryDirective))
		{
			throw new DirectiveMisconfigurationException("Primary directive is not valid. Type: "
					                                             + primaryDirective);
		}
		else
		{
			this.primaryDirective = primaryDirective;
		}

		if (isValidSecondaryOpCode(secondaryDirective))
		{
			throw new DirectiveMisconfigurationException("Secondary directive is not valid. Type: "
					                                             + secondaryDirective);
		}
		else
		{
			this.secondaryDirective = secondaryDirective;
		}

		if (directiveComponentOpCodes.length != directiveComponentData.length)
		{
			throw new DirectiveMisconfigurationException("Component opcodes and provided data are not of the same " +
					                                             "length.");
		}
		else if (directiveComponentOpCodes.length > Byte.MAX_VALUE || directiveComponentData.length > Byte.MAX_VALUE)
		{
			throw new DirectiveMisconfigurationException("Component length exceeds max allowable size.");
		}
		else
		{
			this.directiveComponents = (byte) directiveComponentOpCodes.length;
		}

		this.directiveComponentOpCodes = directiveComponentOpCodes;
		this.directiveComponentData    = directiveComponentData;
	}

	public static boolean isValidPrimaryOpCode(OperationalCodes opcode)
	{
		return (opcode == OperationalCodes.HEARTBEAT_REQ
				|| opcode == OperationalCodes.HEARTBEAT_ACK
				|| opcode == OperationalCodes.CMD_SEND
				|| opcode == OperationalCodes.CMD_ACK
				|| opcode == OperationalCodes.CMD_FIN);
	}

	public static boolean isValidSecondaryOpCode(OperationalCodes opcode)
	{
		return !isValidPrimaryOpCode(opcode);
	}
}
