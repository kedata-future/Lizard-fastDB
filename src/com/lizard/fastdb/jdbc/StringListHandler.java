package com.lizard.fastdb.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.handlers.AbstractListHandler;

/**
 * 创建StringListHandler用于支持 queryForList&lt;String&gt;
 * 
 * @author SHEN.GANG
 */
public class StringListHandler extends AbstractListHandler<String>
{
	private final int		columnIndex;
	private final String	columnName;

	public StringListHandler()
	{
		this(1, null);
	}

	public StringListHandler(int columnIndex)
	{
		this(columnIndex, null);
	}

	public StringListHandler(String columnName)
	{
		this(1, columnName);
	}

	private StringListHandler(int columnIndex, String columnName)
	{
		super();
		this.columnIndex = columnIndex;
		this.columnName = columnName;
	}

	protected String handleRow(ResultSet rs) throws SQLException
	{
		Object val = null;
		if (this.columnName == null)
		{
			val = rs.getObject(this.columnIndex);
		}
		else
		{
			val = rs.getObject(this.columnName);
		}

		return val == null ? "" : val.toString();
	}

}
