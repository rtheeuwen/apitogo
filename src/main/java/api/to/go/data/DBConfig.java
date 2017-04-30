package api.to.go.data;


import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Properties;

final class DBConfig {

	private DBConfig(){
		throw new AssertionError();
	}

	static DataSource dataSource(Properties properties) {
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setDriverClassName("org.h2.Driver");
		dataSource.setJdbcUrl(properties.getProperty("jdbcurl"));
		dataSource.setUsername(properties.getProperty("username"));
		dataSource.setPassword(properties.getProperty("password"));
		dataSource.setMaximumPoolSize(1);
		dataSource.addDataSourceProperty("cachePrepStmts", true);
		dataSource.addDataSourceProperty("prepStmtCacheSize", 15);
		dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", 1024);
		dataSource.addDataSourceProperty("useServerPrepStmts", true);
		return dataSource;
	}

	static Properties properties(){
		Properties properties = new Properties();
		properties.setProperty("jdbcurl", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
		properties.setProperty("username", "root");
		properties.setProperty("password", "root");
		return properties;
	}
}
