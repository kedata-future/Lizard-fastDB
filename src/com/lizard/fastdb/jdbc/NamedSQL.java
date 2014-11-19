package com.lizard.fastdb.jdbc;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.lizard.fastdb.DBException;

/**
 * SQL命名动态参数类，用来表示解析之后的SQL信息<br>
 * 命名参数<:参数名> - <:id>，SELECT * FROM table WHERE id =:id
 *
 * @author SHEN.GANG
 */
public class NamedSQL
{
	/**
	 * 命名参数被占位符 ? 替换后的标准预编译SQL语句
	 */
	private String sql;
	
	/**
	 * 命名参数名称
	 */
	private List<String> namedParameter = new LinkedList<String>();
	
	
	public NamedSQL() { }

	
	public void addParameter( String param_name )
	{
		this.namedParameter.add(param_name);
	}
	
	/**
	 * @return the namedParameter
	 */
	public List<String> getNamedParameter()
	{
		return namedParameter;
	}
	
	/**
	 * @return the sql
	 */
	public String getSQL()
	{
		return sql;
	}

	/**
	 * @param sql the sql to set
	 */
	public void setSQL(String sql)
	{
		this.sql = sql;
	}
	
	/**
	 * 解析命名参数值Map对象为预编译SQL语句使用的Object[]对象
	 * 
	 * @param valueMap 命名参数值Map
	 * @return 相对应顺序的参数值
	 */
	public Object[] parseParameterValue(Map<String, Object> paramValues)
	{
		if( null == namedParameter || namedParameter.size() == 0 
				|| null == paramValues || paramValues.isEmpty() )
		{
			return new Object[]{};
		}
		
		Object[] paramVals = new Object[namedParameter.size()];
		for( int i = 0, len = namedParameter.size(); i < len; i++ )
		{
			paramVals[i] = paramValues.get(namedParameter.get(i));
		}
		
		return paramVals;
	}
	
	
	/**
	 * NamedSQL缓存，用于避免相同named sql的重复解析，获取更高效率
	 */
	private static final Map<String, NamedSQL> NAMED_SQL_CACHE= new HashMap<String, NamedSQL>();
	
	private static final String NAMED_SEPARATORS = " \n\r\f\t,()=<>&|+-=/*'^![]#~\\";
	
	/**
	 * 解析含有命名动态参数的SQL语句<br>
	 * Example: SELECT * FROM table WHERE id >:id AND time BETWEEN :stime AND :etime
	 * 
	 * @param namedSql 含有命名动态参数的SQL语句
	 * @return NamedSQL
	 */
	public static NamedSQL parse(String namedSql)
	{
		synchronized(NAMED_SQL_CACHE)
		{
			NamedSQL namedSQL = NAMED_SQL_CACHE.get(namedSql.toLowerCase());
			if( null == namedSQL )
			{
				namedSQL = parseNamedSQL(namedSql);
				NAMED_SQL_CACHE.put(namedSql.toLowerCase(), namedSQL);
			}
			
			return namedSQL;
		}
	}
	
	/**
	 * 解析含有命名动态参数的SQL语句<br>
	 * Example: SELECT * FROM table WHERE id >:id AND time BETWEEN :stime AND :etime
	 * 
	 * @param namedSql 含有命名动态参数的SQL语句
	 * @return NamedSQL
	 */
	protected static NamedSQL parseNamedSQL(String namedSql)
	{
		NamedSQL namedSQL = new NamedSQL();
		
		int nsql_len = namedSql.length();
		
		// 标志一个<:> 是否出现在''引用中，如果出现在引用中，则忽略
	    boolean inQuote = false;
	    String param = null;
	    
	    StringBuilder sqlFrag = new StringBuilder();
	    int cut_start = 0;
	    
	    for (int indx = 0; indx < nsql_len; indx++) 
	    {
	      char c = namedSql.charAt(indx);
	      // 进入''引用中遍历
	      if (inQuote) 
	      {
	    	// 本次''引用结束
	        if ('\'' == c) 
	        {
	          inQuote = false;
	        }
	      }
	      else if ('\'' == c) 
	      {
	        inQuote = true;
	      }
	      else if (c == ':') 
	      {
	    	// 处理SQL中特殊的 := 赋值操作
	    	if( namedSql.charAt(indx+1) == '=' )
	    	{
	    		continue;
	    	}
	    	  
	        int right = firstIndexOfChar(namedSql, NAMED_SEPARATORS, indx + 1);
	        int chopLocation = right < 0 ? nsql_len : right;
	        
	        param = namedSql.substring(indx + 1, chopLocation);
	        
	        if (param == null || param.trim().length() == 0) 
	        {
	        	throw new DBException("Space is not allowed after parameter prefix ':' [" + namedSql + "]");
	        }

	        namedSQL.addParameter(param);
	        
	        sqlFrag.append(namedSql.substring(cut_start, indx));
	        sqlFrag.append("?");
	        cut_start = indx + param.length() + 1;
	        
	        indx = chopLocation - 1;
	      }
	    }
		
	    sqlFrag.append(namedSql.substring(cut_start, nsql_len));
	    namedSQL.setSQL(sqlFrag.toString());
	    sqlFrag = null;
	    
		return namedSQL;
	}

	private static int firstIndexOfChar(String sqlString, String string, int startindex) 
	{
	    int matchAt = -1;
	    for (int i = 0, len = string.length(); i < len; i++) 
	    {
	      int curMatch = sqlString.indexOf(string.charAt(i), startindex);
	      if (curMatch >= 0) 
	      {
	        if (matchAt == -1) 
	        {
	          matchAt = curMatch;
	        }
	        else 
	        {
	          matchAt = Math.min(matchAt, curMatch);
	        }
	      }
	    }
	    
	    return matchAt;
	}
}
