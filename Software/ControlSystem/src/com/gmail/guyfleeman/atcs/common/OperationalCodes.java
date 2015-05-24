package com.gmail.guyfleeman.atcs.common;

/**
 * @author willstuckey
 * @date 9/18/14 <p></p>
 */
public enum OperationalCodes
{
	//////////////////////////
	//  primary directives  //
	//////////////////////////

	//heartbeat security
	HEARTBEAT_REQ,
	HEARTBEAT_ACK,

	//command directives
	CMD_SEND,
	CMD_ACK,
	CMD_FIN,

	//no operation
	NO_OP,

	//command callback security mode
	SET_CB_NONE,
	SET_CB_ONE,
	SET_CB_TWO,

	////////////////////////////
	//  secondary directives  //
	////////////////////////////

	//halts
	HALT_NO_REC,
	HALT_EMG,
	HALT_NORM,
	HALT_SHUTDOWN,
	HALT_RESTART,

	//sleep
	SLEEP_INDF,
	SLEEP_UNTL,
	SLEEP_FOR,
	AWAKE,
}
