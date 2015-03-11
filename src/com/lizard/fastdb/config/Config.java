package com.lizard.fastdb.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;

import com.lizard.fastdb.datasource.DataSourceException;
import com.lizard.fastdb.dialect.Dialect;
import com.lizard.fastdb.io.ClasspathResourceResolver;
import com.lizard.fastdb.io.ResourceResolver;
import com.lizard.fastdb.util.ClassLoaderUtils;
import com.lizard.fastdb.util.ObjectExtend;
import com.lizard.fastdb.util.XMLUtils;

/**
 * 数据源配置解析类，用于解析给定的数据源配置文件(V1版为datasource.xml)
 * 
 * @author SHEN.GANG
 */
public class Config
{
	private static final Log			logger					= LogFactory.getLog(Config.class);

	// 数据源配置文件格式
	private static final String			DATASOURCE_SCHEMA		= "META-INF/fastdb-1.0.xsd";
	// 默认数据源属性配置
	private static final String			DATASOURCE_DEFAULT_PROP	= "META-INF/datasource-default.properties";

	// 默认数据源配置属性集，在类被载入时集合初始化，可被其他地方调用
	public static final Properties		DEFAULT_PROP			= new Properties();

	// 已加载的配置文件路径集，用于防止配置文件重复加载
	private static final Set<String>	LOADED_CONFIG_PATH		= new LinkedHashSet<String>();

	// 最终的数据源列表
	private List<Properties>			datasourceList			= null;
	// 最终的软连接列表
	private Map<String, String>			linkmapping				= null;
	// 用于获取类路径下的资源
	private ResourceResolver			resolver				= null;

	// 加载数据源默认配置
	static
	{
		loadDefaultConfig();
	}

	/**
	 * 解析指定的数据源配置
	 * 
	 * @param path 数据源配置文件路径
	 */
	public Config(String path)
	{
		datasourceList = new LinkedList<Properties>();
		linkmapping = new HashMap<String, String>();
		resolver = new ClasspathResourceResolver();

		// 解析指定匹配模式的文件
		parsePatternPath(path);

		// 验证软连接合法性
		validateLinkmaping();
	}

	/**
	 * 加载数据源默认配置
	 */
	private static void loadDefaultConfig()
	{
		logger.info("Loaded datasource default configuration from: " + DATASOURCE_DEFAULT_PROP);

		InputStream is = ClassLoaderUtils.getResourceAsStream(DATASOURCE_DEFAULT_PROP, Config.class);
		if (is == null)
		{
			logger.error("Can't get " + DATASOURCE_DEFAULT_PROP + " inputstream.");
			throw new DataSourceException("Can't get " + DATASOURCE_DEFAULT_PROP + " inputstream.");
		}
		else
		{
			try
			{
				DEFAULT_PROP.load(is);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					is.close();
					is = null;
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		if (logger.isDebugEnabled())
		{
			logger.debug("Default datasource configure: [" + DEFAULT_PROP + "]");
		}
	}

	/**
	 * 解析匹配指定模式的文件
	 * 
	 * @param pattern_path 文件名匹配模式
	 */
	private void parsePatternPath(String pattern_path)
	{
		// 从classpath下获得所有匹配文件名模式的文件url
		Set<URL> resources = resolver.getResources(pattern_path);
		if (logger.isDebugEnabled())
		{
			logger.debug("Find resources by [" + pattern_path + "]: " + resources);
		}
		if (resources == null || resources.size() == 0)
		{
			return;
		}

		for (URL resource : resources)
		{
			// 解析数据源配置文件
			parseXml(resource);
		}
	}

	/**
	 * 递归解析数据源xml配置文件
	 * 
	 * @param url 当前xml文件url
	 */
	private void parseXml(URL resource)
	{
		// 1、获取xml
		Document doc = loadXml(resource);
		if (doc == null)
		{
			return;
		}

		Element root = doc.getRootElement();

		// 2、解析constants配置
		Properties cons = parseConstants(root);

		// 3、解析datasource配置
		List<Properties> dsList = parseDataSources(root);

		// 4、合并constant和datasource
		mergeConfigParams(cons, dsList);

		// 5、解析link-mapping
		parseLinkmapping(root);

		// 6、解析include
		parseInclude(root);
	}

	/**
	 * 从指定路径加载xml，并返回Document对象
	 * 
	 * @param url xml文件url
	 * @return Document对象
	 */
	private Document loadXml(URL xmlUrl)
	{
		Document doc = null;
		InputStream is = null;
		try
		{
			logger.info("Loaded datasource configuration from: " + xmlUrl);

			// 检查当前配置文件是否已经被加载过
			if (LOADED_CONFIG_PATH.contains(xmlUrl.getPath()))
			{
				logger.warn("Datasource config file [" + xmlUrl + "] has been loaded.");
				return null;
			}
			LOADED_CONFIG_PATH.add(xmlUrl.getPath());

			is = xmlUrl.openStream();
			doc = XMLUtils.getDocumentFromStream(is);
		}
		catch (IOException e)
		{
			logger.error("Can't load datasource config file from: " + xmlUrl + "!", e);
			e.printStackTrace();
		}
		finally
		{
			if (is != null)
			{
				try
				{
					is.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				is = null;
			}
		}

		// 对数据源配置文件使用datasource schema文件进行合法性验证
		if (doc != null && !XMLValidator.validate(DATASOURCE_SCHEMA, doc.asXML()))
		{
			logger.error("Datasource config file [" + xmlUrl + "] is invalid!");
			throw new DataSourceException("DataSource config file [" + xmlUrl + "] is invalid!");
		}

		return doc;
	}

	/**
	 * 解析常量配置
	 * 
	 * @param root 配置文件根元素
	 */
	@SuppressWarnings("unchecked")
	private Properties parseConstants(Element root)
	{
		// 由于数据源配置文件增加了命名空间等，下面所有的xpath必须进行namespace设置，否则无法获取节点信息
		Element constants = (Element) root.selectSingleNode(XMLUtils.setXPathNamespace(root, "./constants"));// 常量集

		Properties prop = new Properties();

		if (constants == null)
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("Constant configure: []");
			}
			return prop;
		}

		List<Element> constantList = constants.selectNodes(XMLUtils.setXPathNamespace(constants, "./constant"));

		if (constantList == null || constantList.size() == 0)
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("Constant configure: []");
			}
			return prop;
		}

