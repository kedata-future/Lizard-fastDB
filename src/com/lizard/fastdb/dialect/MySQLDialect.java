package com.lizard.fastdb.dialect;


/**
 * MySQL5 关系型数据库特定的 SQL Limit 方言
 * 
 * @author SHEN.GANG
 */
public class MySQLDialect extends Dialect
{
	public MySQLDialect(){}

	@Override
	public String getLimitSQL( String sql, int offset, int limit )
	{
		StringBuffer sb = new StringBuffer( sql.length()+20 );
		sb.append(sql);
		
		if ( offset > 0 ) 
		{
			sb.append(" limit ").append(offset).append(", ").append(limit);
		}
		else 
		{
			sb.append(" limit ").append(limit);
		}
		
		return sb.toString();
	}

	@Override
	public String getGeneratedKeySQL(Object... params)
	{
		return "SELECT LAST_INSERT_ID()";
	}

	@Override
	public String getTestSQL()
	{
		return "SELECT 1";
	}
	


}
