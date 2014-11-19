package com.lizard.fastdb.connection;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lizard.fastdb.datasource.DataSourceException;
import com.lizard.fastdb.util.ReflectUtils;
import com.lizard.fastdb.util.StringUtils;

/**
 * 负责根据具体的ConnectionProvider实现类初始化ConnectionProvider实例
 * 
 * @author SHEN.GANG
 */
public final class ConnectionProviderFactory
{
	private final static Log	logger	= LogFactory.getLog(ConnectionProviderFactory.class);

	private ConnectionProviderFactory()
	{
	}

	/**
	 * 根据数据源配置信息，创建ConnectionProvider实例
	 * 
	 * @param ds 数据源配置
	 * @return ConnectionProvider
	 */
	public static ConnectionProvider createConnectionProvider(Properties ds)
	{
		// Connection provider 提供者
		String provider_class = ds.getProperty("connection-provider");

		// 如果没有发现有效的 ConnectionProvider，则抛出异常
		if (StringUtils.isEmptyString(provider_class))
		{
			logger.error("Can not found valid Connection Provider, it is empty!");
			throw new DataSourceException("Can not found valid Connection Provider, it is empty!");
		}

		ConnectionProvider connP = null;
		try
		{
			// 实例化 ConnectionProvider
			connP = (ConnectionProvider) ReflectUtils.newInstance(provider_class);
			// 执行连接池初始化
			connP.configure(ds);
		}
		catch (Exception e)
		{
			logger.error("Failed to initial the instance of Connection Provider[" + provider_class + "]");
			throw new DataSourceException("Failed to initial the instance of Connection Provider[" + provider_class + "]", e);
		}

		return connP;
	}

}
