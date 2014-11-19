package com.lizard.fastdb.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XML操作工具类，包含各种对XML文件和Document对象的操作方法
 * 
 * @author SHEN.GANG
 */
@SuppressWarnings("rawtypes")
public class XMLUtils implements Serializable
{
	private static final Log	log						= LogFactory.getLog(XMLUtils.class);

	private static final long	serialVersionUID		= -3665640969475129453L;

	// 命名空间
	public static final String	FASTDB_NAMESPACE			= "FASTDB_ns";

	// 为xpath增加命名空间的正则表达式
	public static final String	XPATH_NAMESPACE_REGEX	= "(?:(^|/)([\\w-])|(\\[)([\\w-]+='[^']*']))(?=(?:/|[\\w-]|\\[@?[\\w-]+='[^']*'])*)";

	// 增加的命名空间
	public static final String	XPATH_REPLACE_NAMESPACE	= "$1$3" + FASTDB_NAMESPACE + ":$2$4";

	/**
	 * 元素插入位置：插在前面，值为 insertBefore
	 */
	public static final String	INSERTBEFORE			= "insertBefore";

	/**
	 * 元素插入位置：插在最后，值为 insertAfter
	 */
	public static final String	INSERTAFTER				= "insertAfter";

	/**
	 * 元素插入位置：插在元素内部最前
	 */
	public static final String	INSERT					= "insert";

	/**
	 * 元素插入位置，插在元素内部最后
	 */
	public static final String	APPEND					= "append";

	/**
	 * 处理XML头部采用DTD验证， 以下设置忽略验证，否则无法取到节点信息
	 * 
	 * @param reader
	 *            document解析器
	 */
	private static void setReaderValidation(SAXReader reader)
	{
		// 处理XML头部采用DTD验证， 以下设置忽略验证，否则无法取到节点信息
		reader.setValidation(false);
		reader.setEntityResolver(new EntityResolver()
		{
			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
			{
				return new InputSource(new StringReader(""));
			}

		});
	}

	/**
	 * 设置DocumentFactory的命名空间
	 * 
	 * @param reader
	 *            document解析器
	 * @param namespace
	 *            命名空间
	 */
	private static void setReadernameSpace(SAXReader reader, String namespace)
	{
		Map<String, String> nsMap = new HashMap<String, String>();
		nsMap.put(FASTDB_NAMESPACE, namespace);
		reader.getDocumentFactory().setXPathNamespaceURIs(nsMap);
		// 不需要重新读取一次doc
		// document = saxReader.read(xmlFile);
	}

