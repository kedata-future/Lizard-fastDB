package com.lizard.fastdb.datasource;

import com.lizard.fastdb.DBException;

/**
 *  数据源异常类
 * 
 * @author SHEN.GANG
 */
public class DataSourceException extends DBException
{
	private static final long	serialVersionUID	= 1L;

	public DataSourceException()
	{
	}

	public DataSourceException(String message)
	{
		super(message);
	}

	public DataSourceException(Throwable cause)
	{
		super(cause);
	}

	public DataSourceException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
