package com.lizard.fastdb.datasource;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.lizard.fastdb.config.Config;
import com.lizard.fastdb.dialect.Dialect;
import com.lizard.fastdb.util.StringUtils;

/**
 * 数据源对象类
 * 
 * @author SHEN.GANG
 */
public class DataSource implements Serializable
{
	private static final long	serialVersionUID			= 1L;

	private String				name						= null;																				// 数据源名称
	private String				driverClass					= null;																				// 数据库驱动类
	private String				driverUrl					= null;																				// 数据库连接URL
	private String				user						= null;																				// 数据库连接用户名
	private String				password					= null;																				// 数据库连接密码
	private String				dialect						= null;																				// 数据库方言
	private int					maxConnectionSize			= Integer.parseInt(Config.DEFAULT_PROP.getProperty("max-connection-size"));			// 数据库连接池中最大的连接数
	private int					minConnectionSize			= Integer.parseInt(Config.DEFAULT_PROP.getProperty("min-connection-size"));			// 数据库连接池中最小的连接数
	private int					initConnectionSize			= Integer.parseInt(Config.DEFAULT_PROP.getProperty("init-connection-size"));			// 数据库连接池中初始化的连接数
	private int					acquireIncrementSize		= Integer.parseInt(Config.DEFAULT_PROP.getProperty("acquire-increment-size"));			// 数据库连接池中当连接耗尽时，一次获得的连接数
	private int					availableConnectionSize		= Integer.parseInt(Config.DEFAULT_PROP.getProperty("available-connection-size"));		// 数据库连接池中保持的空闲连接的数量
	private int					maxConnectionIdletime		= Integer.parseInt(Config.DEFAULT_PROP.getProperty("max-connection-idletime"));		// 每个连接的最大空闲时间
	private int					maxConnectionLifetime		= Integer.parseInt(Config.DEFAULT_PROP.getProperty("max-connection-lifetime"));		// 每个连接的最长生命周期
	private int					connectionTimeout			= Integer.parseInt(Config.DEFAULT_PROP.getProperty("connection-timeout"));				// 创建数据库连接超时时间
	private boolean				showSQL						= Boolean.valueOf(Config.DEFAULT_PROP.getProperty("show-sql"));						// 是否打印执行的SQL语句（通常用于调试使用）
	private String				connectionProvider			= Config.DEFAULT_PROP.getProperty("connection-provider");								// 数据连接策略提供者，默认由Proxool提供

	private int					acquireRetryAttempts		= Integer.parseInt(Config.DEFAULT_PROP.getProperty("acquire-retry-attempts"));			// 获取连接失败后的重试次数
	private int					acquireRetryDelay			= Integer.parseInt(Config.DEFAULT_PROP.getProperty("acquire-retry-delay"));			// 获取连接失败后到下次获取连接的重试间隔时间
	private String				testSql						= Config.DEFAULT_PROP.getProperty("test-sql");											// 测试连接有效性的sql
	private boolean				testConnectionCheckout		= Boolean.parseBoolean(Config.DEFAULT_PROP.getProperty("test-connection-checkout"));	// 是否在将连接从连接池取出时检查连接有效性
	private boolean				testConnectionCheckin		= Boolean.parseBoolean(Config.DEFAULT_PROP.getProperty("test-connection-checkin"));	// 是否在连接放入连接池之前检查其有效性
	private int					idleConnectionTestPeriod	= Integer.parseInt(Config.DEFAULT_PROP.getProperty("idle-connection-test-period"));	// 测试空闲连接有效性的间隔时间

	private Map<String, String>	customize					= new HashMap<String, String>();														// 自定义属性

	public DataSource(String name)
	{
		if (name == null || "".equals(name))
		{
			throw new IllegalArgumentException("DataSource name can't be null!");
		}
		this.name = name.toLowerCase();
	}

	public DataSource(String name, String driverClass, String driverUrl, String user, String password)
	{
		if (name == null || "".equals(name))
		{
			throw new IllegalArgumentException("DataSource name can't be null!");
		}
		this.name = name.toLowerCase();
		this.driverClass = driverClass;
		this.driverUrl = driverUrl;
		this.user = user;
		this.password = password;
	}

