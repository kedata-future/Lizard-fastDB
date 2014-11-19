package com.lizard.fastdb.util;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 反射工具类
 * 
 * @author SHEN.GANG
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class ReflectUtils
{
	/**
	 * 实例缓存<br>
	 * key：className<br>
	 * value：instance
	 */
	private volatile static Map<String, Object>	Instance_Cache	= new HashMap<String, Object>();

	/**
	 * 返回与带有给定字符串名的类或接口相关联的 Class 对象
	 * 
	 * @param name 所需类的完全限定名
	 * @return 具有指定名的类的 Class 对象
	 * @throws ClassNotFoundException
	 */
	public static Class classForName(String name) throws ClassNotFoundException
	{
		try
		{
			// 获取当前环境的类加载器
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			// 使用给定的类加载器，返回与带有给定字符串名的类或接口相关联的 Class 对象
			if (contextClassLoader != null)
			{
				return contextClassLoader.loadClass(name);
			}
		}
		catch (Throwable ignore)
		{
		}

		return Class.forName(name);
	}

	/**
	 * 优先从缓存中获得实例，如果缓存中不存在，再实例化对象，并放入缓存
	 * 
	 * @param className 所需类的完全限定名
	 * @return 具有指定名的类的对象
	 * @throws Exception
	 */
	public static Object newInstanceFromCache(String className) throws Exception
	{
		Object o = Instance_Cache.get(className);
		if (o == null)
		{
			o = newInstance(className, new Object[]{});
			Instance_Cache.put(className, o);
		}
		return o;
	}

	/**
	 * 实例化字符串对象
	 * 
	 * @param className 所需类的完全限定名
	 * @param args 构造参数值
	 * @return 具有指定名的类的对象
	 * @throws Exception
	 */
	public static Object newInstance(String className, Object... args) throws Exception
	{
		Class cls = classForName(className);
		// 如果构造方法含有参数
		if (args != null && args.length > 0)
		{
			Constructor cons = cls.getConstructor(initArgsClass(args));
			return cons.newInstance(args);
		}
		// 无参构造对象
		else
		{
			Object o = cls.newInstance();
			return o;
		}

	}

	/**
	 * 获取对象的属性值 <br>
	 * <br>
	 * 只能获取含有getter方法的属性值
	 * 
	 * @param obj 对象
	 * @param fieldName 对象属性名称
	 * @return 属性值
	 * @throws Exception
	 */
	public static Object getProperty(Object obj, String fieldName)
	{
		if (obj == null || fieldName == null || "".equals(fieldName.trim()))
		{
			return null;
		}

		String method_name = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
		Method method;
		try
		{
			method = obj.getClass().getMethod("get" + method_name);

			return method.invoke(obj, new Object[]{});
		}
		catch (SecurityException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchMethodException e)
		{
			// 对于 boolean 类型属性的 getter 方法可能是 isXXX
			try
			{
				method = obj.getClass().getMethod("is" + method_name);

				return method.invoke(obj, new Object[]{});
			}
			catch (Exception e1)
			{
				e1.printStackTrace();
			}
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (InvocationTargetException e)
		{
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 执行某对象的方法
	 * 
	 * @param obj 对象
	 * @param methodName 方法名
	 * @param args 方法参数
	 * @return 方法返回值
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static Object invokeMethod(Object obj, String methodName, Object... args) throws Exception
	{
		// Class cls = obj.getClass();
		// Method method = cls.getMethod(methodName, initArgsClass(args));
		// return method.invoke(obj, args);

		Method method = getMethod(obj.getClass(), methodName);

		return method.invoke(obj, convertMethodValue(method, args));
	}

	/**
	 * 执行某个类的静态方法
	 * 
	 * @param className 所需类的完全限定名
	 * @param methodName 要执行的方法名
	 * @param args 方法参数
	 * @return 方法返回值
	 * @throws Exception
	 */
	public static Object invokeStaticMethod(String className, String methodName, Object... args) throws Exception
	{
		// Class cls = classForName(className);
		// Method method = cls.getMethod(methodName, initArgsClass(args));
		// return method.invoke(null, args);

		Method method = getMethod(classForName(className), methodName);

		// 由于是静态方法，所以不需要借助实例运行：null
		return method.invoke(null, convertMethodValue(method, args));
	}

	private static Class[] initArgsClass(Object... args)
	{
		Class[] argsCls = null;
		if (args != null && args.length > 0)
		{
			argsCls = new Class[args.length];
			for (int i = 0; i < args.length; i++)
			{
				if (args[i] != null)
				{
					argsCls[i] = args[i].getClass();
				}
				else
				{
					throw new IllegalArgumentException("The Constructor can not accept NULL Parameter!");
				}
			}
		}
		else
		{
			argsCls = new Class[]{};
		}

		return argsCls;
	}

	/**
	 * 获取一个类属性对应的WriteMethod
	 * 
	 * @param cls 类
	 * @param propertyName 属性名称
	 * @return 该属性名对应的WriteMethod方法，即：setter方法
	 */
	private static Method getMethod(Class cls, String propertyName)
	{
		Method _method = null;
		try
		{
			/*
			 * BeanInfo bi = Introspector.getBeanInfo( cls ); PropertyDescriptor[] pds = bi.getPropertyDescriptors();
			 */

			_method = new PropertyDescriptor(propertyName, cls).getWriteMethod();

		}
		catch (IntrospectionException e)
		{
			e.printStackTrace();
		}

		return _method;
	}

	/**
	 * 根据方法(Method)参数类型转换参数值类型 注： Java反射类型通常用于类类型，默认不适用于基本数据类型，通过该方法转换后，则可以对基本数据类型进行反射赋值。
	 * 
	 * @param _method Method对象
	 * @param values 参数值
	 * @return 转换类型后的参数值
	 */
	private static Object[] convertMethodValue(Method _method, Object[] values)
	{
		Object[] newVals = null;

		if (values != null && values.length > 0)
		{
			newVals = new Object[values.length];

			Class<?>[] valTypes = _method.getParameterTypes();
			Class _cls = null;
			String _val = null;

			for (int i = 0, len = valTypes.length; i < len; i++)
			{
				_cls = valTypes[i];
				_val = values[i] != null ? values[i].toString() : null;

				if (Integer.TYPE == _cls)
				{
					newVals[i] = Integer.valueOf(_val);
				}
				else if (Long.TYPE == _cls)
				{
					newVals[i] = Long.valueOf(_val);
				}
				else if (Boolean.TYPE == _cls)
				{
					newVals[i] = Boolean.valueOf(_val);
				}
				else if (Float.TYPE == _cls)
				{
					newVals[i] = Float.valueOf(_val);
				}
				else if (Double.TYPE == _cls)
				{
					newVals[i] = Double.valueOf(_val);
				}
				else if (Short.TYPE == _cls)
				{
					newVals[i] = Short.valueOf(_val);
				}
				else if (Byte.TYPE == _cls)
				{
					newVals[i] = Byte.valueOf(_val);
				}
				else if (Character.TYPE == _cls)
				{
					newVals[i] = Character.valueOf(_val.toCharArray()[0]);
				}
				else
				{
					newVals[i] = values[i];
				}
			}
		}

		return newVals;
	}

}
