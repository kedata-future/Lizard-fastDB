package com.lizard.fastdb.connection.c3p0;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.enhydra.jdbc.standard.StandardXADataSource;

import com.lizard.fastdb.connection.ConfigureReflect;
import com.lizard.fastdb.connection.ConnectionProvider;
import com.lizard.fastdb.datasource.DataSourceUtil;
import com.mchange.v2.c3p0.C3P0Registry;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

/**
 * ConnectionProvicer 的 C3P0 实现
 * 
 * @author SHEN.GANG
 */
public class C3P0ConnectionProvider implements ConnectionProvider
{
	private static final long		serialVersionUID	= 1L;
	private static final Log		logger				= LogFactory.getLog(C3P0ConnectionProvider.class);

	private Properties				ds					= null;											// 数据源配置
	private String					name				= null;											// 数据源名称

	private ComboPooledDataSource	cpds				= null;											// 当前ConnectionProvider对应的C3P0数据源

	/**
	 * 配置连接池
	 */
	public void configure(Properties ds)
	{
		if (this.ds != null)
		{
			return;
		}

		this.ds = ds;
		this.name = this.ds.getProperty("name");

		if (!isDataSourcePooled(name))
		{
			logger.info("Initializing C3P0 pool [" + name + "]...");
			convertToC3P0DataSource();
		}
	}

	/**
	 * 从连接池中获得一个连接
	 */
	public Connection getConnection() throws SQLException
	{
		return cpds.getConnection();
	}

	/**
	 * 根据指定的数据源获得该数据源的事务Connection对象，并将其设置给指定的事务管理器管理
	 * 
	 * @param tm 事务管理器
	 * @return 由XAConnection创建的Connection对象
	 * @throws SQLException
	 */
	public Connection getXAConnection(TransactionManager tm) throws SQLException
	{
		StandardXADataSource sxd = DataSourceUtil.convertPropertiesToXADataSource(ds);
		sxd.setTransactionManager(tm);

		Connection conn = sxd.getXAConnection().getConnection();
		conn.setAutoCommit(false);

		return conn;
	}

	/**
	 * 销毁数据源
	 */
	private void destory()
	{
		if (isDataSourcePooled(name))
		{
			try
			{
				DataSources.destroy(C3P0Registry.pooledDataSourceByName(name));
				logger.info("Destroy C3P0 pool [" + name + "] success!");
			}
			catch (SQLException e)
			{
				logger.error("Failed to destroy C3P0 pool [" + name + "]!", e);
			}
		}
	}

	/**
	 * 关闭当前连接池
	 */
	public void shutdown()
	{
		logger.info("Shutdown C3P0 pool [" + name + "] ...");
		destory();

		// Set ds_set = C3P0Registry.getPooledDataSources();
		// if (ds_set != null && ds_set.size() > 0)
		// {
		// try
		// {
		// for (Object ds : ds_set)
		// {
		// logger.info("Destory c3p0 pool [" + ((PooledDataSource) ds).getDataSourceName() + "]");
		// DataSources.destroy((PooledDataSource) ds);
		// }
		// }
		// catch (SQLException e)
		// {
		// logger.error("Failed to shutdown C3P0 pool！", e);
		// }
		// }
	}

	/**
	 * 查看指定名称的连接池是否存在
	 * 
	 * @param ds_name 数据源名称
	 * @return true -- 存在，false -- 不存在
	 */
	private boolean isDataSourcePooled(String ds_name)
	{
		return C3P0Registry.pooledDataSourceByName(ds_name) != null;
	}

	/**
	 * 关闭指定的数据库连接
	 * 
	 * @param conn 要关闭的数据库连接
	 */
	public void closeConnection(Connection conn) throws SQLException
	{
		if (conn != null)
		{
			conn.close();
		}
	}

	/**
	 * 将数据源配置（properties）转换为C3P0的数据源对象
	 */
	private void convertToC3P0DataSource()
	{
		cpds = new ComboPooledDataSource();

		setAttributeMapping();
		try
		{
			ConfigureReflect.setProperties(cpds, ds);

			// 由于C3P0的设计策略是：只有当程序第一次使用Connection时，才真正的创建连接池
			// 所以这里需要根据配置采用手工方式触发创建真正连接池
			if (Boolean.valueOf(ds.getProperty("load-on-startup")))
			{
				// 获取一个连接，然后立刻关闭它，作用： 是向C3P0发送一个创建连接池信号
				closeConnection(cpds.getConnection());
			}
		}
		catch (Exception e1)
		{
			e1.printStackTrace();
		}
	}

	/**
	 * 设置属性映射，用于通过方法反射设置属性
	 */
	private void setAttributeMapping()
	{
		// 以下属性不支持
		// available-connection-size

		this.ds.setProperty("driverClass", this.ds.getProperty("driver-class"));
		this.ds.setProperty("dataSourceName", name);
		this.ds.setProperty("jdbcUrl", this.ds.getProperty("driver-url"));

		this.ds.setProperty("maxPoolSize", this.ds.getProperty("max-connection-size"));
		this.ds.setProperty("minPoolSize", this.ds.getProperty("min-connection-size"));
		this.ds.setProperty("initialPoolSize", this.ds.getProperty("init-connection-size"));
		this.ds.setProperty("maxIdleTime", this.ds.getProperty("max-connection-idletime"));
		this.ds.setProperty("maxConnectionAge", this.ds.getProperty("max-connection-lifetime"));
		this.ds.setProperty("acquireIncrement", this.ds.getProperty("acquire-increment-size"));

		this.ds.setProperty("acquireRetryDelay", this.ds.getProperty("acquire-retry-delay"));
		this.ds.setProperty("acquireRetryAttempts", this.ds.getProperty("acquire-retry-attempts"));
		this.ds.setProperty("testConnectionOnCheckin", this.ds.getProperty("test-connection-checkin"));
		this.ds.setProperty("testConnectionOnCheckout", this.ds.getProperty("test-connection-checkout"));
		this.ds.setProperty("preferredTestQuery", this.ds.getProperty("test-sql"));
		this.ds.setProperty("idleConnectionTestPeriod", this.ds.getProperty("idle-connection-test-period"));

		this.ds.setProperty("loginTimeout", this.ds.getProperty("connection-timeout"));
	}
}
