package com.lizard.fastdb.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.lizard.fastdb.annotation.Column;
import com.lizard.fastdb.annotation.ColumnType;
import com.lizard.fastdb.annotation.GeneratorType;
import com.lizard.fastdb.annotation.Table;

/**
 * 数据库对象注解持久化处理类
 *
 * @author  SHEN.GANG
 */
public final class PersistenceUtil
{
	/**
	 * 获取注解对象的数据库表名
	 * 
	 * @param obj 注解对象
	 * @return
	 */
	public static String getTableName( Class<?> clazz )
	{
		String table_name = null;
		boolean isTableExist = true;
		
		// 解析Table注解
		Table _table = null;
		do
		{
			_table = clazz.getAnnotation( Table.class );
			if( null != _table )
			{
				table_name = _table.name();
				isTableExist = true;
			}
			else
			{
				isTableExist = false;
				clazz = clazz.getSuperclass();
				// 父类不存在，则结束下次递归操作
				if( null == clazz || "java.lang.Object".equals(clazz.getName()) )
				{
					isTableExist = true;
				}
			}
			
		} while( !isTableExist );
		
		// 获取注解中定义的表名，表名不允许为空
		if( null == table_name )
		{
			throw new IllegalArgumentException("Error: The fastDB Annotation[@Table(name='...')] can not find in your JavaBean!");
		}
		
		_table = null;
		
		return table_name;
	}
	
	/**
	 * 将首字母变成小写
	 * 
	 * @param name
	 * @return 首字母变成小写后的字符串
	 */
	public static String lowerFirst(String name)
	{
		return name.substring(0, 1).toLowerCase() + name.substring(1);
	}
	
