package com.lizard.fastdb.dialect;

import com.lizard.fastdb.DBException;

/**
 * 未知数据库方言异常
 * 
 * @author SHEN.GANG
 */
public class UnknownDialectException extends DBException
{
	private static final long	serialVersionUID	= 1L;

	public UnknownDialectException()
	{
	}

	public UnknownDialectException(String message)
	{
		super(message);
	}

	public UnknownDialectException(Throwable cause)
	{
		super(cause);
	}

	public UnknownDialectException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
