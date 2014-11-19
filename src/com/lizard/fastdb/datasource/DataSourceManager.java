package com.lizard.fastdb.datasource;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lizard.fastdb.config.Config;
import com.lizard.fastdb.connection.ConnectionProvider;

/**
 * 数据源管理类 负责加载数据源，获得数据源，更新数据源，销毁数据源，销毁连接池等
 * 
 * @author SHEN.GANG
 */
public class DataSourceManager
{
	private static final Log	logger			= LogFactory.getLog(DataSourceManager.class);

	private static final String	DS_CONFIG_XML	= "datasource.xml";	// 数据源XML配置文件名称

	private volatile static boolean	HAS_STARTUP	= false;	// 用于控制startup方法的调用，该方法仅允许被调用一次
	
	/**
	 * 用于静态数据源获得ConnectionProvider
	 * 
	 * @param ds_name 数据源名称
	 * @return 数据源对应的ConnectionProvider
	 */
	public synchronized static void initConnectionProvider(String ds_name)
	{
		ConnectionProvider connP = DataSourceCache.getConnectionProvider(ds_name);

		// 如果缓存中没有当前数据源的ConnectionProvider对象，说明该数据源没有被加载
		if (connP == null)
		{
			// 查看当前数据源是否在缓存中
			Properties ds = DataSourceCache.getDataSource(ds_name);

			// 缓存中没有该数据源可能是没有加载导致的
			if (ds == null)
			{
				loadDataSourceConfig();
				ds = DataSourceCache.getDataSource(ds_name);
			}

			// 如果还是为null表示数据源不存在
			if (ds == null)
			{
				throw new DataSourceException("DataSource named [" + ds_name + "] is not exist!");
			}

			connP = DataSourceCache.createConnectionProvider(ds);

			loadOnStartup();
		}

		connP = null;
	}

	/**
	 * 用于动态数据源获得ConnnectionProvider
	 * 
	 * @param ds 数据源配置
	 * @return 数据源对应的ConnectionProvider
	 */
	public synchronized static void initConnectionProvider(Properties ds)
	{
		String ds_name = ds.getProperty("name");

		ConnectionProvider connP = DataSourceCache.getConnectionProvider(ds_name);

		// 为null表示数据源不存在或未加载
		if (connP == null)
		{
			// 查看是否存在配置重复的数据源
			String repeat = DataSourceCache.isDataSourceRepeated(ds);
			// 没有重复配置的数据源
			if (repeat == null)
			{
				DataSourceCache.putDataSource(ds);
				DataSourceCache.putLinkmapping(ds_name, ds_name);
				DataSourceCache.createConnectionProvider(ds);
			}
			// 存在重复配置的数据源
			else
			{
				logger.info("DataSource [" + repeat + "] is duplicate to DataSource [" + ds_name + "]！We make the softlink [" + ds_name + " --> "
						+ repeat + "] to alternative it！");

				DataSourceCache.putLinkmapping(ds_name, repeat);

				// 判断数据源是否被加载
				connP = DataSourceCache.getConnectionProvider(repeat);
				if (connP == null)
				{
					DataSourceCache.createConnectionProvider(DataSourceCache.getDataSource(repeat));
				}
			}

			connP = null;
		}
		// 不为null表示当前数据源名称对应的数据源已加载
		else
		{
			Properties ds_cache = DataSourceCache.getDataSource(ds_name);

			// 如果缓存中的数据源配置与当前的数据源配置不同，表示数据源名称重复
			if (!DataSourceUtil.isSameDataSource(ds, ds_cache))
			{
				throw new DataSourceException("DataSource named [" + ds_name + "] is exist!Exist datasource url["
						+ ds_cache.getProperty("driver-url") + "].");
			}
		}
	}

	/**
	 * 加载所有数据源，仅用于监听器调用
	 */
	public synchronized static void startup()
	{
		if (HAS_STARTUP)
		{
			logger.info("DataSource config has been loaded already!");
			return;
		}
		
		HAS_STARTUP = true;
		logger.info("Begin Loading All DataSource");

		loadDataSourceConfig();
		loadOnStartup();
	}

	/**
	 * 加载数据源配置到缓存
	 */
	private static void loadDataSourceConfig()
	{
		// 加载所有配置的数据源到缓存
		Config config = new Config(DS_CONFIG_XML);

		for (Properties ds : config.getDatasources())
		{
			DataSourceCache.putDataSource(ds);
		}

		for (Map.Entry<String, String> entry : config.getLinkmapping().entrySet())
		{
			DataSourceCache.putLinkmapping(entry.getKey(), entry.getValue());
		}

		config = null;
	}

