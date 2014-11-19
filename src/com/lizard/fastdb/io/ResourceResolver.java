package com.lizard.fastdb.io;

import java.net.URL;
import java.util.Set;

/**
 * 资源解析器
 *
 * @author  SHEN.GANG
 */
public interface ResourceResolver
{
	/**
	 * 根据文件路径获取多个资源
	 * 
	 * @param location
	 * @return
	 */
	public Set<URL> getResources( String location );
}
