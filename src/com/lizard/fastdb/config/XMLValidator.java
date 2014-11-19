package com.lizard.fastdb.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

/**
 * XML文件格式验证器
 * 
 * @author SHEN.GANG
 */
public class XMLValidator
{
	/**
	 * 根据 schema 语法定义文件验证 xml文件内容是否规范
	 * 
	 * @param schemaLocation schema 文件
	 * @param xml xml 文件内容
	 * @return 最终的验证信息
	 */
	public static boolean validate(String schemaLocation, String xml)
	{
		// 获取Schema工厂类，
		// 这里的XMLConstants.W3C_XML_SCHEMA_NS_URI的值就是：http://www.w3.org/2001/XMLSchema
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		boolean res = false;
		InputStream is = null;
		try
		{
			// 读取 schema 定义文件
			is = XMLValidator.class.getClassLoader().getResourceAsStream(schemaLocation);
			StreamSource ss = new StreamSource(is);
			// 创建 schema
			Schema schema = factory.newSchema(ss);
			// 从 schema 中获取 validator
			Validator validator = schema.newValidator();
			// 解析要检测的 xml 文件内容
			ByteArrayInputStream bis = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			Source source = new StreamSource(bis);
			// 验证 xml 文件内容格式是否符合schema语法定义规范
			validator.validate(source);

			res = true;
		}
		catch (SAXException e)
		{
			e.printStackTrace();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
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

		return res;
	}
}
