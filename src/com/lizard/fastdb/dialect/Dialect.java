package com.lizard.fastdb.dialect;

import com.lizard.fastdb.util.ReflectUtils;

/**
 * 特定关系型数据库实现的SQL方言
 * 
 * @author SHEN.GANG
 */
public abstract class Dialect
{
	/**
	 * MySQL数据库方言
	 */
	public static final String	MYSQL	= MySQLDialect.class.getName();
	/**
	 * Oracle数据库方言
	 */
	public static final String	ORACLE	= OracleDialect.class.getName();

	/**
	 * 特定关系型数据库查询结果的数量限制原则
	 * 
	 * @param sql
	 *            要限制结果数量的查询语句
	 * @param offset
	 *            查询结果的开始位置
	 * @param limit
	 *            限制的结果数量
	 * @return 追加上限制原则的query语句
	 */
	public abstract String getLimitSQL(String sql, int offset, int limit);

	/**
	 * 根据数据库特性获取新增的主键值
	 * 
	 * @param params
	 * @return 可以获取主键值的SQL
	 */
	public abstract String getGeneratedKeySQL(Object... params);
	
	/**
	 * 定义一个sql语句，用来检测数据库连接是否有效
	 * 该语句应该被快速的执行.
	 * MySQL： SELECT 1
	 * Oracle: SELECT 1 FROM dual
	 * 
	 * @return test sql
	 */
	public abstract String getTestSQL();
	
	/**
	 * 组装完整的分页语句
	 * 
	 * @param db_dialect
	 *            数据库方言
	 * @param sql
	 *            sql语句片段
	 * @param page
	 *            页码
	 * @param pagesize
	 *            页面数据量大小
	 * @return 完整的分页语句
	 */
	public static String createPageSQL(String db_dialect, String sql, int page, int pagesize)
	{
		String pageSql = "";

		// 计算查询开始位置 当page<1 时统一设置为 0
		int offset = page < 1 ? 0 : ((page - 1) * pagesize);
		int limit = pagesize <= 0 ? 10 : pagesize;

		try
		{
			// 实例化Dialect为子类db_dialect，用于获得不同数据库对应sql分页语句
			Dialect dialect = (Dialect) ReflectUtils.newInstanceFromCache(db_dialect);
			pageSql = dialect.getLimitSQL(sql, offset, limit);

		}
		catch (Exception e)
		{
			throw new UnknownDialectException(e);
		}

		return pageSql;
	}

	/**
	 * 根据驱动获取对应的数据库方言
	 * 
	 * @param driverClass
	 * @return
	 */
	public static Dialect getDialect( String driverClass )
	{
		if( null == driverClass )
		{
			return null;
		}
		
		if( driverClass.indexOf("mysql") != -1 )
		{
			return new MySQLDialect();
		}
		else if( driverClass.indexOf("oracle") != -1 )
		{
			return new OracleDialect();
		}
		
		return null;
	}
}
