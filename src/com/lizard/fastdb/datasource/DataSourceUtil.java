package com.lizard.fastdb.datasource;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;

import org.enhydra.jdbc.standard.StandardXADataSource;

import com.lizard.fastdb.config.Config;
import com.lizard.fastdb.util.StringUtils;

/**
 * 数据源操作工具类
 * 
 * @author SHEN.GANG
 */
public class DataSourceUtil
{
	/**
	 * 判断连个数据源是否相同
	 * 
	 * @param ds1
	 * @param ds2
	 * @return true -- 两个数据源配置相同，false -- 两个数据源配置不同
	 */
	protected static boolean isSameDataSource(Properties ds1, Properties ds2)
	{
		String driver_url1 = ds1.getProperty("driver-url");
		String driver_url2 = ds2.getProperty("driver-url");

		int index = driver_url1.indexOf("?");
		if (index != -1)
		{
			driver_url1 = driver_url1.substring(0, index);
		}
		index = driver_url2.indexOf("?");
		if (index != -1)
		{
			driver_url2 = driver_url2.substring(0, index);
		}
		if (!driver_url1.equals(driver_url2))
		{
			return false;
		}

		String user1 = ds1.getProperty("user");
		String user2 = ds2.getProperty("user");

		if (!user1.equals(user2))
		{
			return false;
		}

		String password1 = ds1.getProperty("password");
		String password2 = ds2.getProperty("password");

		if (!password1.equals(password2))
		{
			return false;
		}

		return true;
	}

	/**
	 * 根据Properties创建XADataSource对象
	 * 
	 * @param prop 数据源配置对象
	 * @return StandardXADataSource
	 * @throws SQLException
	 */
	public static StandardXADataSource convertPropertiesToXADataSource(Properties prop) throws SQLException
	{
		StandardXADataSource sxd = new StandardXADataSource();
		sxd.setDriverName(prop.getProperty("driver-class"));
		sxd.setUrl(prop.getProperty("driver-url"));
		sxd.setUser(prop.getProperty("user"));
		sxd.setPassword(prop.getProperty("password"));
		sxd.setMaxCon(Integer.parseInt(prop.getProperty("max-connection-size")));
		sxd.setMinCon(Integer.parseInt(prop.getProperty("min-connection-size")));
		sxd.setLoginTimeout(Integer.parseInt(prop.getProperty("connection-timeout")));
		return sxd;
	}

