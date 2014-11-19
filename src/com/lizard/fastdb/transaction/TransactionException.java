package com.lizard.fastdb.transaction;

public class TransactionException extends Exception {

	private static final long serialVersionUID = -8529978411395179782L;

	public TransactionException(String msg,Exception e)
	{
		super(msg,e);
	}
	
	public TransactionException(String msg)
	{
		super(msg);
	}
}
