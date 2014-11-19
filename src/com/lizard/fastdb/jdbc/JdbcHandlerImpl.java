package com.lizard.fastdb.jdbc;

import java.io.Serializable;
import java.sql.BatchUpdateException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lizard.fastdb.DBException;
import com.lizard.fastdb.connection.ConnectionPool;
import com.lizard.fastdb.datasource.DataSource;
import com.lizard.fastdb.datasource.DataSourceUtil;
import com.lizard.fastdb.dialect.Dialect;
import com.lizard.fastdb.persistence.PersistenceUtil;
import com.lizard.fastdb.util.StringUtils;

/**
 * JDBC 操作实现类， 实现了操作接口类中的接口方法
 * 
 * @author SHEN.GANG
 */
public class JdbcHandlerImpl implements JdbcHandler, Serializable
{
	private static final long					serialVersionUID	= -4972487968257069301L;
	private static final Log					logger				= LogFactory.getLog(JdbcHandlerImpl.class);

	private DataSource							ds					= null;									// 对应的数据源配置

	/**
	 * 查询对象，true -- 表示不使用getParameterType，因为Oracle不支持此功能
	 */
	private QueryRunner							QUERY_RUNNER		= new QueryRunner(true);

	/**
	 * 存储批量分批操作大小，默认不分批操作
	 */
	private static final ThreadLocal<Integer>	BATCH_SIZE_LOCAL	= new ThreadLocal<Integer>()
																	{
																		protected Integer initialValue()
																		{
																			return -1;
																		}
																	};

	/**
	 * 重写dbutils中的ColumnListHandler的handleRow方法，处理BigInt型数据为Long型 注意：这里处理BigInteger 和 BigDecimal 方式已经转移到 apache dbutils 源码中了，所以注释了处理代码段。
	 */
	private final static ColumnListHandler		COLUMNLIST_HANDLER	= new ColumnListHandler();
	// {
	// @Override
	// protected Object handleRow(ResultSet rs) throws SQLException
	// {
	// Object obj = super.handleRow(rs);
	// if (obj instanceof BigInteger)
	// {
	// return ((BigInteger) obj).longValue();
	// }
	// else if (obj instanceof BigDecimal)
	// {
	// return ((BigDecimal) obj).longValue();
	// }
	//
	// return obj;
	// }
	// };

	/**
	 * 创建 StringListHandler对象用于 queryForList( String.class, ...); 支持
	 */
	private final static StringListHandler		STRINGLIST_HANDLER	= new StringListHandler();

	/**
	 * 重写dbutils中的ScalarHandler中的handle方法，处理BigInt型数据为Long型 注意：这里处理BigInteger 和 BigDecimal 方式已经转移到 apache dbutils 源码中了，所以注释了处理代码段。
	 */
	private final static ScalarHandler			SCALAR_HANDLER		= new ScalarHandler();
	// {
	// @Override
	// public Object handle(ResultSet rs) throws SQLException
	// {
	// Object obj = super.handle(rs);
	// if (obj instanceof BigInteger)
	// {
	// return ((BigInteger) obj).longValue();
	// }
	// else if (obj instanceof BigDecimal)
	// {
	// return ((BigDecimal) obj).longValue();
	// }
	//
	// return obj;
	// }
	// };

	/**
	 * 基本数据类型定义
	 */
	private final static Set<Class<?>>			PRIMITIVE_CLASSES	= new HashSet<Class<?>>()
																	{
																		private static final long	serialVersionUID	= -7678425758902391103L;
																		{
																			add(Long.class);
																			add(Integer.class);
																			add(String.class);
																			add(java.util.Date.class);
																			add(java.sql.Date.class);
																			add(java.sql.Timestamp.class);
																			add(Object.class); // 将Object定义为基本数据类型是为了不进入BeanHandler处理
																		}
																	};

	/**
	 * 创建一个JdbcHandler对象，用于静态数据源和动态数据源
	 * 
	 * @param ds 数据源配置对象
	 */
	public JdbcHandlerImpl(DataSource ds)
	{
		this.ds = ds;
	}

