package com.lizard.fastdb.datasource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.lizard.fastdb.connection.ConnectionProvider;
import com.lizard.fastdb.connection.ConnectionProviderFactory;
import com.lizard.fastdb.util.StringUtils;

/**
 * DB缓存池管理，主要包括数据源缓存，软连接缓存，以及ConnectionProvider缓存
 * 
 * @author SHEN.GANG
 */
public class DataSourceCache
{
	/**
	 * 数据源对象池<br>
	 * key：数据源对象名(name)<br>
	 * value：数据源对象
	 */
	public static ConcurrentMap<String, Properties>			DATASOURCE_POOL			= new ConcurrentHashMap<String, Properties>();

	/**
	 * 软连接池<br>
	 * key：虚拟数据源<br>
	 * value：真实数据源
	 */
	public static ConcurrentMap<String, String>				LINKMAPPING_POOL		= new ConcurrentHashMap<String, String>();

	/**
	 * 连接策略对象池<br>
	 * key：策略对象所属的数据源名称<br>
	 * value：策略对象
	 */
	public static ConcurrentMap<String, ConnectionProvider>	CONNECTIONPROVIDER_POOL	= new ConcurrentHashMap<String, ConnectionProvider>();

	/**
	 * 将数据源存入缓存
	 * 
	 * @param ds 数据源配置对象
	 */
	public static void putDataSource(Properties ds)
	{
		DATASOURCE_POOL.putIfAbsent(ds.getProperty("name").toLowerCase(), ds);
	}

	/**
	 * 新增软连接
	 * 
	 * @param virtual 虚拟数据源名称
	 * @param real 真实数据源名称
	 */
	public static void putLinkmapping(String virtual, String real)
	{
		LINKMAPPING_POOL.put(virtual.toLowerCase(), real.toLowerCase());
	}

	/**
	 * 保存ConnectionProvider
	 * 
	 * @param name ConnectionProvider对象所属的数据源名称
	 * @param cp ConnectionProvider对象
	 */
	private static void putConnectionProvider(String name, ConnectionProvider cp)
	{
		CONNECTIONPROVIDER_POOL.putIfAbsent(name.toLowerCase(), cp);
	}

	/**
	 * 移除缓存中指定名称的数据源
	 * 
	 * @param name 数据源名称
	 */
	protected static void evictDataSource(String name)
	{
		DATASOURCE_POOL.remove(name.toLowerCase());
	}

	/**
	 * 移除软连接
	 * 
	 * @param virtual 虚拟数据源名称
	 */
	protected static void evictLinkmapping(String virtual)
	{
		virtual = virtual.toLowerCase();
		// 要移除的key列表
		List<String> keys = new ArrayList<String>();
		keys.add(virtual);
		for (String key : LINKMAPPING_POOL.keySet())
		{
			if (LINKMAPPING_POOL.get(key).equals(virtual))
			{
				keys.add(key);
			}
		}
		for (int i = 0; i < keys.size(); i++)
		{
			LINKMAPPING_POOL.remove(keys.get(i));
		}
	}

	/**
	 * 从缓存中移除指定数据源的ConnectionProvider对象
	 * 
	 * @param name 数据源名称
	 */
	protected static void evictConnectionProvider(String name)
	{
		CONNECTIONPROVIDER_POOL.remove(name.toLowerCase());
	}

	/**
	 * 从缓存中获得指定名称的数据源对象
	 * 
	 * @param name 数据源名称或软连接名称
	 * @return 数据源对象
	 */
	public static Properties getDataSource(String name)
	{
		String fina_link_to = getFinalLinkTo(name);
		if (!StringUtils.isEmptyString(fina_link_to))
		{
			return DATASOURCE_POOL.get(fina_link_to);
		}
		else
		{
			return null;
		}
	}

	/**
	 * 获得软连接的指向的真实数据源
	 * 
	 * @param virtual 虚拟或真实数据源名称
	 * @return 真实数据源名称
	 */
	protected static String getFinalLinkTo(String virtual)
	{
		virtual = virtual.toLowerCase();
		String real = LINKMAPPING_POOL.get(virtual);
		while (real != null && !virtual.equals(real))
		{
			virtual = real;
			real = LINKMAPPING_POOL.get(virtual);
		}
		return real;
	}

	/**
	 * 从缓存中获得指定的连接提供对象，如果不存在则返回null
	 * 
	 * @param name 对象名称
	 * @return 连接对象
	 */
	public static ConnectionProvider getConnectionProvider(String name)
	{
		String real = getFinalLinkTo(name);
		if (real == null)
		{
			return null;
		}
		return CONNECTIONPROVIDER_POOL.get(real);
	}

	/**
	 * 获得或创建ConnectionProvider对象，如果缓存中不存在，则创建并放入缓存
	 * 
	 * @param ds 数据源配置
	 * @return ConnectionProvider对象
	 */
	protected static ConnectionProvider createConnectionProvider(Properties ds)
	{
		ConnectionProvider connP = ConnectionProviderFactory.createConnectionProvider(ds);
		putConnectionProvider(ds.getProperty("name"), connP);
		return connP;
	}

	/**
	 * 获得数据源对象池
	 * 
	 * @return 数据源对象池
	 */
	protected static Map<String, Properties> getDataSourcePool()
	{
		return DATASOURCE_POOL;
	}

	/**
	 * 获得连接提供对象池
	 * 
	 * @return ConnectionProvider池
	 */
	protected static Map<String, ConnectionProvider> getConnectionProviderPool()
	{
		return CONNECTIONPROVIDER_POOL;
	}

	/**
	 * 判断当前数据源缓存中是否存在指定名称的数据源
	 * 
	 * @param name 数据源名称
	 * @return true -- 存在，false -- 不存在
	 */
	public static boolean containDataSource(String name)
	{
		return DATASOURCE_POOL.containsKey(name.toLowerCase());
	}

	/**
	 * 检查当前数据源配置是否在缓存中以存在，判断方式是检查url、username、password是否完全相等
	 * 
	 * @param prop 数据源配置
	 * @return 存在重复 -- 重复的数据源名称，不存在重复 -- null
	 */
	protected static String isDataSourceRepeated(Properties prop)
	{
		String driver_url = prop.getProperty("driver-url");
		String user = prop.getProperty("user");
		String password = prop.getProperty("password");

		// 去除url后面的参数
		int index = driver_url.lastIndexOf("?");
		if (index != -1)
		{
			driver_url = driver_url.substring(0, index);
		}

		Iterator<String> iter = DATASOURCE_POOL.keySet().iterator();
		while (iter.hasNext())
		{
			Properties ds = DATASOURCE_POOL.get(iter.next());

			String ds_driver_url = ds.getProperty("driver-url");
			String ds_user = ds.getProperty("user");
			String ds_password = ds.getProperty("password");

			index = ds_driver_url.lastIndexOf("?");
			if (index != -1)
			{
				ds_driver_url = ds_driver_url.substring(0, index);
			}

			// 数据源相同的判断条件是：user、password、url 三者完全相同
			if (driver_url.equalsIgnoreCase(ds_driver_url) && user.equalsIgnoreCase(ds_user) && password.equalsIgnoreCase(ds_password))
			{
				return ds.getProperty("name");
			}
		}

		return null;
	}

	/**
	 * 清理缓存
	 */
	protected static void clean()
	{
		DATASOURCE_POOL.clear();
		LINKMAPPING_POOL.clear();
		CONNECTIONPROVIDER_POOL.clear();

		DATASOURCE_POOL = null;
		LINKMAPPING_POOL = null;
		CONNECTIONPROVIDER_POOL = null;
	}
}