	public DataSource(String name, String driverClass, String driverUrl, String user, String password, int maxConnectionSize, int minConnectionSize,
			int initConnectionSize, int acquireIncrementSize, int availableConnectionSize, int maxConnectionIdletime, int maxConnectionLifetime,
			int connectionTimeout, boolean showSQL, String connectionProvider)
	{
		super();
		if (name == null || "".equals(name))
		{
			throw new IllegalArgumentException("DataSource name can't be null!");
		}
		this.name = name.toLowerCase();
		this.driverClass = driverClass;
		this.driverUrl = driverUrl;
		this.user = user;
		this.password = password;
		this.maxConnectionSize = maxConnectionSize;
		this.minConnectionSize = minConnectionSize;
		this.initConnectionSize = initConnectionSize;
		this.acquireIncrementSize = acquireIncrementSize;
		this.availableConnectionSize = availableConnectionSize;
		this.maxConnectionIdletime = maxConnectionIdletime;
		this.maxConnectionLifetime = maxConnectionLifetime;
		this.connectionTimeout = connectionTimeout;
		this.showSQL = showSQL;
		this.connectionProvider = connectionProvider;
	}
	
	public DataSource(String name, String driverClass, String driverUrl, String user, String password, int maxConnectionSize, int minConnectionSize,
			int initConnectionSize, int acquireIncrementSize, int availableConnectionSize, int maxConnectionIdletime, int maxConnectionLifetime,
			int connectionTimeout, boolean showSQL, String connectionProvider, Map<String, String> customize)
	{
		this(name, driverClass, driverUrl, user, password, maxConnectionSize, minConnectionSize,
				initConnectionSize, acquireIncrementSize, availableConnectionSize, maxConnectionIdletime, maxConnectionLifetime,
				connectionTimeout, showSQL, connectionProvider);
		
		this.customize = customize;
	}

