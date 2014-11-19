Lizard-fastDB
=============

Lizard-fastDB

1. Based on DBUtils.

2. Unified, simple, extensible data source configuration file (only one datasource configuration file. The XML).

3. Rich JDBC operation interface.

4. The ORM annotation support.

5. Multiple data sources to support：bonecp、proxool、c3p0、druid.

6. V1 version support for mysql, Oracle database.

7. Support the transaction manager (single data source, multi data source and nested transaction).

8. Rich tools support.

JAR Requirements

commons-lang-2.4.jar	             ( 必须 )

commons-io-1.4.jar	               ( 必须 )

commons-logging-1.1.1.jar	         ( 必须 )

dom4j-1.6.1.jar	                   ( 必须 )

jaxen-1.1.1.jar	                   ( 必须， 用于schema验证xml文件内容和xpath选择 )

jotm-2.0.8.jar	                   ( 如果需要使用多数据事务管理器，则必须 )

proxool-0.9.1.jar                  ( 如果使用Proxool连接池，则必须 )

proxool-cglib-0.9.1.jar	Proxool    ( 依赖包 )

c3p0-0.9.1.2.jar	                 ( 如果使用C3P0连接池，则必须 )

c3p0-oracle-thin-extras-0.9.1.2.jar( 如果使用C3P0连接池，且是Oracle数据库并thin连接方式，则必须 )

bonecp-0.7.1.jar	                 ( 如果使用BoneCP连接池，则必须 )

guava-10.0.1.jar	                 ( BoneCP依赖包 )

slf4j-simple-1.5.8.jar	           ( BoneCP依赖包 )

druid-1.0.9.jar	                   ( 如果使用Druid连接池，则必须 )

jdom-1.0.jar	                     (  用于辅助 jaxen 解析xml文件 )