	/**
	 * 加载load-on-startup为true的数据源
	 */
	private static void loadOnStartup()
	{
		Set<Map.Entry<String, Properties>> entrySet = DataSourceCache.getDataSourcePool().entrySet();
		for (Map.Entry<String, Properties> entry : entrySet)
		{
			String name = entry.getKey();
			Properties ds = entry.getValue();

			if (Boolean.valueOf(ds.getProperty("load-on-startup")) && DataSourceCache.getConnectionProvider(name) == null)
			{
				logger.info("Register [" + name + "] DataSource ...");
				// 创建ConnectionProvider配置数据源，并将ConnectionProvider放入缓存
				DataSourceCache.createConnectionProvider(ds);
			}
		}
	}

	/**
	 * 彻底销毁指定的数据源，该数据源以后不能再被使用。
	 * 
	 * @param ds_name 待销毁的数据源名称
	 */
	public static void destroy(String ds_name)
	{
		destroy(ds_name, true);
	}
	
	/**
	 * 关闭一个数据源连接池，释放里面创建的所有物理连接
	 * 
	 * @param ds_name 待关闭的数据源名称
	 * 
	 * @author SHEN.GANG
	 */
	public static void shutdown(String ds_name)
	{
		destroy(ds_name, false);
	}

	/**
	 * 销毁全部连接池，清理数据源、软连接和ConnectionProvider缓存
	 */
	public synchronized static void shutdown()
	{
		logger.info("--- Shutdown Connection Pool ---");

		// 获取系统中注册的所有连接池对象
		Map<String, ConnectionProvider> pool = DataSourceCache.getConnectionProviderPool();
		if (pool == null || pool.isEmpty())
		{
			return;
		}

		Set<Map.Entry<String, ConnectionProvider>> entrySet = pool.entrySet();

		// 每种ConnectionProvider的shutdown方法只被调用一次
		// Set<String> hasShutdown = new HashSet<String>();

		ConnectionProvider connP = null;
		for (Map.Entry<String, ConnectionProvider> entry : entrySet)
		{
			connP = entry.getValue();

			// 下面的方式存在潜在的bug：下面设定了每个ConnectionProvider类名称只能调用一次:
			// 对于Proxool有一个 ProxoolFade.shutdown() 关闭所有的不同的数据源连接池，
			// 而对于 C3P0 和 BoneCP 来说，则没有这样的方法，它们每次只能关闭当前一个对应的数据源连接池，
			// 如果采用下面的限定，则会造成多个同类型数据源(一种连接池提供商下的多个数据源)连接池对象只有一个被销毁，进而造成其它连接池对象无法被销毁，而泄露物理连接。
			// 所以，必须对所有的数据源对象逐个关闭。
			// String connP_class = connP.getClass().getName();
			// if (hasShutdown.contains(connP_class))
			// {
			// continue;
			// }
			// hasShutdown.add(connP_class);

			if (null != connP)
			{
				// 分别调用ConnectionProvider的shutdown方法关闭当前这个连接池
				connP.shutdown();
				connP = null;
			}
		}

		// 清理缓存
		DataSourceCache.clean();
		// hasShutdown.clear();
		// hasShutdown = null;
	}
	
	/**
	 * 销毁指定的数据源<br>
	 * 1、如果传递的是真实数据源名称，将销毁对应的真实数据源；<br>
	 * 2、如果传递的是虚拟数据源名称，将销毁软连接，软连接指向的真实数据源不会被销毁；
	 * 
	 * @param ds_name 待销毁的数据源名称
	 * @param isCompleted  true -- 表示彻底销毁这个数据源，以后不能再被使用；
	 * 				 false -- 表示只释放数据源连接池中创建的所有物理连接，以后可以被再次创建使用。
	 */
	private synchronized static void destroy(String ds_name, boolean isCompleted)
	{
		if(isCompleted) {
			logger.info("Destory [" + ds_name + "] DataSource ...");
		} else {
			logger.info("Shutdown [" + ds_name + "] DataSource ...");
		}

		String real = DataSourceCache.getFinalLinkTo(ds_name);
		
		// 销毁的是真实数据源
		if (real != null && real.equals(ds_name))
		{
			ConnectionProvider connP = DataSourceCache.getConnectionProvider(real);
			
			if( null == connP )
			{
				if(isCompleted) {
					logger.warn("Warning: You attempt to destroy a datasource[" + ds_name + "] that does not exist, maybe it has been destroyed.");
				} else {
					logger.warn("Warning: You attempt to shutdown a datasource[" + ds_name + "] that does not exist, maybe it has been shut down.");
				}
				
				return;
			}
			
			// ConnectionProvider.destory 方法废弃，由 shutdown()替代
			// connP.destory();
			connP.shutdown();
			
			// 只有当为 true -- 彻底销毁时，才清除对应的缓存数据
			if(isCompleted) {
				DataSourceCache.evictDataSource(real);
			}
			
			DataSourceCache.evictConnectionProvider(real);
		}
		// 销毁的是软连接
		else
		{
			// 只有当为 true -- 彻底销毁时，才销毁软连
			if(isCompleted) {
				DataSourceCache.evictLinkmapping(ds_name);
			}
		}
	}
	
}