	public DataSource(Properties prop)
	{
		Properties p = new Properties();

		// 改动：将下面所有的 (String)value 强制类型转换，修改为 String.valueOf()

		Set<Map.Entry<Object, Object>> entrySet = prop.entrySet();
		for (Map.Entry<Object, Object> entry : entrySet)
		{
			p.setProperty(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
		}

		this.name = String.valueOf(p.remove("name"));
		this.driverClass = String.valueOf(p.remove("driver-class"));
		this.driverUrl = String.valueOf(p.remove("driver-url"));
		this.user = String.valueOf(p.remove("user"));
		this.password = String.valueOf(p.remove("password"));

		Object value = p.remove("max-connection-size");
		if (value != null)
		{
			int valueI = Integer.valueOf(String.valueOf(value));
			this.maxConnectionSize = (valueI == 0) ? this.maxConnectionSize : valueI;
		}

		value = p.remove("min-connection-size");
		if (value != null)
		{
			int valueI = Integer.valueOf(String.valueOf(value));
			this.minConnectionSize = (valueI == 0) ? this.minConnectionSize : valueI;
		}

		value = p.remove("init-connection-size");
		if (value != null)
		{
			int valueI = Integer.valueOf(String.valueOf(value));
			this.initConnectionSize = (valueI == 0) ? this.initConnectionSize : valueI;
		}

		value = p.remove("acquire-increment-size");
		if (value != null)
		{
			int valueI = Integer.valueOf(String.valueOf(value));
			this.acquireIncrementSize = (valueI == 0) ? this.acquireIncrementSize : valueI;
		}

		value = p.remove("available-connection-size");
		if (value != null)
		{
			int valueI = Integer.valueOf(String.valueOf(value));
			this.availableConnectionSize = (valueI == 0) ? this.availableConnectionSize : valueI;
		}

		value = p.remove("max-connection-idletime");
		if (value != null)
		{
			int valueI = Integer.valueOf(String.valueOf(value));
			this.maxConnectionIdletime = (valueI == 0) ? this.maxConnectionIdletime : valueI;
		}

		value = p.remove("max-connection-lifetime");
		if (value != null)
		{
			int valueI = Integer.valueOf(String.valueOf(value));
			this.maxConnectionLifetime = (valueI == 0) ? this.maxConnectionLifetime : valueI;
		}

		value = p.remove("connection-timeout");
		if (value != null)
		{
			int valueI = Integer.valueOf(String.valueOf(value));
			this.connectionTimeout = (valueI == 0) ? this.connectionTimeout : valueI;
		}

		value = p.remove("show-sql");
		if (value != null)
		{
			this.showSQL = Boolean.valueOf(String.valueOf(value));
		}

		value = p.remove("connection-provider");
		if (value != null)
		{
			this.connectionProvider = String.valueOf(value);
		}

		value = p.remove("acquire-retry-attempts");
		if (value != null)
		{
			this.acquireRetryAttempts = Integer.parseInt(value.toString());
		}

		value = p.remove("acquire-retry-delay");
		if (value != null)
		{
			this.acquireRetryDelay = Integer.parseInt(value.toString());
		}

		value = p.remove("test-sql");
		if (value != null)
		{
			this.testSql = String.valueOf(value);
		}

		value = p.remove("test-connection-checkout");
		if (value != null)
		{
			this.testConnectionCheckout = Boolean.parseBoolean(value.toString());
		}

		value = p.remove("test-connection-checkin");
		if (value != null)
		{
			this.testConnectionCheckin = Boolean.parseBoolean(value.toString());
		}

		value = p.remove("idle-connection-test-period");
		if (value != null)
		{
			this.idleConnectionTestPeriod = Integer.parseInt(value.toString());
		}

		entrySet = p.entrySet();
		for (Map.Entry<Object, Object> entry : entrySet)
		{
			this.customize.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
		}

		p = null;
		value = null;
	}

	/**
	 * 获得数据源名称
	 * 
	 * @return 数据源名称
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * 获得当前数据源驱动类
	 * 
	 * @return 数据源驱动类名
	 */
	public String getDriverClass()
	{
		return driverClass;
	}

	/**
	 * 设置当前数据源驱动类
	 * 
	 * @param driverClass 数据源驱动类名
	 */
	public void setDriverClass(String driverClass)
	{
		this.driverClass = driverClass;
	}

	/**
	 * 获得当前数据源URL
	 * 
	 * @return 数据源URL
	 */
	public String getDriverUrl()
	{
		return driverUrl;
	}

	/**
	 * 设置当前数据源URL
	 * 
	 * @param driverUrl 数据源URL
	 */
	public void setDriverUrl(String driverUrl)
	{
		this.driverUrl = driverUrl;
	}

	/**
	 * 获得当前数据源用户
	 * 
	 * @return 数据库用户
	 */
	public String getUser()
	{
		return user;
	}

	/**
	 * 设置当前数据源用户
	 * 
	 * @param user 数据库用户
	 */
	public void setUser(String user)
	{
		this.user = user;
	}

	/**
	 * 获得当前数据源密码
	 * 
	 * @return 数据库用户密码
	 */
	public String getPassword()
	{
		return password;
	}

	/**
	 * 设置当前数据源密码
	 * 
	 * @param password 数据库用户密码
	 */
	public void setPassword(String password)
	{
		this.password = password;
	}

	/**
	 * 获得当前数据源方言
	 * 
	 * @return 数据源方言类名
	 */
	public String getDialect()
	{
		if (StringUtils.isEmptyString(dialect))
		{
			if (getDriverClass().indexOf("mysql") != -1)
			{
				dialect = Dialect.MYSQL;
			}
			else if (getDriverClass().indexOf("oracle") != -1)
			{
				dialect = Dialect.ORACLE;
			}
		}

		return dialect;
	}

	/**
	 * 获得当前数据源最大连接数
	 * 
	 * @return 最大连接数
	 */
	public int getMaxConnectionSize()
	{
		return maxConnectionSize;
	}

	/**
	 * 设置当前数据源最大连接数
	 * 
	 * @param maxConnectionSize 最大连接数
	 */
	public void setMaxConnectionSize(int maxConnectionSize)
	{
		this.maxConnectionSize = maxConnectionSize;
	}

	/**
	 * 获得当前数据源最小连接数
	 * 
	 * @return 最小连接数
	 */
	public int getMinConnectionSize()
	{
		return minConnectionSize;
	}

	/**
	 * 设置当前数据源最小连接数
	 * 
	 * @param minConnectionSize 最小连接数
	 */
	public void setMinConnectionSize(int minConnectionSize)
	{
		this.minConnectionSize = minConnectionSize;
	}

	/**
	 * 获得当前数据源初始化连接数
	 * 
	 * @return 初始化连接数
	 */
	public int getInitConnectionSize()
	{
		return initConnectionSize;
	}

	/**
	 * 设置当前数据源初始化连接数
	 * 
	 * @param initConnectionSize 初始化连接数
	 */
	public void setInitConnectionSize(int initConnectionSize)
	{
		this.initConnectionSize = initConnectionSize;
	}

	/**
	 * 获得当前数据源连接超时时间
	 * 
	 * @return 连接超时时间
	 */
	public int getConnectionTimeout()
	{
		return connectionTimeout;
	}

	/**
	 * 设置当前数据源连接超时时间
	 * 
	 * @param connectionTimeout 连接超时时间
	 */
	public void setConnectionTimeout(int connectionTimeout)
	{
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * 查看是否在服务器启动时加载，默认不加载（false）<br>
	 * 注意：该属性对于动态数据源无效
	 * 
	 * @return true -- 服务器启动时加载数据源，false -- 服务器启动时不加载数据源
	 */
	public boolean getLoadOnStartup()
	{
		return true;
	}

	/**
	 * 查看当前是否打印SQL
	 * 
	 * @return true -- 打印SQL，false -- 不打印SQL
	 */
	public boolean getShowSQL()
	{
		return showSQL;
	}

	/**
	 * 设置当前是否打印SQL
	 * 
	 * @param showSQL true -- 打印SQL，false -- 不打印SQL
	 */
	public void setShowSQL(boolean showSQL)
	{
		this.showSQL = showSQL;
	}

	/**
	 * 获得当前数据源的ConnectionProvider
	 * 
	 * @return ConnectionProvider类名
	 */
	public String getConnectionProvider()
	{
		return connectionProvider;
	}

	/**
	 * 设置当前数据源的ConnectionProvider
	 * 
	 * @param connectionProvider ConnectionProvider类名
	 */
	public void setConnectionProvider(String connectionProvider)
	{
		this.connectionProvider = connectionProvider;
	}

	/**
	 * 获得当前数据源连接耗尽时，一次创建的连接数
	 * 
	 * @return 数据源连接耗尽时，一次创建的连接数
	 */
	public int getAcquireIncrementSize()
	{
		return acquireIncrementSize;
	}

	/**
	 * 设置当前数据源连接耗尽时，一次创建的连接数
	 * 
	 * @param acquireIncrementSize 数据源连接耗尽时，一次创建的连接数
	 */
	public void setAcquireIncrementSize(int acquireIncrementSize)
	{
		this.acquireIncrementSize = acquireIncrementSize;
	}

	/**
	 * 获得当前数据源的可用连接数
	 * 
	 * @return 可用连接数
	 */
	public int getAvailableConnectionSize()
	{
		return availableConnectionSize;
	}

	/**
	 * 设置当前数据源的可用连接数
	 * 
	 * @param aviableConnectionSize 可用连接数
	 */
	public void setAvailableConnectionSize(int availableConnectionSize)
	{
		this.availableConnectionSize = availableConnectionSize;
	}

	/**
	 * 获得当前数据源连接的最大空闲时间
	 * 
	 * @return 连接的最大空闲时间
	 */
	public int getMaxConnectionIdletime()
	{
		return maxConnectionIdletime;
	}

	/**
	 * 设置当前数据源连接的最大空闲时间
	 * 
	 * @param maxConnectionIdletime 连接的最大空闲时间
	 */
	public void setMaxConnectionIdletime(int maxConnectionIdletime)
	{
		this.maxConnectionIdletime = maxConnectionIdletime;
	}

	/**
	 * 获得当前数据源连接的最大生命周期
	 * 
	 * @return 连接的最大生命周期
	 */
	public int getMaxConnectionLifetime()
	{
		return maxConnectionLifetime;
	}

	/**
	 * 设置当前数据源连接的最大生命周期
	 * 
	 * @param maxConnectionLifetime 连接的最大生命周期
	 */
	public void setMaxConnectionLifetime(int maxConnectionLifetime)
	{
		this.maxConnectionLifetime = maxConnectionLifetime;
	}

	/**
	 * 获得当获取连接失败时，尝试重新获取连接的次数
	 * 
	 * @return 重新获得连接的次数
	 */
	public int getAcquireRetryAttempts()
	{
		return acquireRetryAttempts;
	}

	/**
	 * 设置获取连接失败时，尝试重新获取连接的次数
	 * 
	 * @param acquireRetryAttempts 重新获得连接的次数
	 */
	public void setAcquireRetryAttempts(int acquireRetryAttempts)
	{
		this.acquireRetryAttempts = acquireRetryAttempts;
	}

	/**
	 * 获得当获取连接失败时，每次重试之间的间隔时间
	 * 
	 * @return 重试获得连接之间的间隔时间
	 */
	public int getAcquireRetryDelay()
	{
		return acquireRetryDelay;
	}

	/**
	 * 设置获得连接失败时，每次重试之间的间隔时间
	 * 
	 * @param acquireRetryDelay 重试获得连接之间的间隔时间
	 */
	public void setAcquireRetryDelay(int acquireRetryDelay)
	{
		this.acquireRetryDelay = acquireRetryDelay;
	}

	/**
	 * 获得测试连接有效性的sql
	 * 
	 * @return 测试连接有效性的sql
	 */
	public String getTestSql()
	{
		return testSql;
	}

	/**
	 * 设置测试连接有效性的sql
	 * 
	 * @param testSql 测试连接有效性的sql
	 */
	public void setTestSql(String testSql)
	{
		this.testSql = testSql;
	}

	/**
	 * 获得是否在从连接池取出连接时测试连接的有效性
	 * 
	 * @return true -- 测试，false -- 不测试（默认）
	 */
	public boolean isTestConnectionCheckout()
	{
		return testConnectionCheckout;
	}

	/**
	 * 设置是否在从连接池取出连接时测试连接的有效性
	 * 
	 * @param testConnectionCheckout true -- 测试，false -- 不测试
	 */
	public void setTestConnectionCheckout(boolean testConnectionCheckout)
	{
		this.testConnectionCheckout = testConnectionCheckout;
	}

	/**
	 * 获得是否在将连接放入连接池之前测试连接的有效性
	 * 
	 * @return true -- 测试，false -- 不测试（默认）
	 */
	public boolean isTestConnectionCheckin()
	{
		return testConnectionCheckin;
	}

	/**
	 * 设置是否在将连接放入连接池之前测试连接的有效性
	 * 
	 * @param testConnectionCheckin true -- 测试，false -- 不测试
	 */
	public void setTestConnectionCheckin(boolean testConnectionCheckin)
	{
		this.testConnectionCheckin = testConnectionCheckin;
	}

	/**
	 * 获得每次测试空闲连接有效性的间隔时间
	 * 
	 * @return 测试空闲连接有效性的间隔时间
	 */
	public int getIdleConnectionTestPeriod()
	{
		return idleConnectionTestPeriod;
	}

	/**
	 * 设置测试空闲连接有效性的间隔时间
	 * 
	 * @param idleConnectionTestPeriod 测试空闲连接有效性的间隔时间
	 */
	public void setIdleConnectionTestPeriod(int idleConnectionTestPeriod)
	{
		this.idleConnectionTestPeriod = idleConnectionTestPeriod;
	}

	/**
	 * 设置自定义属性，该方法用于设置除当前DataSource已有属性外的特殊属性（无相应的get和set方法）<br>
	 * 注意：key在内部将被强制转换为小写格式
	 * 
	 * @param key 自定义属性名
	 * @param value 自定义属性值
	 */
	public void set(String key, String value)
	{
		if (StringUtils.isEmptyString(key))
		{
			throw new DataSourceException("Key can't be null!");
		}
		if (StringUtils.isEmptyString(value))
		{
			throw new DataSourceException("Value can't be null!");
		}

		this.customize.put(key, value);
	}

	/**
	 * 获得指定的自定义属性的值
	 * 
	 * @param key 自定义属性名
	 * @return 自定义属性的值
	 */
	public String get(String key)
	{
		return this.customize.get(key);
	}

	/**
	 * 获得自定义属性集
	 * 
	 * @return 自定义属性集
	 */
	public Map<String, String> getCustomize()
	{
		return customize;
	}
}
