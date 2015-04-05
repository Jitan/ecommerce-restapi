package se.groupone.ecommerce.repository.sql;

import se.groupone.ecommerce.exception.RepositoryException;
import se.groupone.ecommerce.model.Order;
import se.groupone.ecommerce.repository.OrderRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SQLOrderRepository implements OrderRepository
{
	private final String orderTableName = "`order`";
	private final String productTableName = "product";
	private final String productOrderTableName = "product_order";
	private final SQLConnector sql;
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	public SQLOrderRepository() throws RepositoryException
	{
		try
		{
			sql = new SQLConnector(DBConfig.HOST, DBConfig.PORT, DBConfig.USERNAME,
					DBConfig.PASSWORD, DBConfig.DATABASE);
		}
		catch (SQLException e)
		{
			throw new RepositoryException(
					"Could not construct SQLOrder: Could not construct database object", e);
		}
	}

	@Override
	public void addOrder(final Order order) throws RepositoryException
	{
		final String addOrderQuery =
				"INSERT INTO " + orderTableName + " (id_order, customer_name, created) "
						+ "VALUES(?, ?, ?);";
		final String addProductsToOrderQuery =
				"INSERT INTO " + productOrderTableName + " (id_order, id_product) "
						+ "VALUES(?, ?);";
		final String updateQuantityQuery =
				"UPDATE " + productTableName + " SET quantity = quantity + ? WHERE id_product = "
						+ "?;";

		try (Connection con = SQLConnector.getConnection())
		{
			con.setAutoCommit(false);

			try (PreparedStatement prepStmtAddOrder
						 = con.prepareStatement(addOrderQuery);

				 PreparedStatement prepStmtAddProductsToOrder =
						 con.prepareStatement(addProductsToOrderQuery);

				 PreparedStatement prepStmtDecreaseProductQuantity =
						 con.prepareStatement(updateQuantityQuery))

			{
				prepStmtAddOrder.setInt(1, order.getId());
				prepStmtAddOrder.setString(2, order.getUsername());
				prepStmtAddOrder.setString(3, sdf.format(order.getDateCreated()));
				prepStmtAddOrder.executeUpdate();
				
				ArrayList<Integer> orderProductList = order.getProductIds();
				for (int productId : orderProductList)
				{
					prepStmtAddProductsToOrder.setInt(1, order.getId());
					prepStmtAddProductsToOrder.setInt(2, productId);
					prepStmtAddProductsToOrder.executeUpdate();

					prepStmtDecreaseProductQuantity.setInt(1, -1);
					prepStmtDecreaseProductQuantity.setInt(2, productId);
					prepStmtDecreaseProductQuantity.executeUpdate();
				}

				con.commit();
			}
			catch (SQLException e)
			{
				con.rollback();
				throw new RepositoryException("Could not add order!", e);
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not get SQL Connection when trying to add order!",
					e);
		}
	}

	@Override
	public Order getOrder(final int orderID) throws RepositoryException
	{
		final String customerUserName;
		Date dateOrderCreated;
		Date dateOrderShipped;
		ResultSet rs;

		try
		{
			StringBuilder orderInfoQuery = new StringBuilder();
			orderInfoQuery.append("SELECT customer_name, created, shipped FROM " + DBConfig
					.DATABASE
					+ "." + orderTableName
					+ " ");
			orderInfoQuery.append("WHERE id_order = " + orderID + ";");

			rs = sql.queryResult(orderInfoQuery.toString());
			if (!rs.isBeforeFirst())
			{
				throw new RepositoryException(
						"No matches for found for query: " + orderInfoQuery.toString());
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not query database for Order info!", e);
		}

		try
		{
			rs.next();
			customerUserName = rs.getString("customer_name");
			dateOrderCreated = rs.getDate("created");
			rs.close();
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not parse SQL values!", e);
		}

		try
		{
			StringBuilder numOfItemIDsQuery = new StringBuilder();
			numOfItemIDsQuery.append("SELECT count(id_product) FROM " + DBConfig.DATABASE + "."
					+ productOrderTableName + " ");
			numOfItemIDsQuery.append("WHERE id_order = " + orderID + ";");

			rs = sql.queryResult(numOfItemIDsQuery.toString());
			if (!rs.isBeforeFirst())
			{
				throw new RepositoryException(
						"No matches for count(id_product) in database!\nSQL QUERY: "
								+ numOfItemIDsQuery.toString());
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not count product IDs from OrderItems database!",
					e);
		}

		final int numProductsInOrder;
		try
		{
			rs.next();
			numProductsInOrder = rs.getInt(1);
			rs.close();
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not parse SQL values!", e);
		}

		try
		{
			StringBuilder orderIDsQuery = new StringBuilder();
			orderIDsQuery
					.append("SELECT id_product FROM " + DBConfig.DATABASE + "."
							+ productOrderTableName
							+ " ");
			orderIDsQuery.append("WHERE id_order = " + orderID + ";");

			rs = sql.queryResult(orderIDsQuery.toString());
			if (!rs.isBeforeFirst())
			{
				throw new RepositoryException(
						"No matches for orderID found in database!\nSQL QUERY: " + orderIDsQuery
								.toString());
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not query CustomerUserName from Order!", e);
		}

		try
		{
			ArrayList<Integer> productIds = new ArrayList<>();
			for (int i = 0; i < numProductsInOrder; i++)
			{
				rs.next();
				productIds.add(rs.getInt(1));
			}
			rs.close();
			return new Order(orderID, customerUserName, productIds, dateOrderCreated);
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not construct Order from database!", e);
		}
	}

	@Override
	public void removeOrder(final int orderID) throws RepositoryException
	{
		try
		{
			StringBuilder removeOrderItemsQuery = new StringBuilder();
			removeOrderItemsQuery
					.append("DELETE FROM " + DBConfig.DATABASE + "." + productOrderTableName + ""
							+ " ");
			removeOrderItemsQuery.append("WHERE id_order = " + orderID + ";");

			sql.query(removeOrderItemsQuery.toString());
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not query removal of OrderItems!", e);
		}

		try
		{
			StringBuilder removeOrderQuery = new StringBuilder();
			removeOrderQuery
					.append("DELETE FROM " + DBConfig.DATABASE + "." + orderTableName + " ");
			removeOrderQuery.append("WHERE id_order = " + orderID + ";");

			sql.query(removeOrderQuery.toString());
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not query removal of order!", e);
		}
	}

	@Override
	public List<Order> getOrders(final String customerUsername) throws RepositoryException
	{
		final int numOrderIDs;
		ResultSet rs;
		try
		{
			StringBuilder countOrderIDsQuery = new StringBuilder();
			countOrderIDsQuery.append("SELECT count(id_order) FROM " + DBConfig.DATABASE + "." +
					orderTableName
					+ " ");
			countOrderIDsQuery.append("WHERE customer_name = '" + customerUsername + "';");

			rs = sql.queryResult(countOrderIDsQuery.toString());
			if (!rs.isBeforeFirst())
			{
				throw new RepositoryException(
						"No matches for count(customer_name) in database!\nSQL QUERY: "
								+ countOrderIDsQuery.toString());
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not fetch OrderIDs from database!", e);
		}

		try
		{
			rs.next();
			numOrderIDs = rs.getInt(1);
			rs.close();
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not parse numeber of Order IDs!", e);
		}

		try
		{
			StringBuilder getOrderIDsQuery = new StringBuilder();
			getOrderIDsQuery
					.append("SELECT id_order FROM " + DBConfig.DATABASE + "." + orderTableName
							+ " ");
			getOrderIDsQuery.append("WHERE customer_name = '" + customerUsername + "';");

			rs = sql.queryResult(getOrderIDsQuery.toString());
			if (!rs.isBeforeFirst())
			{
				throw new RepositoryException(
						"No matches found customerUserName in database!\nSQL QUERY: "
								+ getOrderIDsQuery.toString());
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not fetch OrderIDs from database!", e);
		}

		int customerOrderIDs[] = new int[numOrderIDs];
		try
		{
			for (int i = 0; i < numOrderIDs; i++)
			{
				rs.next();
				customerOrderIDs[i] = rs.getInt(1);
			}
			rs.close();
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not parse Order IDs from ResultSet!", e);
		}

		List<Order> orderList = new ArrayList<>();
		for (int i = 0; i < numOrderIDs; i++)
		{
			orderList.add(getOrder(customerOrderIDs[i]));
		}
		return orderList;
	}

	@Override
	public int getHighestId() throws RepositoryException
	{
		ResultSet rs;
		try
		{
			final String highestOrderIDQuery =
					"SELECT MAX(id_order) FROM " + DBConfig.DATABASE + "." + orderTableName;
			rs = sql.queryResult(highestOrderIDQuery);
			if (!rs.isBeforeFirst())
			{
				throw new RepositoryException(
						"No matches found for MAX(id_order) in Orders!\nSQL QUERY: "
								+ highestOrderIDQuery.toString());
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not get query MAX(id_order)!", e);
		}

		try
		{
			rs.next();
			final int highestID = rs.getInt(1);
			rs.close();
			return highestID;
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not parse MAX(id_order)!", e);
		}
	}

	@Override
	public void updateOrder(final Order order) throws RepositoryException
	{
		// Firstly, lets convert java.util.Date format into MYSQL DATE format
		// String
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		final String sqlDateCreated = sdf.format(order.getDateCreated());
		String sqlDateShipped;
		if (order.getDateShipped() == null)
		{
			sqlDateShipped = sdf.format(new Date(0L));
		}
		else
		{
			sqlDateShipped = sdf.format(order.getDateShipped());
		}

		// Now lets update the orders created and shipped fields
		try
		{
			StringBuilder updateTableOrderQuery = new StringBuilder();
			updateTableOrderQuery
					.append("UPDATE " + DBConfig.DATABASE + "." + orderTableName + " SET ");
			updateTableOrderQuery.append("created = '" + sqlDateCreated + "', ");
			updateTableOrderQuery.append("shipped = '" + sqlDateShipped + "' ");
			updateTableOrderQuery.append("WHERE id_order = " + order.getId() + ";");

			sql.query(updateTableOrderQuery.toString());
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not query Order update!", e);
		}

		try
		{
			StringBuilder deleteOrderItemsQuery = new StringBuilder();
			deleteOrderItemsQuery
					.append("DELETE FROM " + DBConfig.DATABASE + "." + productOrderTableName + ""
							+ " ");
			deleteOrderItemsQuery.append("WHERE id_order = " + order.getId() + ";");

			sql.query(deleteOrderItemsQuery.toString());
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not delete Order items!", e);
		}

		ArrayList<Integer> productIDs = order.getProductIds();
		try
		{
			StringBuilder toOrderItemsQuery = new StringBuilder();
			for (int i = 0; i < productIDs.size(); i++)
			{
				toOrderItemsQuery
						.append("INSERT INTO " + DBConfig.DATABASE + "." + productOrderTableName
								+ ""
								+ " ");
				toOrderItemsQuery.append("(id_order, id_product) ");
				toOrderItemsQuery.append("VALUES(" + order.getId() + ", ");
				toOrderItemsQuery.append("" + productIDs.get(i) + ");");

				sql.query(toOrderItemsQuery.toString());
				toOrderItemsQuery.delete(0, toOrderItemsQuery.length());
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not add OrderIDs to Order! in database!", e);
		}
	}
}
