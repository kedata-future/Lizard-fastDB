package com.lizard.fastdb.dialect;

/**
 * ORACLE 关系型数据库特定的 SQL 方言
 * 
 * @author SHEN.GANG
 */
public class OracleDialect extends Dialect
{
	public OracleDialect(){
	}
	
	public String getLimitSQL( String sql, int offset, int limit ) 
	{
		sql = sql.trim();
		StringBuffer sb = new StringBuffer( sql.length()+100 );
		
		if ( offset > 0 ) {
			sb.append("select * from ( select row_.*, rownum rownum_ from ( ");
		}
		else {
			sb.append("select * from ( ");
		}
		
		sb.append( sql );
		
		if ( offset > 0 ) {
			sb.append(" ) row_ ) where rownum_ <= "+(offset+limit)+" and rownum_ > "+offset);
//			sb.append(" ) row_ where rownum <= ?) where rownum_ > ?");
		}
		else {
			sb.append(" ) where rownum <= "+(offset+limit));
		}

		return sb.toString();
	}

	@Override
	public String getGeneratedKeySQL(Object... params)
	{
		if( params == null || params.length == 0 || "".equals( params[0].toString().trim()) )
		{
			throw new IllegalArgumentException("Can't not found the Oracle Sequence Name to [ SELECT .CURRVAL ]!");
		}
		
		// params[0] 为 当前操作 Sequence name
		return "SELECT "+ params[0] +".CURRVAL FROM dual";
	}

	@Override
	public String getTestSQL()
	{
		return "SELECT 1 FROM dual";
	}

}