	/**
	 * 解析符合fastDB的 Annotation 注解对象
	 * 注：该方法是升级版，支持继承注解属性获取；采用的是 getMethods 获取继承的所有公共方法，然后解析 getter 方法获得字段属性（这就要求getter方法和字段属性必须是严格的生成关系）。
	 * 
	 * @param obj 注解对象
	 * @param isPersistenced 对象是否已经被持久化了， false -- 用在 insert 时； true -- 用在 update 时
	 * @return Persistence对象
	 */
	public static Persistence parsePersistence(Object obj, boolean isPersistenced)
	{
		Class<?> clz = obj.getClass(); 	
		
		// 只有通过 getMethods 方法才能获取到继承自父类的私有字段属性
		Method[] methods = clz.getMethods();
		
		// 字段是否存在，用来递归解析注解字段
		boolean isFieldExist = true;
		// 用于内部强制异常时，中断catch；此处用在下面的 sequence 判断
		boolean isBreak = false;

		// 主键字段
		//PrimaryField p_field = null;
		// 普通字段
		ColumnField n_field = null;
		
		String col_name = null;
		Object col_val = null;
		
		Persistence persistence = new Persistence();
		persistence.setTableName( getTableName( clz ) );
		
		for( Method m : methods )
		{
			/* 
			 每次字段查找都要从当前类开始，然后再递归父类。
			 因为 getMethods()返回的数组中的元素没有排序，也没有任何特定的顺序，有可能当前类的某(些)字段被放置到了父类属性的后面，
			 而后面的递归父类操作改变了 clz 的对象，这样一来会造成这些字段被丢弃；
			 所以每次查找开始都要将 clz 对象重置为当前对象。
			 */
			clz = obj.getClass();
			
			String m_name = m.getName();
			
			// 只解析getter方法，对于Boolean类型，其getter方法可能是 isAbc
			if(!m_name.startsWith("get") && !m_name.startsWith("is"))
			{
				continue;
			}
			
			// getter 方法无参数
			if( m.getParameterTypes().length > 0 )
			{
				continue;
			}
			
			// Java注解只支持基本数据类型
			if( !ColumnType.isPrimitiveClass(m.getReturnType()) )
			{
				continue;
			}
			
			m_name = lowerFirst(m_name.startsWith("get") ? m_name.substring(3) : m_name.substring(2));
			
			// 如果当前类找不到Field定义，则递归到其父类中寻找，以此进行下去
			Field f;
			Column col;
			do
			{
				try
				{
					// 这里会产生后面的异常：如果字段是父类中继承过来的，则在当前类getDeclaredField会产生异常
					f = clz.getDeclaredField(m_name);
					col = f.getAnnotation(Column.class);
					
					// 如果该字段定义了注解，则解析；如果没有定义注解，则也不会到父类中查找
					if( null != col )
					{
						col_name = col.name();
						col_val = m.invoke(obj, new Object[]{});
						
						// 列名 @Column(name='') 为空，则使用属性名替代
						if( null == col_name || col_name.trim().length() == 0 )
						{
							col_name = f.getName();
						}
						
						// 如果是主键
						if( col.primaryKey() )
						{
							// 如果对象未被持久化 并且是 SEQUENCE 生成方式
							if( !isPersistenced && col.generatorType() == GeneratorType.SEQUENCE )
							{
								// 对于SEQUENCE 生成方式的主键，必须指定 SEQUENCE 名称
								if( null == col.sequence() || col.sequence().trim().length() == 0 )
								{
									isBreak = true;
									throw new IllegalArgumentException("The "+clz.getName()+"["+f.getName()+
												"] Annotation @Column(isPrimaryKey=true, generatorType=GeneratorType.SEQUENCE, sequence='...'), but the sequence name is empty!");
								}
								
								// sequence 方式主键的值是 seqname.NEXTVAL
								col_val = col.sequence()+".NEXTVAL";
							}
							
							// 创建主键字段
							// 扩充了ColumnField，使用 ColumnField替代
							/*p_field = new PrimaryField();
							p_field.setName( col_name );
							p_field.setValue( col_val );
							p_field.setGeneratorType( col.generatorType() );
							p_field.setType(f.getType());
							
							persistence.addPrimaryField( p_field );*/
						}
						
						// 创建字段
						n_field = new ColumnField();
						n_field.setName( col_name );
						n_field.setValue( col_val );
						n_field.setType( f.getType() );
						n_field.setPrimaryKey( col.primaryKey() );
						n_field.setGeneratorType(col.generatorType());
						
						persistence.addColumnField( n_field );
					}
					
					isFieldExist = true;
				} catch (Exception e)
				{
					// 内部强制抛出异常
					if( isBreak )
					{
						e.printStackTrace();
						return null;
					}
					
					isFieldExist = false;
					
					// 如果当前类没有找到，则到其父类中查找
					clz = clz.getSuperclass();
					// 父类不存在或是Object基类，则结束下次递归操作
					if( null == clz || "java.lang.Object".equals(clz.getName()) )
					{
						isFieldExist = true;
					}
				} 
				
			} while(!isFieldExist);
			
			n_field = null;
		}
		
		return persistence;
	}
	