	/**
	 * 根据输入流InputStream获取XML文件Document对象
	 * <br>
	 * 注意：XMLUtils不会关闭 InputStream，请自己手工关闭，避免输入流泄流。
	 * 
	 * @param inputStream xml文档输入流
	 * @return XML Document对象
	 */
	public static Document getDocumentFromStream( InputStream inputStream )
	{
		InputStreamReader isr = null;
		BufferedReader br = null;

		SAXReader saxReader = new SAXReader();
		Document document = null;

		try
		{
			isr = new InputStreamReader(inputStream, "UTF-8");
			br = new BufferedReader(isr, 1024);

			setReaderValidation(saxReader);

			document = saxReader.read(br);

			String namespace_uri = document.getRootElement().getNamespaceURI();
			if (!StringUtils.isEmptyString(namespace_uri))
			{
				setReadernameSpace(saxReader, namespace_uri);
			}
		}
		catch (DocumentException e)
		{
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (br != null)
			{
				try
				{
					br.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				br = null;
			}
			
			if (isr != null)
			{
				try
				{
					isr.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				isr = null;
			}
		}

		return document;
	}
	
	/**
	 * 从jar包中加载xml文件为document对象<br>
	 * 该方法优先从classpath进行加载，如果没有找到，再通过classloader从jar包中进行加载
	 * 
	 * @param loader
	 *            指定jar包中类的类加载器
	 * @param path
	 *            xml文件在jar包中的相对位置
	 * @return Document对象
	 */
	public static Document getDocumentFromJar(ClassLoader loader, String path)
	{
		Document document = getDocumentFromClasspath(path);

		if (document == null && loader != null)
		{
			SAXReader saxReader = new SAXReader();
			InputStream is = loader.getResourceAsStream(path);
			try
			{
				setReaderValidation(saxReader);

				document = saxReader.read(is);

				String namespace_uri = document.getRootElement().getNamespaceURI();
				if (!StringUtils.isEmptyString(namespace_uri))
				{
					setReadernameSpace(saxReader, namespace_uri);
				}
			}
			catch (DocumentException e)
			{
				e.printStackTrace();
			}
		}
		return document;
	}

	/**
	 * 根据xml文件对象获得其Document对象
	 * 
	 * @param xmlFile
	 *            待解析的XML文件对象
	 * @return document对象
	 * @throws IOException
	 */
	public static Document getDocumentFromFile(File xmlFile) throws IOException
	{
		if (!xmlFile.exists())
		{
			throw new FileNotFoundException("file " + xmlFile + " does not exist!");
		}
		else if (xmlFile.isDirectory())
		{
			throw new IOException("file " + xmlFile + " exists but is a directory!");
		}
		else if (xmlFile.canRead() == false)
		{
			throw new IOException("file " + xmlFile + " cannot be read!");
		}

		Document document = null;
		SAXReader saxReader = new SAXReader();

		try
		{
			setReaderValidation(saxReader);

			document = saxReader.read(xmlFile);

			String namespace_uri = document.getRootElement().getNamespaceURI();
			if (!StringUtils.isEmptyString(namespace_uri))
			{
				setReadernameSpace(saxReader, namespace_uri);
			}

		}
		catch (DocumentException e)
		{
			e.printStackTrace();
		}

		return document;
	}

	/**
	 * 根据指定<b>相对于项目根目录</b>的XML文件路径获得该文件的document对象
	 * 
	 * @param classpathFile
	 *            XML文件相对于项目根目录的路径，路径前面不能有/
	 * @return XML文件的document对象
	 * @throws IOException
	 */
	public static Document getDocumentFromClasspath(String classpathFile)
	{
		/*InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;

		SAXReader saxReader = new SAXReader();
		Document document = null;

		try
		{
			is = XMLUtils.class.getClassLoader().getResourceAsStream(classpathFile);
			isr = new InputStreamReader(is, "UTF-8");
			br = new BufferedReader(isr, 1024);

			setReaderValidation(saxReader);

			document = saxReader.read(br);

			String namespace_uri = document.getRootElement().getNamespaceURI();
			if (!StringUtils.isEmptyString(namespace_uri))
			{
				setReadernameSpace(saxReader, namespace_uri);
			}
		}
		catch (DocumentException e)
		{
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (br != null)
			{
				try
				{
					br.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				br = null;
			}
			if (isr != null)
			{
				try
				{
					isr.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				isr = null;
			}
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
		}*/
		
		Document document = null;
		InputStream is = XMLUtils.class.getClassLoader().getResourceAsStream(classpathFile);
		if( null != is )
		{
			document = getDocumentFromStream(is);
			try
			{
				is.close();
				is = null;
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			
		}
		
		return document;
	}

	/**
	 * 根据指定的XML文件绝对路径获得该文件的document对象
	 * 
	 * @param filepath
	 *            文件绝对路径
	 * @return XML文件的document对象
	 * @throws IOException
	 */
	public static Document getDocumentFromFilepath(String filepath) throws IOException
	{
		File file = new File(filepath);

		Document document = getDocumentFromFile(file);

		return document;
	}

	/**
	 * 将指定的xml格式字符串转换为Document对象，使用UTF-8编码
	 * 
	 * @param xmlContent
	 *            待解析的XML字符串
	 * @return document对象
	 */
	public static Document getDocumentFromString(String xmlContent)
	{
		SAXReader saxReader = new SAXReader();
		Document document = null;

		try
		{
			setReaderValidation(saxReader);

			document = saxReader.read(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));

			String namespace_uri = document.getRootElement().getNamespaceURI();
			if (!StringUtils.isEmptyString(namespace_uri))
			{
				setReadernameSpace(saxReader, namespace_uri);
			}

		}
		catch (DocumentException e)
		{
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}

		return document;
	}

	/**
	 * 根据xpath路径获得指定的节点信息列表 <br>
	 * 列表中每一个Map代表一个节点，key为节点的属性名称，value为对应属性的值 <br>
	 * <br>
	 * <b>key统一为小写</b><br>
	 * <br>
	 * key为<b>node_name</b>表示该节点的名称<br>
	 * key为<b>node_value</b>表示该节点的值
	 * 
	 * @param document
	 *            待解析的文档对象
	 * @param xpath
	 *            节点路径
	 * @return 节点列表
	 */
	public static List<Map<String, String>> getNodeInfoList(Document document, String xpath)
	{
		List<Map<String, String>> result = null;

		// 进行命名空间处理
		if (!StringUtils.isEmptyString(document.getRootElement().getNamespaceURI()))
		{
			// 将xpath中增加命名空间
			xpath = xpath.replaceAll(XPATH_NAMESPACE_REGEX, XPATH_REPLACE_NAMESPACE);
		}

		List list = document.selectNodes(xpath);

		if (list != null && list.size() > 0)
		{
			result = new ArrayList<Map<String, String>>();

			for (Object obj : list)
			{
				Element element = (Element) obj;

				Map<String, String> map = new HashMap<String, String>();

				map.put("node_name", element.getName()); // 节点名称
				map.put("node_value", element.getText()); // 节点值

				List attrList = element.attributes();

				if (attrList != null && attrList.size() > 0)
				{
					for (Object o : attrList)
					{
						Node attr = (Node) o;

						map.put(attr.getName().toLowerCase(), attr.getText());
					}
				}

				result.add(map);
			}
		}

		return result;
	}

	/**
	 * 根据xpath获得指定document对象节点的属性的值
	 * 
	 * @param document
	 *            待解析的document对象
	 * @param xpath
	 *            节点XPath
	 * @return 指定节点属性的值列表
	 */
	public static List<String> getNodeAttributeValue(Document document, String xpath)
	{
		List<String> result = null;

		// 进行命名空间处理
		if (!StringUtils.isEmptyString(document.getRootElement().getNamespaceURI()))
		{
			// 将xpath中增加命名空间
			xpath = xpath.replaceAll(XPATH_NAMESPACE_REGEX, XPATH_REPLACE_NAMESPACE);
		}

		List nodeList = document.selectNodes(xpath);

		if (nodeList != null && nodeList.size() > 0)
		{
			result = new ArrayList<String>();

			int begin = xpath.lastIndexOf("@");
			int end = xpath.indexOf("]", begin + 1);

			if (begin == -1 || end == -1)
			{
				throw new IllegalArgumentException("no attribute has be assigned in xpath!");
			}

			String attrName = xpath.substring(begin, end);

			for (Object obj : nodeList)
			{
				Node attr = (Node) obj;

				result.add(attr.valueOf(attrName));
			}
		}

		return result;
	}

	/**
	 * 根据xpath获得指定document对象节点属性的值，如果xpath有多个匹配结果，则只返回第一个值
	 * 
	 * @param document
	 *            待解析的document对象
	 * @param xpath
	 *            节点XPath
	 * @return 指定节点属性的值
	 */
	public static String getSingleNodeAttributeValue(Document document, String xpath)
	{
		List<String> result = getNodeAttributeValue(document, xpath);

		if (result != null && result.size() > 0)
		{
			return result.get(0);
		}
		else
		{
			return null;
		}
	}

	/**
	 * 获得指定节点的值列表
	 * 
	 * @param document
	 *            待解析的document对象
	 * @param xpath
	 *            节点XPath
	 * @return 节点值列表
	 */
 	public static List<String> getNodeValue(Document document, String xpath)
	{
		List<String> result = null;

		// 进行命名空间处理
		if (!StringUtils.isEmptyString(document.getRootElement().getNamespaceURI()))
		{
			// 将xpath中增加命名空间
			xpath = xpath.replaceAll(XPATH_NAMESPACE_REGEX, XPATH_REPLACE_NAMESPACE);
		}

		List nodeList = document.selectNodes(xpath);

		if (nodeList != null && nodeList.size() > 0)
		{
			result = new ArrayList<String>();

			for (Object obj : nodeList)
			{
				Node node = (Node) obj;

				result.add(node.getText());
			}
		}

		return result;
	}

	/**
	 * 获得单一节点的值，如果xpath有多个匹配结果，则只返回第一个值
	 * 
	 * @param document
	 *            待解析的document
	 * @param xpath
	 *            节点XPath
	 * @return 节点值
	 */
	public static String getSingleNodeValue(Document document, String xpath)
	{
		List<String> result = getNodeValue(document, xpath);

		if (result != null && result.size() > 0)
		{
			return result.get(0);
		}
		else
		{
			return null;
		}
	}

	public static String setXPathNamespace( Element ele, String xpath ) {
		// 进行命名空间处理
		if (!StringUtils.isEmptyString(ele.getNamespaceURI()))
		{
			// 将xpath中增加命名空间
			xpath = xpath.replaceAll(XPATH_NAMESPACE_REGEX, XPATH_REPLACE_NAMESPACE);
		}
		
		return xpath;
	}
	
	/**
	 * 保存document对象为文件，默认UTF-8编码
	 * 
	 * @param document
	 *            待保存的document对象
	 * @param filepath
	 *            待保存的绝对路径
	 * @param fileEncoding
	 *            文件编码
	 * @return 成功 == true，失败 == false
	 */
	public static boolean saveFile(Document document, String filepath, String fileEncoding)
	{
		boolean result = false;

		FileOutputStream outStream = null;
		OutputStreamWriter outWriter = null;
		XMLWriter writer = null;

		if (fileEncoding == null || "".equals(fileEncoding.trim()))
		{
			fileEncoding = "UTF-8";
		}

		try
		{
			outStream = new FileOutputStream(filepath);
			outWriter = new OutputStreamWriter(outStream, fileEncoding);
			writer = new XMLWriter(outWriter);

			writer.write(document);

			result = true;
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (writer != null)
				{
					writer.close();
				}

				if (outWriter != null)
				{
					outWriter.close();
				}

				if (outStream != null)
				{
					outStream.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		return result;
	}

	/**
	 * 保存document对象为XML文件，默认UTF-8编码。
	 * 
	 * @param document
	 *            document对象
	 * @param filepath
	 *            保存路径
	 * @return 成功 == true，失败 == false
	 */
	public static boolean saveFile(Document document, String filepath)
	{
		boolean result = false;

		result = saveFile(document, filepath, "UTF-8");

		return result;
	}

	/**
	 * 将xml字符串内容保存为xml文件
	 * 
	 * @param xmlContent
	 *            XML字符串
	 * @param filepath
	 *            文件路径
	 * @return 成功 == true，失败 == false
	 */
	public static boolean saveFile(String xmlContent, String filepath)
	{
		boolean result = false;

		Document document = getDocumentFromString(xmlContent);

		result = saveFile(document, filepath, "UTF-8");

		return result;
	}

	/**
	 * 将XML字符串内容保存为指定编码的XML文件
	 * 
	 * @param xmlContent
	 *            XML字符串
	 * @param filepath
	 *            文件路径
	 * @param fileEncoding
	 *            文件编码
	 * @return 成功 == true，失败 == false
	 */
	public static boolean saveFile(String xmlContent, String filepath, String fileEncoding)
	{
		boolean result = false;

		Document document = getDocumentFromString(xmlContent);

		result = saveFile(document, filepath, fileEncoding);

		return result;
	}

	/**
	 * 根据 schema 语法定义文件验证 xml 文件内容是否规范
	 * 
	 * @param schemaFile
	 *            schema 文件
	 * @param xmlFile
	 *            xml 文件
	 * @return 验证成功 == true，验证失败 == false
	 */
	public static boolean schemaValidate(String schemaFile, String xmlFile)
	{
		boolean result = false;

		Document document = getDocumentFromClasspath(xmlFile);

		result = schemaValidateXMLContent(schemaFile, document.asXML());

		return result;
	}

	/**
	 * 根据 schema 语法定义文件验证 xml 文件内容是否规范
	 * 
	 * @param schemaFile
	 *            schema 文件
	 * @param xmlContent
	 *            xml 文件内容
	 * @return 验证成功 == true，验证失败 == false
	 */

	public static boolean schemaValidateXMLContent(String schemaFile, String xmlContent)
	{
		boolean result = false;

		// 获取Schema工厂类，
		// 这里的XMLConstants.W3C_XML_SCHEMA_NS_URI的值就是：http://www.w3.org/2001/XMLSchema
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		try
		{
			// 读取 schema 定义文件
			String fileName = XMLUtils.class.getClassLoader().getResource(schemaFile).toString();
			fileName = fileName.substring(fileName.indexOf('/'));
			File schemaSource = new File(fileName);

			// 创建 schema
			Schema schema = factory.newSchema(schemaSource);

			// 从 schema 中获取 validator
			Validator validator = schema.newValidator();

			// 解析要检测的 xml 文件内容
			ByteArrayInputStream bis = new ByteArrayInputStream(xmlContent.getBytes("UTF-8"));

			Source source = new StreamSource(bis);

			// 验证 xml 文件内容格式是否符合schema语法定义规范
			validator.validate(source);

			result = true;
		}
		catch (SAXException e)
		{
			log.error("Validate dconfigure file error：", e);
		}
		catch (IOException e)
		{
			log.error("An error has happend when validate file：", e);
		}

		return result;
	}

	/**
	 * 在指定的父元素的指定位置插入xml字符串
	 * 
	 * @param filepath
	 *            xml文件路径
	 * @param xpath
	 *            父元素的xpath
	 * @param xml
	 *            待插入的xml字符串
	 * @param position
	 *            插入位置，最前 <code>XMLUtils.INSERTBEFORE</code> 或 最后 <code>XMLUtils.INSERTAFTER</code>
	 * @param isSave
	 *            是否保存更改后的文件
	 * @return 插入成功后的xml文件内容字符串
	 */
	public static String insert(String filepath, String xpath, String xml, boolean isSave)
	{
		String result = insert(filepath, new String[]{ xpath }, new String[]{ xml }, isSave);

		return result;
	}

	public static String insert(String filepath, String[] xpaths, String[] xmls, boolean isSave)
	{
		String result = null;

		try
		{
			Document document = getDocumentFromFilepath(filepath);

			if (xpaths != null && xpaths.length > 0)
			{
				for (int i = 0; i < xpaths.length; i++)
				{
					insert(document, xpaths[i], xmls[i]);
				}
			}

			if (isSave)
			{
				FileUtils.forceDelete(new File(filepath));
				saveFile(document, filepath);
			}

			result = document.asXML();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * 向指定xml文档对象指定元素内插入xml
	 * 
	 * @param document
	 *            xml文档对象
	 * @param xpath
	 *            元素xpath
	 * @param xml
	 *            插入内容
	 * @return xml文档对象最终内容字符串
	 */
	@SuppressWarnings("unchecked")
	public static String insert(Document document, String xpath, String xml)
	{
		String namespace = document.getRootElement().getNamespaceURI();
		if (!StringUtils.isEmptyString(xpath) && !StringUtils.isEmptyString(xml))
		{
			((Element) document.selectSingleNode(xpath)).elements().addAll(0, getElementListFromXMl(xml, namespace));
		}
		else
		{
			document.getRootElement().elements().addAll(0, getElementListFromXMl(xml, namespace));
		}

		return document.asXML();
	}

	/**
	 * 在xml文件根元素最后插入xml字符串
	 * 
	 * @param filepath
	 *            xml文件路径
	 * @param xml
	 *            待插入的xml字符串
	 * @param isSave
	 *            是否保存更改后的文件
	 * @return 插入成功后的xml文件内容字符串
	 */
	public static String append(String filepath, String xpath, String xml, boolean isSave)
	{
		String result = append(filepath, new String[]{ xpath }, new String[]{ xml }, isSave);

		return result;
	}

	public static String append(String filepath, String[] xpaths, String[] xmls, boolean isSave)
	{
		String result = null;

		try
		{
			Document document = getDocumentFromFilepath(filepath);

			if (xpaths != null && xpaths.length > 0)
			{
				for (int i = 0; i < xpaths.length; i++)
				{
					append(document, xpaths[i], xmls[i]);
				}
			}

			if (isSave)
			{
				FileUtils.forceDelete(new File(filepath));
				saveFile(document, filepath);
			}

			result = document.asXML();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public static String append(Document document, String xpath, String xml)
	{
		String namespace = document.getRootElement().getNamespaceURI();
		if (!StringUtils.isEmptyString(xpath) && !StringUtils.isEmptyString(xml))
		{
			((Element) document.selectSingleNode(xpath)).elements().addAll(getElementListFromXMl(xml, namespace));
		}
		else
		{
			document.getRootElement().elements().addAll(getElementListFromXMl(xml, namespace));
		}

		return document.asXML();
	}

	/**
	 * 在指定的元素前面插入xml字符串
	 * 
	 * @param filepath
	 *            xml文件路径
	 * @param xpath
	 *            指定元素的xpath，xpath为空表示在根元素最前面插入
	 * @param xml
	 *            待插入的xml字符串
	 * @param isSave
	 *            是否保存更改后的文件
	 * @return 插入成功后的xml文件内容字符串
	 */
	public static String insertBefore(String filepath, String xpath, String xml, boolean isSave)
	{
		String result = insertBefore(filepath, new String[]{ xpath }, new String[]{ xml }, isSave);

		return result;
	}

	/**
	 * 在指定的元素前面插入xml字符串
	 * 
	 * @param filepath
	 *            xml文件路径
	 * @param xpath
	 *            指定元素的xpath，xpath为空表示在根元素最前面插入
	 * @param xml
	 *            待插入的xml字符串
	 * @param isSave
	 *            是否保存更改后的文件
	 * @return 插入成功后的xml文件内容字符串
	 */
	public static String insertBefore(String filepath, String[] xpaths, String[] xmls, boolean isSave)
	{
		String result = null;

		try
		{
			Document document = getDocumentFromFilepath(filepath);

			if (xpaths != null && xpaths.length > 0)
			{
				for (int i = 0; i < xpaths.length; i++)
				{
					insertBefore(document, xpaths[i], xmls[i]);
				}
			}

			if (isSave)
			{
				FileUtils.forceDelete(new File(filepath));
				saveFile(document, filepath);
			}

			result = document.asXML();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * 向xml文档对象指定位置前面插入xml
	 * 
	 * @param document
	 *            xml文档对象
	 * @param xpath
	 *            位置xpath
	 * @param xml
	 *            插入内容
	 * @return xml文档最终内容字符串
	 */
	@SuppressWarnings("unchecked")
	public static String insertBefore(Document document, String xpath, String xml)
	{
		String namespace = document.getRootElement().getNamespaceURI();
		if (!StringUtils.isEmptyString(xpath) && !StringUtils.isEmptyString(xml))
		{
			List<Element> list = ((Element) document.selectSingleNode(xpath)).getParent().elements();

			for (int j = 0; j < list.size(); j++)
			{
				if (list.get(j).matches(xpath))
				{
					list.addAll(j, getElementListFromXMl(xml, namespace));
					break;
				}
			}
		}
		else
		{
			document.getRootElement().elements().addAll(0, getElementListFromXMl(xml, namespace));
		}

		return document.asXML();
	}

	/**
	 * 在指定的元素后面插入xml字符串
	 * 
	 * @param filepath
	 *            xml文件路径
	 * @param xpath
	 *            指定元素的xpath，为空表示在文档根元素最后插入
	 * @param xml
	 *            待插入的xml字符串
	 * @param isSave
	 *            修改后是否保存文件
	 * @return 插入成功后的xml文件内容字符串
	 */
	public static String insertAfter(String filepath, String xpath, String xml, boolean isSave)
	{
		String result = insertAfter(filepath, new String[]{ xpath }, new String[]{ xml }, isSave);

		return result;
	}

	/**
	 * 在指定的元素后面插入xml字符串
	 * 
	 * @param filepath
	 *            xml文件路径
	 * @param xpaths
	 *            指定元素的xpath，为空表示在文档根元素最后插入
	 * @param xmls
	 *            待插入的xml字符串
	 * @param isSave
	 *            是否保存更改后的文件
	 * @return 插入成功后的xml文件内容字符串
	 */
	public static String insertAfter(String filepath, String[] xpaths, String[] xmls, boolean isSave)
	{
		String result = null;

		try
		{
			Document document = getDocumentFromFilepath(filepath);

			if (xpaths != null && xpaths.length > 0)
			{
				for (int i = 0; i < xpaths.length; i++)
				{
					insertAfter(document, xpaths[i], xmls[i]);
				}
			}

			if (isSave)
			{
				FileUtils.forceDelete(new File(filepath));
				saveFile(document, filepath);
			}

			result = document.asXML();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * 向指定的xml文档对象中指定位置后面插入xml
	 * 
	 * @param document
	 *            xml文档对象
	 * @param xpath
	 *            插入位置xpath
	 * @param xml
	 *            插入内容xml
	 * @return xml文档最终内容字符串
	 */
	@SuppressWarnings("unchecked")
	public static String insertAfter(Document document, String xpath, String xml)
	{
		String namespace = document.getRootElement().getNamespaceURI();
		if (!StringUtils.isEmptyString(xpath) && !StringUtils.isEmptyString(xml))
		{
			List<Element> list = ((Element) document.selectSingleNode(xpath)).getParent().elements();

			for (int j = 0; j < list.size(); j++)
			{
				if (list.get(j).matches(xpath))
				{
					if (j < list.size() - 1)
					{
						list.addAll(j + 1, getElementListFromXMl(xml, namespace));
					}
					else
					{
						list.addAll(getElementListFromXMl(xml, namespace));
					}
					break;
				}
			}
		}
		else
		{
			document.getRootElement().elements().addAll(getElementListFromXMl(xml, namespace));
		}

		return document.asXML();
	}

	/**
	 * 根据xml字符串获得Element元素列表
	 * 
	 * @param xml
	 *            xml字符串
	 * @param namespace
	 *            命名空间
	 * @return Element列表
	 */
	@SuppressWarnings("unchecked")
	private static List<Element> getElementListFromXMl(String xml, String namespace)
	{
		String root = RandomUtils.randomString(5);

		xml = "<_" + root + (StringUtils.isEmptyString(namespace) ? "" : " xmlns=\"" + namespace + "\"") + ">" + xml + "</_" + root + ">";

		List<Element> result = new ArrayList<Element>();
		List<Element> elementList = getDocumentFromString(xml).getRootElement().elements();
		for (int i = 0; i < elementList.size(); i++)
		{
			result.add(elementList.get(i).createCopy());
		}

		return result;
	}
}
