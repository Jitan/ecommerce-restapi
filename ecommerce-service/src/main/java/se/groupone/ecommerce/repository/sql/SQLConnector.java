package se.groupone.ecommerce.repository.sql;

import se.groupone.ecommerce.exception.RepositoryException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class SQLConnector
{
	private static final String sqlDriver = "com.mysql.jdbc.Driver";

	public static Connection getConnection() throws RepositoryException
	{
		try
		{
			Class.forName(sqlDriver);
		}
		catch (ClassNotFoundException e)
		{
			throw new RepositoryException("Could not load database driver: " + e.getMessage());
		}

		try
		{
			return DriverManager
					.getConnection("jdbc:mysql://" + DBConfig.HOST + ":" + DBConfig.PORT + "/"
							+ DBConfig.DATABASE, DBConfig.USERNAME, DBConfig.PASSWORD);
		}
		catch (SQLException e)
		{
			throw new RepositoryException(
					"Could not getConnection() from DriverManager", e);
		}
	}
}