	/**
	 * 分析注解对象
	 * 
	 * @param obj 注解对象
	 * @return Persistence对象
	 */
	/*@Deprecated
	public static Persistence parsePersistence( Object obj, boolean isPersistenced )
	{
		Class<?> cls = obj.getClass();
		
		// 获取声明的属性
		//Field[] fields = cls.getDeclaredFields();
		Field[] fields = cls.getFields();
		
		if( fields == null || fields.length <= 0 )
		{
			return null;
		}
		
		Field f = null;
		Column col = null;
		
		// 主键字段
		PrimaryField p_field = null;
		// 普通字段
		ColumnField n_field = null;
		
		String col_name = null;
		Object col_val = null;
		
		Persistence persistence = new Persistence();
		persistence.setTableName( getTableName( cls ) );
		
		// 遍历对象字段
		for( int i = 0; i < fields.length; i++ )
		{
			f = fields[i];
			
			// 判断属性是否是基本数据类型，对于非基本数据类型直接跳过，因为 jdk annotation 只支持基本数据类型
			if( !ColumnType.isPrimitiveClass( f.getType() ) )
			{
				continue;
			}
			
			// 获取列注解名称
			col = f.getAnnotation( Column.class );
			// 只获取含有注解的属性
			if( col != null )
			{
				// 列名
				col_name = col.name();
				// 列名 @Column(name='') 为空，则使用属性名替代
				if( col_name == null || "".equals(col_name.trim()) )
				{
					col_name = f.getName();
				}
				
				// 如果是主键
				if( col.isPrimaryKey() )
				{
					// 获取主键值
					col_val = ReflectUtils.getProperty( obj, f.getName() );
					
					// 如果对象未被持久化 并且是 SEQUENCE 生成方式
					if( !isPersistenced && col.generatorType() == GeneratorType.SEQUENCE )
					{
						// 对于SEQUENCE 生成方式的主键，必须指定 SEQUENCE 名称
						if( col.sequence() == null || "".equals(col.sequence().trim()) )
						{
							throw new IllegalArgumentException("The "+cls.getName()+"["+f.getName()+
										"] annotation @Column(isPrimaryKey=true, generatorType=GeneratorType.SEQUENCE, sequence=''), but the sequence name is empty!");
						}
						
						// sequence 方式主键的值是 seqname.NEXTVAL
						col_val = col.sequence()+".NEXTVAL";
					}
					
					// 创建主键字段
					p_field = new PrimaryField();
					p_field.setName( col_name );
					p_field.setValue( col_val );
					p_field.setGeneratorType( col.generatorType() );
					p_field.setType(f.getType());
					
					persistence.addPrimaryField( p_field );
				}
				// 普通属性
				else
				{
					// 列值
					col_val = ReflectUtils.getProperty( obj, f.getName() );
					
					// 创建普通字段
					n_field = new ColumnField();
					n_field.setName( col_name );
					n_field.setValue( col_val );
					n_field.setType( f.getType() );
					
					persistence.addColumnField( n_field );
				}
				
				col_name = null; 
				col_val = null;
				p_field = null;
				n_field = null;
				col = null;
				f = null;
			}
		}
		
		return persistence;
	}*/
	
	/**
	 * 解析一个使用了fastDB Annotation的类的注解属性映射关系
	 * 
	 * @param clazz 使用了fastDB Annotation的类
	 * @return Map： key -- 表示数据库表字段；value -- 表示JavaBean对应的注解属性名称
	 */
	public static Map<String, String> getFieldAnnotationMapping(Class<?> clazz)
	{
		// 只有通过 getMethods 方法才能获取到继承自父类的私有字段属性
		Method[] methods = clazz.getMethods();
		
		// 存放注解映射关系信息
		// 使用大小写不区分HashMap存放，因为oracle数据库默认返回的列名都是大写的
		Map<String, String> field_mapping = new CaseInsensitiveHashMap<String>();
		
		// 字段是否存在，用来递归解析注解字段
		boolean isFieldExist = true;
		
		Class<?> clz = clazz;
		String col_name = null;
		
		for( Method m : methods )
		{
			/* 
			 每次字段查找都要从当前类开始，然后再递归父类。
			 因为 getMethods()返回的数组中的元素没有排序，也没有任何特定的顺序，有可能当前类的某(些)字段被放置到了父类属性的后面，
			 而后面的递归父类操作改变了 clz 的对象，这样一来会造成这些字段被丢弃；
			 所以每次查找开始都要将 clz 对象重置为当前对象。
			 */
			clz = clazz;
			
			String m_name = m.getName();
			
			if(!m_name.startsWith("get") && !m_name.startsWith("is"))
			{
				continue;
			}
			
			// getter 方法无参数
			if( m.getParameterTypes().length > 0 )
			{
				continue;
			}
			
			// Java注解只支持基本数据类型
			if( !ColumnType.isPrimitiveClass(m.getReturnType()) )
			{
				continue;
			}
			
			m_name = lowerFirst(m_name.startsWith("get") ? m_name.substring(3) : m_name.substring(2));
			
			// 如果当前类找不到Field定义，则递归到其父类中寻找，以此进行下去
			Field f;
			Column col;
			do
			{
				try
				{
					// 这里会产生后面的异常：如果字段是父类中继承过来的，则在当前类getDeclaredField会产生异常
					f = clz.getDeclaredField(m_name);
					col = f.getAnnotation(Column.class);
					
					// 如果该字段定义了注解，则解析；如果没有定义注解，则也不会到父类中查找
					if( null != col )
					{
						col_name = col.name();
						
						// 列名 @Column(name='') 为空，则使用属性名替代
						if( null == col_name || col_name.trim().length() == 0 )
						{
							col_name = f.getName();
						}
						
						// key:数据库对应的列名， value:JavaBean被注解的属性名称
						field_mapping.put(col_name, f.getName());
					}
					
					isFieldExist = true;
				} catch (Exception e)
				{
					isFieldExist = false;
					
					// 如果当前类没有找到，则到其父类中查找
					clz = clz.getSuperclass();
					// 父类不存在 或是 Object，则结束下次递归操作
					if( null == clz || "java.lang.Object".equals(clz.getName()))
					{
						isFieldExist = true;
					}
				} 
				
			} while(!isFieldExist);
				
		}
		
		return field_mapping;
	}
	