		Attribute nameA = null;
		Attribute valueA = null;
		String name = null;
		String value = null;
		for (Element constant : constantList)
		{
			nameA = constant.attribute("name");
			if (nameA == null)
			{
				throw new DataSourceException("Couldn't find [name] attribute in [constant] element!");
			}

			valueA = constant.attribute("value");
			if (valueA == null)
			{
				throw new DataSourceException("Couldn't find [value] attribute in [constant] element!");
			}

			name = nameA.getStringValue().trim();
			value = valueA.getStringValue().trim();

			prop.setProperty(name.toLowerCase(), value);
		}
		nameA = null;
		valueA = null;
		name = null;
		value = null;

		if (logger.isDebugEnabled())
		{
			logger.debug("Constant configure: [" + prop + "]");
		}

		return prop;
	}

	/**
	 * 加载所有数据源配置
	 * 
	 * @param root 配置文件根元素
	 * @return 数据源properties配置
	 */
	@SuppressWarnings("unchecked")
	private List<Properties> parseDataSources(Element root)
	{
		// 获取所有数据源元素对象
		List<Element> datasources = root.selectNodes(XMLUtils.setXPathNamespace(root, "./datasource"));

		if (datasources == null || datasources.size() == 0)
		{
			return null;
		}

		List<Properties> propList = new ArrayList<Properties>();
		for (Element dsE : datasources)
		{
			propList.add(parseDataSource(dsE));
		}

		return propList;
	}

	/**
	 * 加载指定数据源
	 * 
	 * @param datasource 数据源配置根元素
	 * @return 数据源properties格式配置
	 */
	@SuppressWarnings("unchecked")
	private Properties parseDataSource(Element datasource)
	{
		Properties prop = new Properties();

		List<Element> elements = datasource.selectNodes(XMLUtils.setXPathNamespace(datasource, "./*"));

		if (elements == null || elements.size() == 0)
		{
			throw new DataSourceException("Couldn't find any element in datasource element.");
		}

		String name = null;
		String value = null;
		for (Element ele : elements)
		{
			name = ele.getName();

			// 如果是自定义元素
			if ("customize".equals(name))
			{
				parseCustomize(ele, prop);
				continue;
			}

			value = ele.getStringValue().trim();

			prop.setProperty(name, value);
		}
		name = null;
		value = null;

		if (logger.isDebugEnabled())
		{
			logger.debug("Datasource configure before merge: [" + prop + "]");
		}

		return prop;
	}

	/**
	 * 解析customize元素配置
	 * 
	 * @param customize customize元素
	 * @param prop 当前数据源配置集
	 */
	@SuppressWarnings("unchecked")
	private void parseCustomize(Element customize, Properties prop)
	{
		List<Element> customizeElements = customize.selectNodes("./*");

		if (customizeElements == null || customizeElements.size() == 0)
		{
			return;
		}

		String name = null;
		String value = null;
		for (Element customizeEle : customizeElements)
		{
			name = customizeEle.getName();
			value = customizeEle.getStringValue().trim();

			prop.setProperty(name, value);
		}
		name = null;
		value = null;
	}

	/**
	 * 合并constant和datasource配置
	 * 
	 * @param cons constant配置
	 * @param dsList datasource配置列表
	 */
	private void mergeConfigParams(Properties cons, List<Properties> dsList)
	{
		if (dsList == null || dsList.size() == 0)
		{
			return;
		}

		for (Properties ds : dsList)
		{
			// 数据源重复配置判断
			String exist_dsname = isDataSourceRepeated(ds);
			// 当前数据源名称
			String name = ds.getProperty("name");

			// 不重复
			if (exist_dsname == null)
			{
				// 查看数据源名字是否存在
				if (isDataSourceNameExist(name))
				{
					logger.error("Datasource named [" + name + "] is exist!");
					throw new DataSourceException("DataSource named [" + name + "] is exist!");
				}

				// 加入db-dialect
				initDSDialect(ds);

				ds = ObjectExtend.extend(Properties.class, DEFAULT_PROP, cons, ds);

				if (logger.isDebugEnabled())
				{
					logger.debug("Datasource configure after merge: " + ds);
				}

				datasourceList.add(ds);
				addLinkmapping(name, name);
			}
			else
			{
				logger.info("DataSource [" + exist_dsname + "] is duplicate to DataSource [" + name + "]! We make the softlink [" + name + " --> "
						+ exist_dsname + "] to alternative it!");

				addLinkmapping(name, exist_dsname);
			}
		}
	}

	/**
	 * 判断数据源是否重复<br>
	 * 通过driver-url、user、password判断是否重复
	 * 
	 * @param source 源数据源
	 * @return 重复的数据源名称，没有重复的数据源则返回null
	 */
	private String isDataSourceRepeated(Properties source)
	{
		String driver_url = source.getProperty("driver-url");
		String user = source.getProperty("user");
		String password = source.getProperty("password");

		// 去除url后面的参数
		int index = driver_url.lastIndexOf("?");
		if (index != -1)
		{
			driver_url = driver_url.substring(0, index);
		}

		for (Properties ds : datasourceList)
		{
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
	 * 判断数据源名称是否重复，忽略大小写
	 * 
	 * @param name 数据源名称
	 * @return true -- 重复，false -- 不重复
	 */
	private boolean isDataSourceNameExist(String name)
	{
		for (Properties ds : datasourceList)
		{
			if (ds.getProperty("name").equalsIgnoreCase(name))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * 根据数据源的 driver-class 设置数据源的 db-dialect
	 * 
	 * @param prop 当前数据源
	 */
	private void initDSDialect(Properties prop)
	{
		if (prop.getProperty("driver-class").indexOf("mysql") != -1)
		{
			prop.setProperty("db-dialect", Dialect.MYSQL);
		}
		else if (prop.getProperty("driver-class").indexOf("oracle") != -1)
		{
			prop.setProperty("db-dialect", Dialect.ORACLE);
		}

		// TODO 此处为兼容权限系统1版，权限系统升级后可移除
		prop.setProperty("dbdialect", prop.getProperty("db-dialect"));
	}

	/**
	 * 解析软连接配置
	 * 
	 * @param root 配置文件根元素
	 */
	@SuppressWarnings("unchecked")
	private void parseLinkmapping(Element root)
	{
		List<Element> linkmappings = root.selectNodes(XMLUtils.setXPathNamespace(root, "./link-mapping"));
		if (linkmappings == null || linkmappings.size() == 0)
		{
			return;
		}

		Attribute nameA = null;
		Attribute linktoA = null;
		String name = null;
		String linkto = null;
		for (Element lm : linkmappings)
		{
			nameA = lm.attribute("name");
			if (nameA == null)
			{
				throw new DataSourceException("Couldn't find [name] attribute in [link-mapping] element!");
			}

			linktoA = lm.attribute("linkto");
			if (linktoA == null)
			{
				throw new DataSourceException("Couldn't find [linkto] attribute in [link-mapping] element!");
			}

			name = nameA.getStringValue();
			linkto = linktoA.getStringValue();

			addLinkmapping(name, linkto);
		}
		nameA = null;
		linktoA = null;
		name = null;
		linkto = null;
	}

	/**
	 * 增加软连接
	 * 
	 * @param name 软连接名称
	 * @param linkto 数据源名称
	 */
	private void addLinkmapping(String name, String linkto)
	{
		// 去除首尾空格并转换为小写
		name = (name == null ? null : name.trim().toLowerCase());
		linkto = (linkto == null ? null : linkto.trim().toLowerCase());

		if (name == null || "".equals(name) || linkto == null || "".equals(linkto))
		{
			logger.error("link-mapping name and linkto should not be empty!");
			return;
		}

		// 软连接名称已经存在，则提示替换的警告信息
		if (linkmapping.containsKey(name))
		{
			logger.warn("link-mapping name [" + name + "] is exist, replace linkmapping from [" + name + " ->" + linkmapping.get(name) + "] to ["
					+ name + " -> " + linkto + "].");
		}

		if (logger.isDebugEnabled())
		{
			logger.debug("Add linkmapping configure: [name:" + name + ", linkto:" + linkto + "]");
		}

		linkmapping.put(name, linkto);
	}

	/**
	 * 解析include元素
	 * 
	 * @param root 配置文件根元素
	 */
	@SuppressWarnings("unchecked")
	private void parseInclude(Element root)
	{
		// 获得所有引用数据源配置
		List<Element> includes = root.selectNodes(XMLUtils.setXPathNamespace(root, "./include"));

		if (includes == null || includes.size() == 0)
		{
			return;
		}

		for (Element include : includes)
		{
			Attribute dsA = include.attribute("datasource");
			if (dsA == null)
			{
				throw new DataSourceException("Couldn't find attribute [datasource] in [include] element!");
			}

			String dspath = dsA.getStringValue();

			if (dspath == null || "".equals(dspath.trim()))
			{
				continue;
			}

			// include中的datasource属性支持按逗号分隔多个匹配模式的书写方式
			for (String path : dspath.split(","))
			{
				if (path == null || "".equals(path.trim()))
				{
					continue;
				}

				parsePatternPath(path);
			}
		}
	}

	/**
	 * 验证软连接指向的数据源是否是真实数据源
	 */
	private void validateLinkmaping()
	{
		for (Map.Entry<String, String> entry : linkmapping.entrySet())
		{
			String name = entry.getKey();
			String linkto = entry.getValue();

			if (!name.equals(linkto))
			{
				String final_linkto = getLinkmappingFinalLinkto(name);

				// 如果final_linkto为空，表示没有找到对应的数据源
				if (final_linkto == null)
				{
					logger.error("Can't find named [" + linkto + "] datasource config by link-mapping!");
					throw new DataSourceException("Can't find named [" + linkto + "] datasource config by link-mapping!");
				}

				// 如果不相等表示当前linkto并不是最终指向的数据源
				if (!final_linkto.equals(linkto))
				{
					logger.warn("Replace [" + name + "] linkto from [" + linkto + "] to [" + final_linkto + "].");
					linkto = final_linkto;
					entry.setValue(final_linkto);
				}
			}

			// 连接的数据源必须是真实数据源
			if (!isDataSourceNameExist(linkto))
			{
				logger.error("Linkto [" + linkto + "] should be a real datasource!");
				throw new DataSourceException("Linkto [" + linkto + "] should be a real datasource!");
			}
		}

		if (logger.isDebugEnabled())
		{
			logger.debug("Linkmapping configure: " + linkmapping);
		}
	}

	/**
	 * 获取软连接name最终指向的linkto
	 * 
	 * @param name 软连接name
	 * @return 最终指向
	 */
	private String getLinkmappingFinalLinkto(String name)
	{
		// 存放已遍历的名称，防止死循环
		Set<String> names = new LinkedHashSet<String>();
		names.add(name);

		String linkto = linkmapping.get(name);
		while (linkto != null && !linkto.equals(name))
		{
			// 如果当前获得的名称已经在 names 中存在，则存在死循环
			if (names.contains(linkto))
			{
				throw new RuntimeException("Linkmapping config contain a dead loop -- " + names);
			}
			names.add(linkto);

			name = linkto;
			linkto = linkmapping.get(name);
		}

		names.clear();
		names = null;

		return linkto;
	}

	/**
	 * 获得所有解析的数据源配置
	 * 
	 * @return 数据源配置列表
	 */
	public List<Properties> getDatasources()
	{
		return datasourceList;
	}

	/**
	 * 获得所有软连接配置
	 * 
	 * @return 软连接配置集合
	 */
	public Map<String, String> getLinkmapping()
	{
		return linkmapping;
	}
}
