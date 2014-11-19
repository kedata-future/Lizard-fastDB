package com.lizard.fastdb.datasource;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 数据源连接池加载销毁监听器
 * 
 * @author SHEN.GANG
 */
public class DataSourceListener implements ServletContextListener
{
	public void contextInitialized(ServletContextEvent arg0)
	{
		// 服务器启动时加载注册所有数据源
		DataSourceManager.startup();
	}

	public void contextDestroyed(ServletContextEvent arg0)
	{
		// 服务器停止时，销毁所有连接池
		DataSourceManager.shutdown();
	}
}