	/**
	 * 根据未持久化的注解对象创建 Insert SQL 语句
	 * 
	 * @param bean 注解对象
	 * 
	 * @return object[0] -- 带占位符 ? 的 Insert SQL; <br/>
	 * 		   object[1] -- 占位符对应的参数值
	 */
	public static Object[] createInsertSQL( Object bean )
	{
		if( bean == null )
		{
			return null;
		}
		
		// 采用新的解析方式，用于支持注解继承
		Persistence p = parsePersistence( bean, false );
		
		if( null == p || null == p.getTableName() )
		{
			return null;
		}
		
		Object[] oo = new Object[2];
		
		StringBuilder sql_buf_1 = new StringBuilder();
		StringBuilder sql_buf_2 = new StringBuilder();
		sql_buf_1.append( "INSERT INTO ");
		sql_buf_1.append( p.getTableName());
		sql_buf_1.append( "(");
		sql_buf_2.append( " VALUES(" );
		
		List<Object> valList = new LinkedList<Object>();
		
		List<ColumnField> col_fields = p.getColumnFields();
		
		// 主键列
		for( ColumnField pf : col_fields )
		{
			if( !pf.isPrimaryKey() )
			{
				continue;
			}
			
			if( pf.getGeneratorType() == GeneratorType.SEQUENCE )
			{
				sql_buf_1.append(pf.getName());
				sql_buf_1.append(", ");
				
				sql_buf_2.append(pf.getValue());
				sql_buf_2.append(", ");
			}
			else if( pf.getGeneratorType() == GeneratorType.ASSIGN )
			{
				sql_buf_1.append(pf.getName());
				sql_buf_1.append(", ");
				
				sql_buf_2.append( "?, " );
				
				valList.add( pf.getValue() );
			}
		}
		
		// 普通列
		for( ColumnField cf : col_fields )
		{
			if( cf.isPrimaryKey() )
			{
				continue;
			}
			
			sql_buf_1.append(cf.getName());
			sql_buf_1.append(", ");
			
			sql_buf_2.append( "?, " );
			
			valList.add( cf.getValue() );
		}
		
		String sql1 = sql_buf_1.toString();
		String sql2 = sql_buf_2.toString();
		
		if( sql1.lastIndexOf(",") != -1 )
		{
			sql1 = sql1.substring(0, sql1.lastIndexOf(",")) +")";
		}
		if( sql2.lastIndexOf(",") != -1 )
		{
			sql2 = sql2.substring(0, sql2.lastIndexOf(",")) +")";
		}
		
		oo[0] = sql1 + sql2;
		oo[1] = valList.toArray();
		
		sql_buf_1 = null; 
		sql_buf_2 = null; 
		valList.clear(); 
		valList = null;
		p.clear();
		p = null;
		
		return oo;
	}
	
