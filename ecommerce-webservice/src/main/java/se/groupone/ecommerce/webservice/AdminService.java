package se.groupone.ecommerce.webservice;

import se.groupone.ecommerce.exception.RepositoryException;
import se.groupone.ecommerce.repository.sql.SQLConnector;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Path("admin")
public class AdminService extends WebShopService
{
	public AdminService() throws RepositoryException
	{
		super();
	}

	@POST
	public Response readAdminCommand(String command) throws RepositoryException
	{
		if (command.equals("reset-repo"))
		{
			try (Connection con = SQLConnector.getConnection();
				 Statement statement = con.createStatement())
			{
				statement.addBatch("SET FOREIGN_KEY_CHECKS = 0");
				statement.addBatch("TRUNCATE TABLE customer_cart");
				statement.addBatch("TRUNCATE TABLE product_order");
				statement.addBatch("TRUNCATE TABLE `order`");
				statement.addBatch("TRUNCATE TABLE product");
				statement.addBatch("TRUNCATE TABLE customer");
				statement.addBatch("SET FOREIGN_KEY_CHECKS = 1");
				statement.executeBatch();

			}
			catch (SQLException e)
			{
				throw new RepositoryException("Failed to reset database tables", e);
			}

			return Response.ok("SQLRepo has been reset").build();

			//			 Code below is for InMemory Repo reset
			//			 shopService = new ShopService(new
			//			 InMemoryCustomerRepository(),
			//			 new InMemoryProductRepository(), new InMemoryOrderRepository());
			//
			//			 return Response.ok("InMemoryRepo has been reset").build();
		}
		return Response.status(400).entity("Invalid command received").build();
	}
}