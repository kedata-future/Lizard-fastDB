package com.lizard.fastdb.connection;

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * 对象属性设置反射工具类
 * 
 * @author SHEN.GANG
 */
public class ConfigureReflect
{
	/**
	 * Sets the properties by reading off entries in the given parameter (where each key is equivalent to the field name)
	 * 
	 * @param props Parameter list to set
	 * @throws Exception on error
	 */
	public static void setProperties(Object obj, Properties props) throws Exception
	{
		// Use reflection to read in all possible properties of int, String or boolean.
		for (Method method : obj.getClass().getMethods())
		{
			String tmp = null;
			if (method.getName().startsWith("is"))
			{
				tmp = lowerFirst(method.getName().substring(2));
			}
			else if (method.getName().startsWith("set"))
			{
				tmp = lowerFirst(method.getName().substring(3));
			}
			else
			{
				continue;
			}
			
			Class<?>[] pt_clzs = method.getParameterTypes();

			if( null != pt_clzs && pt_clzs.length == 1 )
			{
				// 使用 get() 替换 getProperty()
				// 因为如果Properties使用put()一个非String类型的值，则 getProperty() 将返回 null
				// String val = props.getProperty(tmp);
				Object _val = props.get(tmp);
				if( null == _val )
				{
					continue;
				}
				
				String val = String.valueOf(_val).trim();
				
				Class<?> pt_clz = pt_clzs[0];
				
				if( pt_clz.equals(int.class) )
				{
					if (!val.matches("^\\d+$"))
					{
						continue;
					}
					
					try
					{
						method.invoke(obj, Integer.parseInt(val));
					}
					catch (NumberFormatException e)
					{
						// do nothing, use the default value
						System.out.println("Warning: The value["+val+"] out of Integer bounds.");
					}
				}
				else if( pt_clz.equals(long.class) )
				{
					if (!val.matches("^\\d+$"))
					{
						continue;
					}
					
					try
					{
						method.invoke(obj, Long.parseLong(val));
					}
					catch (NumberFormatException e)
					{
						// do nothing, use the default value
						System.out.println("Warning: The value["+val+"] out of Long bounds.");
					}
				}
				else if( pt_clz.equals(String.class) )
				{
					method.invoke(obj, val);
				}
				else if( pt_clz.equals(boolean.class) )
				{
					method.invoke(obj, Boolean.parseBoolean(val));
				}
			}
		}
	}

	/**
	 * 将首字母变成小写
	 * 
	 * @param name
	 * @return 首字母变成小写后的字符串
	 */
	private static String lowerFirst(String name)
	{
		return name.substring(0, 1).toLowerCase() + name.substring(1);
	}
}