	/**
	 * 根据已经持久化的注解对象创建 Update SQL 语句
	 * 
	 * @param bean 已经持久化的注解对象
	 * @param updateFields 给定待更新的字段集合，如果为null，则更新全部
	 * 
	 * @return object[0] -- 带占位符 ? 的Update SQL;<br/>
	 *         object[1] -- 占位符对应的参数值
	 */
	public static Object[] createUpdateSQL( Object bean, String[] updateFields )
	{
		if( null == bean )
		{
			return null;
		}
		
		// 采用新的解析方式，用于支持注解继承
		Persistence p = parsePersistence( bean, true );
		Map<String, String> mapping = getFieldAnnotationMapping(bean.getClass());
		
		if( null == p || null == p.getTableName() )
		{
			return null;
		}
		
		Object[] oo = new Object[2];
		
		StringBuilder sql_buf_1 = new StringBuilder();
		StringBuilder sql_buf_2 = new StringBuilder();
		sql_buf_1.append( "UPDATE ");
		sql_buf_1.append(p.getTableName());
		sql_buf_1.append(" SET ");
		sql_buf_2.append( " WHERE " );
		
		List<Object> valList = new LinkedList<Object>();
		
		List<ColumnField> col_fields = p.getColumnFields();
		
		// 普通列
		for( ColumnField cf : col_fields )
		{
			/*if( cf.isPrimaryKey() )
			{
				continue;
			}*/
			
			// 判断Bean的属性，而不是@Column对应的数据表列名
			if( withinUpdateFields(mapping.get(cf.getName()), updateFields) )
			{
				sql_buf_1.append(cf.getName());
				sql_buf_1.append(" = ?, ");
				
				valList.add( cf.getValue() );
			}
		}
		
		// 主键列
		for( ColumnField pf : col_fields )
		{
			if( !pf.isPrimaryKey() )
			{
				continue;
			}
			
			sql_buf_2.append(pf.getName());
			sql_buf_2.append(" = ? AND ");
			
			valList.add( pf.getValue() );
		}
		
		String sql1 = sql_buf_1.toString();
		String sql2 = sql_buf_2.toString();
		
		if( sql1.lastIndexOf(",") != -1 )
		{
			sql1 = sql1.substring(0, sql1.lastIndexOf(","));
		}
		
		if( sql2.lastIndexOf("AND") != -1 )
		{
			sql2 = sql2.substring(0, sql2.lastIndexOf("AND"));
		}
		
		oo[0] = sql1 + sql2;
		oo[1] = valList.toArray();
		
		sql_buf_1 = null; 
		sql_buf_2 = null; 
		valList.clear(); 
		valList = null;
		p.clear();
		p = null;
		
		return oo;
	}
	
	/**
	 * 根据已经持久化的注解对象创建 Update SQL 语句
	 * 
	 * @param bean 已经持久化的注解对象
	 * 
	 * @return object[0] -- 带占位符 ? 的Update SQL;<br/>
	 *         object[1] -- 占位符对应的参数值
	 */
	public static Object[] createUpdateSQL( Object bean )
	{
		return createUpdateSQL(bean, null);
	}
	
	/**
	 * 判断一个字段名称是否在一个给定的待更新字段集合中
	 * 
	 * @param fname 待判断的字段名称
	 * @param updateFields 给定的待更新字段集合
	 * @return
	 */
	private static boolean withinUpdateFields( String fname, String[] updateFields )
	{
		if(null == updateFields || updateFields.length == 0)
		{
			return true;
		}
		
		for( String f : updateFields )
		{
			if( fname.equalsIgnoreCase(f) )
			{
				return true;
			}
		}
		
		return false;
	}
	
}
