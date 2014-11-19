package com.lizard.fastdb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lizard.fastdb.datasource.DataSource;
import com.lizard.fastdb.datasource.DataSourceCache;
import com.lizard.fastdb.datasource.DataSourceManager;
import com.lizard.fastdb.datasource.DataSourceUtil;
import com.lizard.fastdb.jdbc.JdbcHandler;
import com.lizard.fastdb.jdbc.JdbcHandlerImpl;

/**
 * 数据库工厂
 * 
 * @author SHEN.GANG
 */
public final class DBFactory
{
	private static final Log	logger	= LogFactory.getLog(DBFactory.class);

	private DBFactory() { }

	/**
	 * 创建指定名称的数据源的 JdbcHandler
	 * 
	 * @param datasourceName 数据源名称
	 * @return JdbcHandler
	 */
	public static JdbcHandler create(String datasourceName)
	{
		if (datasourceName == null || datasourceName.trim().length() == 0)
		{
			logger.warn("DataSource name can't be null!");
			return null;
		}

		datasourceName = datasourceName.toLowerCase().trim();

		DataSourceManager.initConnectionProvider(datasourceName);

		return createJdbcHandler(new DataSource(DataSourceCache.getDataSource(datasourceName)));
	}

	/**
	 * 创建根据当前配置的数据源的 JdbcHandler
	 * 
	 * @param ds_name 数据源名称
	 * @param driver_class 数据库驱动类
	 * @param driver_url 数据库URL
	 * @param user 用户
	 * @param password 密码
	 * @return JdbcHandler
	 */
	public static JdbcHandler create(String ds_name, String driver_class, String driver_url, String user, String password)
	{
		return create(new DataSource(ds_name, driver_class, driver_url, user, password));
	}

	/**
	 * 创建当前数据源对象的 JdbcHandler
	 * 
	 * @param ds 动态DataSource配置
	 * @return JdbcHandler
	 */
	public static JdbcHandler create(DataSource datasource)
	{
		DataSourceManager.initConnectionProvider(DataSourceUtil.convertDataSourceToProperties(datasource));

		return createJdbcHandler(datasource);
	}

	/**
	 * 根据数据源对象创建JdbcHandler对象
	 * 
	 * @param ds 数据源配置对象
	 * @return JdbcHandler
	 */
	private static JdbcHandler createJdbcHandler(DataSource ds)
	{
		return new JdbcHandlerImpl(ds);
	}


	/**
	 * 销毁指定的数据源<br>
	 * <b>注意：该操作将彻底销毁该数据源相关的所有内存资源、物理连接等，以后不能再被使用！</b>
	 * 
	 * 1.如果传递的是真实数据源名称，将销毁对应的真实数据源；<br>
	 * 2.如果传递的是虚拟数据源名称，将销毁软连接，软连接将不可用，软连接指向的真实数据源不会被销毁；
	 * 
	 * @param ds_name 待销毁的数据源名称
	 */
	public static void destroy(String ds_name)
	{
		if (ds_name == null || ds_name.trim().length() == 0)
		{
			logger.warn("DataSource name can't be null!");
			return;
		}
		
		DataSourceManager.destroy(ds_name.toLowerCase().trim());
	}
	
	/**
	 * 释放指定数据源连接池中创建的所有物理连接，清空连接池，用于暂时缓解数据库连接资源；<br>
	 * 该数据源以后可以被继续创建使用。<br>
	 * 
	 * 1.如果传递的是真实数据源名称，将关闭对应的真实数据源；<br>
	 * 2.如果传递的是软连接虚拟数据源名称，则不作任何处理；
	 * 
	 * @param ds_name 待释放的数据源名称
	 * 
	 */
	public static void shutdown( String ds_name )
	{
		if (ds_name == null || ds_name.trim().length() == 0)
		{
			logger.warn("DataSource name can't be null!");
			return;
		}
		
		DataSourceManager.shutdown(ds_name.toLowerCase().trim());
	}
}