	/**
	 * 获得当前数据源对象Properties形式
	 * 
	 * @return Properties
	 */
	public static Properties convertDataSourceToProperties(DataSource ds)
	{
		Properties prop = new Properties();

		// 自定义属性
		prop.putAll(ds.getCustomize());

		// 以下属性不能为空
		if (StringUtils.isEmptyString(ds.getName()))
		{
			throw new DataSourceException("DataSource attribute [name] can't be null!");
		}
		if (StringUtils.isEmptyString(ds.getDriverClass()))
		{
			throw new DataSourceException("DataSource attribute [driverClass] can't be null!");
		}
		if (StringUtils.isEmptyString(ds.getDriverUrl()))
		{
			throw new DataSourceException("DataSource attribute [driverUrl] can't be null!");
		}

		// 以下属性不能为null，但允许为空
		if (ds.getUser() == null)
		{
			throw new DataSourceException("DataSource attribute [user] can't be null!");
		}
		if (ds.getPassword() == null)
		{
			throw new DataSourceException("DataSource attribute [password] can't be null!");
		}

		// 必须属性设置
		prop.setProperty("name", ds.getName());
		prop.setProperty("user", ds.getUser());
		prop.setProperty("password", ds.getPassword());
		prop.setProperty("driver-url", ds.getDriverUrl());
		prop.setProperty("driver-class", ds.getDriverClass());

		// 可选属性设置
		int max_conn_size = ds.getMaxConnectionSize();
		prop.setProperty("max-connection-size", max_conn_size <= 0 ? Config.DEFAULT_PROP.getProperty("max-connection-size") : String
				.valueOf(max_conn_size));

		int min_conn_size = ds.getMinConnectionSize();
		prop.setProperty("min-connection-size", min_conn_size <= 0 ? Config.DEFAULT_PROP.getProperty("min-connection-size") : String
				.valueOf(min_conn_size));

		int init_conn_size = ds.getInitConnectionSize();
		prop.setProperty("init-connection-size", init_conn_size <= 0 ? Config.DEFAULT_PROP.getProperty("init-connection-size") : String
				.valueOf(init_conn_size));

		int acq_inc_size = ds.getAcquireIncrementSize();
		prop.setProperty("acquire-increment-size", acq_inc_size <= 0 ? Config.DEFAULT_PROP.getProperty("acquire-increment-size") : String
				.valueOf(acq_inc_size));

		int avi_conn_size = ds.getAvailableConnectionSize();
		prop.setProperty("available-connection-size", avi_conn_size <= 0 ? Config.DEFAULT_PROP.getProperty("available-connection-size") : String
				.valueOf(avi_conn_size));

		int max_conn_idle = ds.getMaxConnectionIdletime();
		prop.setProperty("max-connection-idletime", max_conn_idle <= 0 ? Config.DEFAULT_PROP.getProperty("max-connection-idletime") : String
				.valueOf(max_conn_idle));

		int max_conn_life = ds.getMaxConnectionLifetime();
		prop.setProperty("max-connection-lifetime", max_conn_life <= 0 ? Config.DEFAULT_PROP.getProperty("max-connection-lifetime") : String
				.valueOf(max_conn_life));

		int conn_tout = ds.getConnectionTimeout();
		prop.setProperty("connection-timeout", conn_tout <= 0 ? Config.DEFAULT_PROP.getProperty("connection-timeout") : String.valueOf(conn_tout));

		prop.setProperty("show-sql", String.valueOf(ds.getShowSQL()));
		prop.setProperty("load-on-startup", String.valueOf(ds.getLoadOnStartup()));
		prop.setProperty("connection-provider", ds.getConnectionProvider());
		prop.setProperty("db-dialect", ds.getDialect());

		// TODO 此处为兼容权限系统1版，权限系统升级后可移除
		prop.setProperty("dbdialect", ds.getDialect());

		int acq_ret_att = ds.getAcquireRetryAttempts();
		prop.setProperty("acquire-retry-attempts", acq_ret_att <= 0 ? Config.DEFAULT_PROP.getProperty("acquire-retry-attempts") : String
				.valueOf(acq_ret_att));

		int acq_ret_del = ds.getAcquireRetryDelay();
		prop.setProperty("acquire-retry-delay", acq_ret_del <= 0 ? Config.DEFAULT_PROP.getProperty("acquire-retry-delay") : String
				.valueOf(acq_ret_del));

		String test_sql = ds.getTestSql();
		prop.setProperty("test-sql", test_sql);

		boolean test_conn_cho = ds.isTestConnectionCheckout();
		prop.setProperty("test-connection-checkout", String.valueOf(test_conn_cho));

		boolean test_conn_chi = ds.isTestConnectionCheckin();
		prop.setProperty("test-connection-checkin", String.valueOf(test_conn_chi));

		int idl_conn_tesp = ds.getIdleConnectionTestPeriod();
		prop.setProperty("idle-connection-test-period", idl_conn_tesp <= 0 ? Config.DEFAULT_PROP.getProperty("idle-connection-test-period") : String
				.valueOf(idl_conn_tesp));

		return prop;
	}

	/**
	 * 将普通数据源对象转换为只读的数据源对象
	 * 
	 * @param ds 数据源对象
	 * @return 只读数据源对象
	 */
	public static ReadableDataSource convertDataSourceToReadable(DataSource ds)
	{
		return new ReadableDataSource(ds);
	}

	/**
	 * 只读DataSource对象
	 * 
	 * @author SHEN.GANG
	 */
	private static class ReadableDataSource extends DataSource
	{
		private static final long	serialVersionUID	= 1L;

		public ReadableDataSource(DataSource ds)
		{
			super(ds.getName(), ds.getDriverClass(), ds.getDriverUrl(), ds.getUser(), ds.getPassword(), ds.getMaxConnectionSize(), ds
					.getMinConnectionSize(), ds.getInitConnectionSize(), ds.getAcquireIncrementSize(), ds.getAvailableConnectionSize(), ds
					.getMaxConnectionIdletime(), ds.getMaxConnectionLifetime(), ds.getConnectionTimeout(), ds.getShowSQL(), ds
					.getConnectionProvider(), Collections.unmodifiableMap(ds.getCustomize()));
		}

		public void setDriverClass(String driverClass)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setDriverUrl(String driverUrl)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setUser(String user)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setPassword(String password)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setMaxConnectionSize(int maxConnectionSize)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setMinConnectionSize(int minConnectionSize)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setInitConnectionSize(int initConnectionSize)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setConnectionTimeout(int connectionTimeout)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setShowSQL(boolean showSQL)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setConnectionProvider(String connectionProvider)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setAcquireIncrementSize(int acquireIncrementSize)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setAvailableConnectionSize(int availableConnectionSize)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setMaxConnectionIdletime(int maxConnectionIdletime)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setMaxConnectionLifetime(int maxConnectionLifetime)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setIdleConnectionTestPeriod(int idleConnectionTestPeriod)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setTestConnectionCheckin(boolean testConnectionCheckin)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setTestConnectionCheckout(boolean testConnectionCheckout)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setTestSql(String testSql)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setAcquireRetryDelay(int acquireRetryDelay)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void setAcquireRetryAttempts(int acquireRetryAttempts)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}

		public void set(String key, String value)
		{
			throw new UnsupportedOperationException("Can't change any value!");
		}
	}
}
