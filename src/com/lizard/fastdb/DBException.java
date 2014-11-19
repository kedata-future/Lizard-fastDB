package com.lizard.fastdb;

/**
 * fast DB 数据库全局异常类
 * 
 * @author SHEN.GANG
 */
public class DBException extends RuntimeException
{
	private static final long	serialVersionUID	= 1L;

	public DBException()
	{
	}

	public DBException(String msg)
	{
		super(msg);
	}

	public DBException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public DBException(Throwable cause)
	{
		super(cause);
	}
}