	/**
	 * 判断一个Class是否是基本数据类型
	 * 
	 * @param cls 要进行判断的类
	 * @return 是否是基本数据类型
	 */
	private final static boolean isPrimitiveClass(Class<?> cls)
	{
		return cls.isPrimitive() || PRIMITIVE_CLASSES.contains(cls);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#query(java.lang.String, com.lizard.fastdb.jdbc.RowCallbackHandler, java.lang.Object[])
	 */
	public <T> T query(String sql, RowCallbackHandler<T> rch, Object... paramValues)
	{
		printSQL(sql, paramValues);

		Connection conn = getConnection();
		try
		{
			return (T) QUERY_RUNNER.query(conn, sql, rch, paramValues);
		}
		catch (SQLException e)
		{
			throw new DBException("Failed to execute [ " + JdbcUtil.fillSQL(sql, paramValues) + " ] by RowCallbackHandler<T>!", e);
		}
		finally
		{
			close(conn);
		}
	}

	/* (non-Javadoc)
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#query(java.lang.String, com.lizard.fastdb.jdbc.RowCallbackHandler, java.util.Map)
	 */
	public <T> T query(String namedSql, RowCallbackHandler<T> rch, Map<String, Object> paramValues)
	{
		printNamedSQL(namedSql, paramValues);
		
		NamedSQL namedSQL = NamedSQL.parse(namedSql);
		
		return query(namedSQL.getSQL(), rch, namedSQL.parseParameterValue(paramValues));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForBean(java.lang.Class, java.lang.String)
	 */
	public <T> T queryForBean(Class<T> beanClass, String sql)
	{
		return queryForBean(beanClass, sql, new Object[]{});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForBean(java.lang.Class, java.lang.String, java.lang.Object[])
	 */
	@SuppressWarnings("unchecked")
	public <T> T queryForBean(Class<T> beanClass, String sql, Object... paramValues)
	{
		printSQL(sql, paramValues);

		Connection conn = getConnection();
		try
		{
			return (T) QUERY_RUNNER.query(conn, sql, isPrimitiveClass(beanClass) ? SCALAR_HANDLER : new BeanHandler<T>(beanClass), paramValues);
		}
		catch (SQLException e)
		{
			throw new DBException("Failed to query For Bean<" + beanClass.getName() + "> by [ " + JdbcUtil.fillSQL(sql, paramValues)
					+ " ]!", e);
		}
		finally
		{
			close(conn);
		}
	}

	/* (non-Javadoc)
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForBean(java.lang.Class, java.lang.String, java.util.Map)
	 */
	public <T> T queryForBean(Class<T> beanClass, String namedSql, Map<String, Object> paramValues)
	{
		printNamedSQL(namedSql, paramValues);
		
		NamedSQL namedSQL = NamedSQL.parse(namedSql);
		return queryForBean(beanClass, namedSQL.getSQL(), namedSQL.parseParameterValue(paramValues));
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForInteger(java.lang.String)
	 */
	public Integer queryForInteger(String sql)
	{
		return queryForInteger(sql, new Object[]{});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForInteger(java.lang.String, java.lang.Object[])
	 */
	public Integer queryForInteger(String sql, Object... paramValues)
	{
		try
		{
			Object intObj = queryForObject(sql, paramValues);

			if (intObj != null)
			{
				return Integer.parseInt(intObj.toString());
			}
			else
			{
				return null;
			}
		}
		catch (ClassCastException e)
		{
			throw new DBException("Failed to query for Integer value by [" + JdbcUtil.fillSQL(sql, paramValues) + "]!", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForInteger(java.lang.String, java.util.Map)
	 */
	public Integer queryForInteger(String namedSql, Map<String, Object> paramValues)
	{
		printNamedSQL(namedSql, paramValues);
		
		NamedSQL namedSQL = NamedSQL.parse(namedSql);
		return queryForInteger(namedSQL.getSQL(), namedSQL.parseParameterValue(paramValues));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForLong(java.lang.String)
	 */
	public Long queryForLong(String sql)
	{
		return queryForLong(sql, new Object[]{});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForLong(java.lang.String, java.lang.Object[])
	 */
	public Long queryForLong(String sql, Object... paramValues)
	{
		try
		{
			Object longObj = queryForObject(sql, paramValues);

			if (longObj != null)
			{
				return Long.parseLong(longObj.toString());
			}
			else
			{
				return 0L;
			}
		}
		catch (ClassCastException e)
		{
			throw new DBException("Failed to query for Long value by [ " + JdbcUtil.fillSQL(sql, paramValues) + " ]", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForLong(java.lang.String, java.util.Map)
	 */
	public Long queryForLong(String namedSql, Map<String, Object> paramValues)
	{
		printNamedSQL(namedSql, paramValues);
		
		NamedSQL namedSQL = NamedSQL.parse(namedSql);
		return queryForLong( namedSQL.getSQL(), namedSQL.parseParameterValue(paramValues) );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForMap(java.lang.String)
	 */
	public Map<String, Object> queryForMap(String sql)
	{
		return queryForMap(sql, new Object[]{});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForMap(java.lang.String, java.lang.Object[])
	 */
	public Map<String, Object> queryForMap(String sql, Object... paramValues)
	{
		printSQL(sql, paramValues);

		Connection conn = getConnection();
		try
		{
			return (Map<String, Object>) QUERY_RUNNER.query(conn, sql, new MapHandler(), paramValues);
		}
		catch (SQLException e)
		{
			throw new DBException("Failed to query for Map by [ " + JdbcUtil.fillSQL(sql, paramValues) + " ]", e);
		}
		finally
		{
			close(conn);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForMap(java.lang.String, java.util.Map)
	 */
	public Map<String, Object> queryForMap(String namedSql, Map<String, Object> paramValues)
	{
		printNamedSQL(namedSql, paramValues);
		
		NamedSQL namedSQL = NamedSQL.parse(namedSql);
		return queryForMap(namedSQL.getSQL(), namedSQL.parseParameterValue(paramValues));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForObject(java.lang.String)
	 */
	public Object queryForObject(String sql)
	{
		return queryForObject(sql, new Object[]{});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForObject(java.lang.String, java.lang.Object[])
	 */
	public Object queryForObject(String sql, Object... paramValues)
	{
		printSQL(sql, paramValues);

		Connection conn = getConnection();
		try
		{
			return QUERY_RUNNER.query(conn, sql, SCALAR_HANDLER, paramValues);
		}
		catch (SQLException e)
		{
			throw new DBException("Failed to query for Object by [ " + JdbcUtil.fillSQL(sql, paramValues) + " ]", e);
		}
		finally
		{
			close(conn);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForObject(java.lang.String, java.util.Map)
	 */
	public Object queryForObject(String namedSql, Map<String, Object> paramValues)
	{
		printNamedSQL(namedSql, paramValues);
		
		NamedSQL namedSQL = NamedSQL.parse(namedSql);
		return queryForObject(namedSQL.getSQL(), namedSQL.parseParameterValue(paramValues));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForList(java.lang.Class, java.lang.String)
	 */
	public <T> List<T> queryForList(Class<T> beanClass, String sql)
	{
		return queryForList(beanClass, sql, new Object[]{});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForList(java.lang.Class, java.lang.String, java.lang.Object[])
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> queryForList(Class<T> beanClass, String sql, Object... paramValues)
	{
		printSQL(sql, paramValues);

		Connection conn = getConnection();
		try
		{
			return (List<T>) QUERY_RUNNER
					.query(conn, sql, isPrimitiveClass(beanClass) ? ("java.lang.String".equals(beanClass.getName()) ? STRINGLIST_HANDLER
							: COLUMNLIST_HANDLER) : new BeanListHandler<T>(beanClass), paramValues);
		}
		catch (SQLException e)
		{
			throw new DBException(
					"Failed to query for List<" + beanClass.getName() + "> by [ " + JdbcUtil.fillSQL(sql, paramValues) + " ]", e);
		}
		finally
		{
			close(conn);
		}
	}

	/* (non-Javadoc)
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForList(java.lang.Class, java.lang.String, java.util.Map)
	 */
	public <T> List<T> queryForList(Class<T> beanClass, String namedSql, Map<String, Object> paramValues)
	{
		printNamedSQL(namedSql, paramValues);
		
		NamedSQL namedSQL = NamedSQL.parse(namedSql);
		return queryForList(beanClass, namedSQL.getSQL(), namedSQL.parseParameterValue(paramValues));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForPageList(java.lang.Class, java.lang.String, int, int, java.lang.Object[])
	 */
	public <T> List<T> queryForPageList(Class<T> beanClass, String sql, int pageno, int pagesize, Object... paramValues)
	{
		// 根据方言获取完整的分页sql语句
		String pagerSql = Dialect.createPageSQL(ds.getDialect(), sql, pageno, pagesize);

		return queryForList(beanClass, pagerSql, paramValues);
	}
	
	/* (non-Javadoc)
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForPageList(java.lang.Class, java.lang.String, int, int, java.util.Map)
	 */
	public <T> List<T> queryForPageList(Class<T> beanClass, String namedSql, int page, int pagesize,
			Map<String, Object> paramValues)
	{
		printNamedSQL(namedSql, paramValues);
		
		NamedSQL namedSQL = NamedSQL.parse(namedSql);
		return queryForPageList(beanClass, namedSQL.getSQL(), page, pagesize, namedSQL.parseParameterValue(paramValues));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForList(java.lang.String)
	 */
	public List<Map<String, Object>> queryForList(String sql)
	{
		return queryForList(sql, new Object[]{});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForList(java.lang.String, java.lang.Object[])
	 */
	public List<Map<String, Object>> queryForList(String sql, Object... paramValues)
	{
		printSQL(sql, paramValues);

		Connection conn = getConnection();
		try
		{
			return (List<Map<String, Object>>) QUERY_RUNNER.query(conn, sql, new MapListHandler(), paramValues);
		}
		catch (SQLException e)
		{
			throw new DBException("Failed to query for List<Map> by [ " + JdbcUtil.fillSQL(sql, paramValues) + " ]!", e);
		}
		finally
		{
			close(conn);
		}
	}

	/* (non-Javadoc)
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForList(java.lang.String, java.util.Map)
	 */
	public List<Map<String, Object>> queryForList(String namedSql, Map<String, Object> paramValues)
	{
		printNamedSQL(namedSql, paramValues);
		
		NamedSQL namedSQL = NamedSQL.parse(namedSql);
		return queryForList(namedSQL.getSQL(), namedSQL.parseParameterValue(paramValues));
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForPageList(java.lang.String, int, int, java.lang.Object[])
	 */
	public List<Map<String, Object>> queryForPageList(String sql, int pageno, int pagesize, Object... paramValues)
	{
		// 获取完整的分页sql语句
		String pagerSql = Dialect.createPageSQL(ds.getDialect(), sql, pageno, pagesize);

		return queryForList(pagerSql, paramValues);
	}
	
	/* (non-Javadoc)
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryForPageList(java.lang.String, int, int, java.util.Map)
	 */
	@Override
	public List<Map<String, Object>> queryForPageList(String namedSql, int page, int pagesize,
			Map<String, Object> paramValues)
	{
		printNamedSQL(namedSql, paramValues);
		
		NamedSQL namedSQL = NamedSQL.parse(namedSql);
		return queryForPageList(namedSQL.getSQL(), page, pagesize, namedSQL.parseParameterValue(paramValues));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#execute(java.lang.String)
	 */
	public boolean execute(String sql)
	{
		return execute(sql, new Object[]{});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#execute(java.lang.String, java.lang.Object[])
	 */
	public boolean execute(String sql, Object... paramValues)
	{
		Connection conn = getConnection();
		try
		{
			return execute(conn, sql, paramValues);
		}
		finally
		{
			close(conn);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#execute(java.lang.String, java.util.Map)
	 */
	public boolean execute(String namedSql, Map<String, Object> paramValues)
	{
		printNamedSQL(namedSql, paramValues);
		
		NamedSQL namedSQL = NamedSQL.parse(namedSql);
		return execute(namedSQL.getSQL(), namedSQL.parseParameterValue(paramValues));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#execute(java.sql.Connection, java.lang.String, java.lang.Object[])
	 */
	public boolean execute(Connection conn, String sql, Object... paramValues)
	{
		printSQL(sql, paramValues);

		try
		{
			QUERY_RUNNER.update(conn, sql, paramValues);

			return true;
		}
		catch (SQLException e)
		{
			logger.error("Failed to execute [ " + JdbcUtil.fillSQL(sql, paramValues) + " ]!", e);
			// 如果处于事务管理，则抛出异常，用于捕获
			if (ConnectionPool.isInTransaction())
			{
				throw new DBException("Failed to execute [ " + JdbcUtil.fillSQL(sql, paramValues) + " ]!", e);
			}
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#execute(java.sql.Connection, java.lang.String, java.util.Map)
	 */
	public boolean execute(Connection conn, String namedSql, Map<String, Object> paramValues)
	{
		printNamedSQL(namedSql, paramValues);
		
		NamedSQL namedSQL = NamedSQL.parse(namedSql);
		return execute(conn, namedSQL.getSQL(), namedSQL.parseParameterValue(paramValues));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#save(java.lang.Object)
	 */
	public boolean save(Object bean)
	{
		Object[] bean_sql = PersistenceUtil.createInsertSQL(bean);
		if (bean_sql == null || bean_sql.length == 0)
		{
			return false;
		}

		return execute(bean_sql[0].toString(), (Object[]) bean_sql[1]);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#update(java.lang.Object)
	 */
	public boolean update(Object bean)
	{
		return update(bean, null);
		
		/*Object[] bean_sql = PersistenceUtil.createUpdateSQL(bean);
		if (bean_sql == null || bean_sql.length == 0)
		{
			return false;
		}

		return execute(bean_sql[0].toString(), (Object[]) bean_sql[1]);*/
	}
	
	/* (non-Javadoc)
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#update(java.lang.Object, java.lang.String[])
	 */
	public boolean update(Object bean, String[] updatedFields)
	{
		Object[] bean_sql = PersistenceUtil.createUpdateSQL(bean, updatedFields);
		if (bean_sql == null || bean_sql.length == 0)
		{
			return false;
		}

		return execute(bean_sql[0].toString(), (Object[]) bean_sql[1]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#saveForGeneratedKey(java.lang.String)
	 */
	public long saveForGeneratedKey(String sql)
	{
		return saveForGeneratedKey(sql, new Object[]{});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#saveForGeneratedKey(java.lang.String, java.lang.Object[])
	 */
	public long saveForGeneratedKey(String sql, Object... paramValues)
	{
		printSQL(sql, paramValues);

		Connection conn = getConnection();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		long keyValue = -1;
		try
		{
			pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			if (paramValues != null && paramValues.length > 0)
			{
				QUERY_RUNNER.fillStatement(pstmt, paramValues);
			}
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (rs.next())
			{
				keyValue = rs.getLong(1);
			}
		}
		catch (SQLException e)
		{
			throw new DBException("Failed to save for generated key by [ " + JdbcUtil.fillSQL(sql, paramValues) + " ]!", e);
		}
		finally
		{
			close(rs);
			close(pstmt);
			close(conn);
		}

		return keyValue;
	}
	
	/* (non-Javadoc)
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#saveForGeneratedKey(java.lang.String, java.util.Map)
	 */
	public long saveForGeneratedKey(String namedSql, Map<String, Object> paramValues)
	{
		printNamedSQL(namedSql, paramValues);
		
		NamedSQL namedSQL = NamedSQL.parse(namedSql);
		return saveForGeneratedKey(namedSQL.getSQL(), namedSQL.parseParameterValue(paramValues));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#callProcedure(java.lang.String)
	 */
	public boolean callProcedure(String procedureName)
	{
		return callProcedure(procedureName, new Object[]{});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#callProcedure(java.lang.String, java.lang.Object[])
	 */
	public boolean callProcedure(String procedureName, Object... paramValues)
	{
		boolean result = false;
		// 存储过程的connection不归入事务管理，此处直接冲连接池获得连接
		Connection conn = ConnectionPool.getIndependConnection(this.ds.getName());
		CallableStatement cstmt = null;
		try
		{
			// 语法：{ call procedure_name[(?, ?, ...)] }
			// 组织调用参数
			String sql = JdbcUtil.joinCallSQL(procedureName, null, paramValues);

			printSQL(sql, paramValues);

			// 调用存储过程
			cstmt = conn.prepareCall(sql);
			// 设置参数
			if (paramValues != null && paramValues.length > 0)
			{
				for (int i = 0; i < paramValues.length; i++)
				{
					cstmt.setObject(i + 1, paramValues[i]);
				}
			}
			// 执行存储过程
			cstmt.execute();
			sql = null;
			result = true;
		}
		catch (SQLException e)
		{
			logger.error("Failed to call procedure[" + procedureName + "]"
					+ (paramValues != null ? (" by params [" + Arrays.deepToString(paramValues) + "]") : ""), e);
			// 如果出于事务处理，则抛出异常用于捕获
			if (ConnectionPool.isInTransaction())
			{
				throw new DBException("Failed to call procedure[" + procedureName + "]"
						+ (paramValues != null ? (" by params [" + Arrays.deepToString(paramValues) + "]") : ""), e);
			}
		}
		finally
		{
			close(cstmt);
			// 存储过程关闭Connection不归入事务管理，直接调用连接池关闭
			ConnectionPool.closeIndependConnection(this.ds.getName(), conn);
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#callProcedureForResult(java.lang.String)
	 */
	public String callProcedureForResult(String procedureName)
	{
		return callProcedureForResult(procedureName, (Object[]) null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#callProcedureForResult(java.lang.String, java.lang.Object[])
	 */
	public String callProcedureForResult(String procedureName, Object... paramValues)
	{
		return (String) callProcedureForResult(procedureName, Types.VARCHAR, paramValues);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#callProcedureForResult(java.lang.String, int)
	 */
	public Object callProcedureForResult(String procedureName, int sqlType)
	{
		return callProcedureForResult(procedureName, sqlType, (Object[]) null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#callProcedureForResult(java.lang.String, int, java.lang.Object[])
	 */
	public Object callProcedureForResult(String procedureName, int sqlType, Object... paramValues)
	{
		return callProcedureForResult(procedureName, new int[]{ sqlType }, paramValues).get(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#callProcedureForResult(java.lang.String, int, int, java.lang.Object[])
	 */
	public Object callProcedureForResult(String procedureName, int sqlType, int sqlTypeIndex, Object... paramValues)
	{
		return callProcedureForResult(procedureName, new int[]{ sqlType }, new int[]{ sqlTypeIndex }, paramValues).get(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#callProcedureForResult(java.lang.String, int[], java.lang.Object[])
	 */
	public List<Object> callProcedureForResult(String procedureName, int[] sqlTypes, Object... paramValues)
	{
		int[] sql_index = new int[sqlTypes.length];
		for (int i = 0; i < sql_index.length; i++)
		{
			sql_index[i] = i;
		}
		return callProcedureForResult(procedureName, sqlTypes, sql_index, paramValues);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#callProcedureForResult(java.lang.String, int[], int[], java.lang.Object[])
	 */
	public List<Object> callProcedureForResult(String procedureName, int[] sqlTypes, int[] sqlTypesIndex, Object... paramValues)
	{
		// 存储过程的connection不归入事务管理，此处直接从连接池获得连接
		Connection conn = ConnectionPool.getIndependConnection(this.ds.getName());
		if (conn == null)
		{
			throw new DBException("Get connection fail!");
		}

		// sqlTypes 与 sqlTypesIndex 不一致
		if ((sqlTypes != null && sqlTypesIndex == null) || (sqlTypes == null && sqlTypesIndex != null)
				|| (sqlTypes != null && sqlTypesIndex != null && sqlTypes.length != sqlTypesIndex.length))
		{
			throw new DBException("[sqlTypes] and [sqlTypesIndex] is not corresponding!");
		}

		List<Object> list = new ArrayList<Object>();

		CallableStatement cstmt = null;
		try
		{
			// 组织调用语句
			String sql = JdbcUtil.joinCallSQL(procedureName, sqlTypes == null ? new int[]{} : sqlTypes, paramValues);
			cstmt = conn.prepareCall(sql);

			// 参数个数
			int paramCount = (sqlTypes == null ? 0 : sqlTypes.length) + (paramValues == null ? 0 : paramValues.length);

			// 最终参数集
			Object[] final_params = new Object[paramCount];
			Set<Integer> i_set = new HashSet<Integer>();
			// 设置out参数在所有参数的位置
			if (sqlTypes != null && sqlTypesIndex != null)
			{
				for (int i = 0; i < sqlTypesIndex.length; i++)
				{
					i_set.add(sqlTypesIndex[i]);
					final_params[sqlTypesIndex[i]] = sqlTypes[i];
				}
			}

			// 设置in参数在所有参数的位置
			if (paramValues != null)
			{
				int p_i = 0;
				for (int i = 0; i < paramCount; i++)
				{
					if (final_params[i] == null)
					{
						final_params[i] = paramValues[p_i++];
					}
				}
			}

			printSQL(sql, final_params);

			if ((sqlTypes != null && sqlTypesIndex != null) || paramValues != null)
			{
				for (int i = 0; i < final_params.length; i++)
				{
					// 注册out参数
					if (i_set.contains(i))
					{
						cstmt.registerOutParameter(i + 1, (Integer) final_params[i]);
					}
					// 设置 in 参数
					else
					{
						cstmt.setObject(i + 1, final_params[i]);
					}
				}
			}

			cstmt.execute();

			if (sqlTypes != null && sqlTypesIndex != null)
			{
				for (int i = 0; i < sqlTypes.length; i++)
				{
					list.add(cstmt.getObject(sqlTypesIndex[i] + 1));
				}
			}
			else
			{
				list.add(cstmt.getObject(1));
			}

			sql = null;
		}
		catch (SQLException e)
		{
			logger.error("Failed to call procedure [ " + procedureName + " ]");
			throw new DBException("Failed to call procedure [ " + procedureName + " ]", e);
		}
		finally
		{
			close(cstmt);
			// 存储过程关闭Connection不归入事务管理，直接调用连接池关闭
			ConnectionPool.closeIndependConnection(this.ds.getName(), conn);
		}

		return list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#batchUpdate(java.lang.String[])
	 */
	public BatchUpdateResult batchUpdate(String[] sqls)
	{
		return batchUpdate(sqls, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#batchUpdate(java.lang.String[], boolean)
	 */
	@SuppressWarnings("resource")
	public BatchUpdateResult batchUpdate(String[] sqls, boolean rollback)
	{
		if (sqls == null || sqls.length <= 0)
		{
			throw new DBException("BatchUpdate sql array can not be empty!");
			// return new BatchUpdateResult(true, new int[0], new int[0]);
		}

		Connection conn = getConnection();

		if (conn == null)
		{
			throw new DBException("Get connection fail!");
			// return new BatchUpdateResult(false, new int[0], new int[0]);
		}

		Statement stmt = null;

		// 将所有SQL语句分为 totalBatchs 批执行，如果设置了回滚或设置了每批的大小，则分批提交
		int totalBatchs = getTotalBatchs(sqls.length);
		// 最终的执行结果是否成功，只有当所有批都执行没有任何错误时，才为ture
		boolean _success = true;
		// 执行失败的批编号
		List<Integer> errorBatchList = new ArrayList<Integer>();
		// 当前批执行成功的每条SQL语句的影响计数
		int[] urows = null;
		// 存储分批操作中的executeBatch 执行影响的计数，便于最后计算全局的计数数组
		List<Integer> rowList = new ArrayList<Integer>();
		// 当前线程 batchSize 值
		int batchSize = BATCH_SIZE_LOCAL.get();

		// 如果当前操作在事务管理器中，则将回滚置为true，便于出错后事务处理器进行统一回滚操作
		// SHEN.GANG 于 2014-08-12 增加
		if (ConnectionPool.isInTransaction())
		{
			rollback = true;
		}

		try
		{
			// 如果需要回滚 或者 需要分批批量操作
			if (rollback || totalBatchs > 0)
			{
				conn.setAutoCommit(false);
			}

			stmt = conn.createStatement();

			// 已经执行的批数计数
			int run_batch = 0;
			for (int i = 0; i < sqls.length; i++)
			{
				if (sqls[i] != null && sqls[i].trim().length() > 0)
				{
					printSQL(sqls[i], null);
					stmt.addBatch(sqls[i]);
				}

				// 到达一个批次，则提交
				if (totalBatchs > 0 && (i + 1) % batchSize == 0)
				{
					try
					{
						urows = stmt.executeBatch();
						// 如果当前操作没有包含在事务管理器中，则按批次提交，否则，由事务管理器统一提交
						if (!ConnectionPool.isInTransaction())
						{
							conn.commit();
						}
					}
					catch (SQLException e)
					{
						// 该批次回滚
						if (rollback)
						{
							// 如果当前操作没有包含在事务管理器中，则按批次回滚，否则，由事务管理器统一回滚
							if (!ConnectionPool.isInTransaction())
							{
								conn.rollback();
							}
							// 如果当前 batchUpdate 处在事务管理器中，则抛出异常让事务管理器捕获，由事务管理器负责统一回滚
							// SHEN.GANG 于 2014-07-12 增加
							else
							{
								logger.error("Failed to execute the batchUpdate sqls, [totalBatchs = " + totalBatchs + ", batchSize = "
										+ batchSize + ", index = " + i + "]!", e);
								throw e;
							}
						}
						// 记录失败
						_success = false;

						urows = ((BatchUpdateException) e).getUpdateCounts();

						// 记录执行失败的批
						errorBatchList.add(run_batch + 1);
					}

					// 记录执行命令的计数
					addBatchRows(urows, rowList);

					run_batch++;
				}
				// 循环到最后，如果未分批或最后的一批SQL还为执行
				else if (i == sqls.length - 1 && (totalBatchs <= 0 || run_batch < totalBatchs))
				{
					try
					{
						// 提交未到达批次的操作（或者未分批的所有操作）
						urows = stmt.executeBatch();

						// 需要回滚 或者 执行分批操作
						if (rollback || totalBatchs > 0)
						{
							// 如果当前操作没有包含在事务管理器中，则按批次提交，否则，由事务管理器统一提交
							if (!ConnectionPool.isInTransaction())
							{
								conn.commit();
							}
						}
					}
					catch (SQLException e)
					{
						// 该批次回滚
						if (rollback)
						{
							// 如果当前操作没有包含在事务管理器中，则按批次回滚，否则，由事务管理器统一回滚
							if (!ConnectionPool.isInTransaction())
							{
								conn.rollback();
							}
							// 如果当前 batchUpdate 处在事务管理器中，则抛出异常让事务管理器捕获，由事务管理器负责统一回滚
							// SHEN.GANG 于 2014-07-12 增加
							else
							{
								logger.error("Failed to execute the batchUpdate sqls, [totalBatchs = " + totalBatchs + ", batchSize = "
										+ batchSize + ", index = " + i + "]!", e);
								throw e;
							}
						}
						// 记录失败
						_success = false;

						urows = ((BatchUpdateException) e).getUpdateCounts();

						// 记录失败的批次
						errorBatchList.add(run_batch + 1);
					}

					// 记录执行命令的计数
					addBatchRows(urows, rowList);

					run_batch++;
				}
			}

			// 返回最终的计数数组
			int[] _rows = new int[rowList.size()];
			for (int j = 0; j < rowList.size(); j++)
			{
				_rows[j] = rowList.get(j);
			}

			return initBatchUpdateResult(_success, _rows, errorBatchList);
		}
		catch (Exception e)
		{
			logger.error("Failed to execute the batchUpdate sqls " + Arrays.deepToString(sqls) + "!", e);
			throw new DBException("Failed to execute the batchUpdate sqls " + Arrays.deepToString(sqls) + "!", e);
		}
		finally
		{
			close(stmt);
			close(conn);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#batchUpdate(java.lang.String, java.lang.Object[][])
	 */
	public BatchUpdateResult batchUpdate(String sql, Object[][] paramValues)
	{
		return batchUpdate(sql, paramValues, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#batchUpdate(java.lang.String, java.lang.Object[][], boolean)
	 */
	public BatchUpdateResult batchUpdate(String sql, Object[][] paramValues, boolean rollback)
	{
		Connection conn = getConnection();
		if (conn == null)
		{
			throw new DBException("Get connection fail!");
		}

		PreparedStatement pstmt = null;

		if (StringUtils.isEmptyString(sql))
		{
			throw new DBException("BatchUpdate sql can not be empty!");
		}

		if (paramValues == null)
		{
			throw new DBException("BatchUpdate paramValues[][] can not be null!");
		}

		// 当前线程 batchSize 的值
		int batchSize = BATCH_SIZE_LOCAL.get();
		// 分批次数
		int totalBatchs = getTotalBatchs(paramValues.length);
		// 已经执行的批次
		int run_batch = 0;

		// 每次 executeBatch 执行是否成功，只有当全部成功时，才算是成功
		boolean _success = true;
		// 执行失败的批次
		List<Integer> errorBatchList = new ArrayList<Integer>();
		// executeBatch 执行影响的计数
		int[] urows = null;
		// 存储分批操作中的executeBatch 执行影响的计数，便于最后计算全局的计数数组
		List<Integer> rowList = new ArrayList<Integer>();

		// 如果当前操作在事务管理器中，则默认将回滚置为true，便于事务处理器进行统一回滚操作
		// SHEN.GANG 于 2014-07-12 增加
		if (ConnectionPool.isInTransaction())
		{
			rollback = true;
		}

		try
		{
			// 需要回滚 或者 分批次操作
			if (rollback || totalBatchs > 0)
			{
				conn.setAutoCommit(false);
			}

			pstmt = conn.prepareStatement(sql);

			if (paramValues != null && paramValues.length > 0)
			{
				for (int i = 0; i < paramValues.length; i++)
				{
					// 为预编译SQL语句赋值
					Object[] param = paramValues[i];
					if (param != null && param.length > 0)
					{
						printSQL(sql, param);
						QUERY_RUNNER.fillStatement(pstmt, param);
					}
					pstmt.addBatch();

					// 分批次提交
					if (totalBatchs > 0 && (i + 1) % batchSize == 0)
					{
						try
						{
							urows = pstmt.executeBatch();
							// 如果当前操作没有包含在事务管理器中，则按批次提交，否则，由事务管理器统一提交
							if (!ConnectionPool.isInTransaction())
							{
								conn.commit();
							}
						}
						catch (SQLException e)
						{
							// 该批次回滚
							if (rollback)
							{
								// 如果当前操作没有包含在事务管理器中，则按批次回滚，否则，由事务管理器统一回滚
								if (!ConnectionPool.isInTransaction())
								{
									conn.rollback();
								}
								// 如果当前 batchUpdate 处在事务管理器中，则抛出异常让事务管理器捕获，由事务管理器负责统一回滚
								// SHEN.GANG 于 2014-07-12 增加
								else
								{
									logger.error("Failed to execute the batchUpdate sql, [totalBatchs = " + totalBatchs + ", batchSize = "
											+ batchSize + ", index = " + i + "]!", e);
									throw new BatchUpdateException(e);
								}
							}
							// 记录失败
							_success = false;

							urows = ((BatchUpdateException) e).getUpdateCounts();
							// 记录执行失败的批次
							errorBatchList.add(run_batch + 1);
						}

						// 记录执行命令的计数
						addBatchRows(urows, rowList);

						run_batch++;
					}
					else if (i == paramValues.length - 1 && (totalBatchs <= 0 || run_batch < totalBatchs))
					{
						try
						{
							// 提交未到达批次的操作（或者未分批的所有操作）
							urows = pstmt.executeBatch();

							// 需要回滚 或者 执行分批操作
							if (rollback || totalBatchs > 0)
							{
								// 如果当前操作没有包含在事务管理器中，则按批次提交，否则，由事务管理器统一提交
								if (!ConnectionPool.isInTransaction())
								{
									conn.commit();
								}
							}
						}
						catch (SQLException e)
						{
							// 该批次回滚
							if (rollback)
							{
								// 如果当前操作没有包含在事务管理器中，则按批次回滚，否则，由事务管理器统一回滚
								if (!ConnectionPool.isInTransaction())
								{
									conn.rollback();
								}
								// 如果当前 batchUpdate 处在事务管理器中，则抛出异常让事务管理器捕获，由事务管理器负责统一回滚
								// SHEN.GANG 于 2014-07-12 增加
								else
								{
									logger.error("Failed to execute the batchUpdate sql, [totalBatchs = " + totalBatchs + ", batchSize = "
											+ batchSize + ", index = " + i + "]!", e);
									throw new BatchUpdateException(e);
								}
							}
							// 记录失败
							_success = false;

							urows = ((BatchUpdateException) e).getUpdateCounts();

							// 记录失败的批次
							errorBatchList.add(run_batch + 1);
						}
						// 记录执行命令的计数
						addBatchRows(urows, rowList);
					}
				}
			}

			// 返回最终的计数数组
			int[] _rows = new int[rowList.size()];
			for (int j = 0; j < rowList.size(); j++)
			{
				_rows[j] = rowList.get(j);
			}

			return initBatchUpdateResult(_success, _rows, errorBatchList);
		}
		catch (Exception e)
		{
			logger.error("Can't execute the batchUpdate SQL[" + sql + "] by params" + Arrays.deepToString((Object[]) paramValues), e);
			throw new DBException("Can't execute the batchUpdate SQL[" + sql + "] by params" + Arrays.deepToString((Object[]) paramValues),
					e);
		}
		finally
		{
			close(pstmt);
			close(conn);
		}
	}

	/**
	 * 计算总分批次数
	 * 
	 * @param totals 数据总数
	 * @return 总分批次数
	 */
	private int getTotalBatchs(int totals)
	{
		int batchSize = BATCH_SIZE_LOCAL.get();
		if (batchSize <= 0 || totals <= 0)
		{
			return 0;
		}
		else
		{
			return totals % batchSize == 0 ? totals / batchSize : totals / batchSize + 1;
		}
	}

	/**
	 * 统计批量操作成功执行命令的计数
	 * 
	 * @param rows
	 * @param rowList
	 */
	private void addBatchRows(int[] rows, List<Integer> rowList)
	{
		if (rows != null && rows.length > 0)
		{
			for (int i = 0; i < rows.length; i++)
			{
				rowList.add(rows[i]);
			}
		}
	}

	/**
	 * 根据执行结果初始化BatchUpdateResult对象
	 * 
	 * @param success 是否全部执行成功
	 * @param rows 执行结果
	 * @param errorBatchList 错误批次列表
	 * @return 批量更新结果BatchUpdateResult
	 */
	private BatchUpdateResult initBatchUpdateResult(boolean success, int[] rows, List<Integer> errorBatchList)
	{
		// 批量更新返回的结果信息
		BatchUpdateResult bur = new BatchUpdateResult();
		// 设置批量处理最终的处理结果
		bur.setSuccess(success);
		bur.setBatchRows(rows);
		// 保存执行失败的批次
		if (errorBatchList.size() > 0)
		{
			int[] errorBatchs = new int[errorBatchList.size()];
			for (int t = 0; t < errorBatchList.size(); t++)
			{
				errorBatchs[t] = errorBatchList.get(t);
			}
			bur.setErrorBatchs(errorBatchs);
		}

		return bur;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#setBatchSize(int)
	 */
	public void setBatchSize(int batchSize)
	{
		if (batchSize < 0)
		{
			throw new IllegalArgumentException("The batchSize must be greater than or equal to 0!");
		}
		BATCH_SIZE_LOCAL.set(batchSize);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#isTableExist(java.lang.String)
	 */
	public boolean isTableExist(String tableName)
	{
		// 表名为空
		if (StringUtils.isEmptyString(tableName))
		{
			return false;
		}

		String sql = null;
		Object[] params = null;
		if (Dialect.MYSQL.equals(this.ds.getDialect()))// MySQL
		{
			sql = "SELECT count(1) FROM information_schema.tables WHERE table_name = ? AND table_schema = ?";
			params = new Object[]{ tableName.toLowerCase(), JdbcUtil.getMySQLSchema(this.ds.getDriverUrl()) };
		}
		else
		// Oracle
		{
			sql = "SELECT count(1) FROM sys.user_tables WHERE table_name = ?";
			params = new Object[]{ tableName.toUpperCase() };
		}

		return queryForLong(sql, params) > 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#queryTableFields(java.lang.String)
	 */
	public List<String> queryTableFields(String tableName)
	{
		if (StringUtils.isEmptyString(tableName))
		{
			throw new IllegalArgumentException("TableName cannot be empty!");
		}

		String sql = null;
		Object[] params = null;

		// MySQL 数据库
		if (Dialect.MYSQL.equals(this.ds.getDialect()))
		{
			sql = "SELECT column_name FROM information_schema.columns WHERE table_name = ? AND table_schema = ?";
			params = new Object[]{ tableName.toLowerCase(), JdbcUtil.getMySQLSchema(this.ds.getDriverUrl()) };
		}
		// Oracle 数据库
		else
		{
			sql = "SELECT column_name FROM sys.all_tab_columns WHERE table_name = ? AND owner = ?";
			params = new Object[]{ tableName.toUpperCase(), ds.getUser().toUpperCase() };
		}

		return queryForList(String.class, sql, params);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#isSequenceExist(java.lang.String)
	 */
	public boolean isSequenceExist(String sequenceName)
	{
		if (StringUtils.isEmptyString(sequenceName))
		{
			return false;
		}

		String sql = "SELECT count(1) FROM sys.all_sequences WHERE sequence_name = ?";

		return queryForInteger(sql, sequenceName) > 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#isIndexExist(java.lang.String, java.lang.String)
	 */
	public boolean isIndexExist(String tableName, String indexName)
	{
		if (StringUtils.isEmptyString(tableName) || StringUtils.isEmptyString(indexName))
		{
			return false;
		}

		String sql = null;
		if (Dialect.MYSQL.equals(this.ds.getDialect()))
		{
			sql = "SELECT count(1) FROM information_schema.statistics WHERE TABLE_NAME = ? and INDEX_NAME = ? and TABLE_SCHEMA = '"
					+ JdbcUtil.getMySQLSchema(this.ds.getDriverUrl()) + "'";
		}
		else
		{
			sql = "SELECT count(1) FROM sys.user_indexes WHERE TABLE_NAME = ? and INDEX_NAME = ?";
		}

		return queryForInteger(sql, tableName, indexName) > 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#isFunctionExist(java.lang.String)
	 */
	public boolean isFunctionExist(String functionName)
	{
		if (StringUtils.isEmptyString(functionName))
		{
			return false;
		}

		String sql = "SELECT count(1) FROM sys.user_objects WHERE object_type = 'FUNCTION' AND object_name = ?";

		return queryForInteger(sql, functionName.toUpperCase()) > 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#isProcedureExist(java.lang.String)
	 */
	public boolean isProcedureExist(String procedureName)
	{
		if (StringUtils.isEmptyString(procedureName))
		{
			return false;
		}

		String sql = "SELECT count(1) FROM sys.user_objects WHERE object_type = 'PROCEDURE' AND object_name = ?";

		return queryForInteger(sql, procedureName.toUpperCase()) > 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#isConnectionReachable()
	 */
	public boolean isConnectionReachable()
	{
		Connection conn = null;

		try
		{
			Class.forName(ds.getDriverClass());
			conn = DriverManager.getConnection(ds.getDriverUrl(), ds.getUser(), ds.getPassword());
			return true;
		}
		catch (ClassNotFoundException e)
		{
			logger.error("Can not found Connection Driver Class ["+ ds.getDriverClass() +"].", e);
			return false;
		}
		catch (SQLException e)
		{
			logger.error("Can not get the Connection by["+ ds.getDriverUrl() +":"+ ds.getUser()+":"+ds.getPassword() +"].", e);
			return false;
		}
		finally
		{
			if (conn != null)
			{
				try
				{
					conn.close();
				}
				catch (SQLException e)
				{
					logger.error("Failed to close the connection in isConnectionReachable().", e);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#getDataSource()
	 */
	public DataSource getDataSource()
	{
		return DataSourceUtil.convertDataSourceToReadable(ds);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#getConnection()
	 */
	public Connection getConnection()
	{
		return ConnectionPool.getConnection(this.ds.getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#closeConnection(java.sql.Connection)
	 */
	public void close(Connection conn)
	{
		ConnectionPool.closeConnection(this.ds.getName(), conn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#close(java.sql.Statement)
	 */
	public void close(Statement stmt)
	{
		try
		{
			if (stmt != null)
			{
				stmt.close();
				stmt = null;
			}
		}
		catch (SQLException e)
		{
			logger.error("Close the Statement failed!", e);
			throw new DBException("Close the Statement failed!", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lizard.fastdb.jdbc.JdbcHandler#close(java.sql.ResultSet)
	 */
	public void close(ResultSet rs)
	{
		try
		{
			if (rs != null)
			{
				rs.close();
				rs = null;
			}
		}
		catch (SQLException e)
		{
			logger.error("Close the ResultSet failed!", e);
			throw new DBException("Close the ResultSet failed!", e);
		}
	}

	/**
	 * 打印SQL
	 * 
	 * @param sql SQL语句
	 * @param paramValues SQL语句参数
	 */
	private void printSQL(String sql, Object[] paramValues)
	{
		if (Boolean.valueOf(this.ds.getShowSQL()))
		{
			JdbcUtil.printSQL(sql, paramValues);
		}
	}
	
	/**
	 * 打印命名参数SQL语句
	 * 
	 * @param namedSql
	 * @param paramValues
	 */
	private void printNamedSQL( String namedSql, Map<String, Object> paramValues )
	{
		if (Boolean.valueOf(this.ds.getShowSQL()))
		{
			JdbcUtil.printNamedSQL(namedSql, paramValues);
		}
	}
}
