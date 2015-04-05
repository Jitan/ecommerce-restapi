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
	public Order getOrder(final int orderId) throws RepositoryException
	{
		final String getOrderQuery = "SELECT customer_name, created FROM " + orderTableName
				+ " WHERE id_order = ?;";
		final String getProductsForOrderQuery = "SELECT id_product FROM " + productOrderTableName
				+ " WHERE id_order = ?;";
		final String customerName;
		final Date dateCreated;
		ArrayList<Integer> productIds = new ArrayList<>();

		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement prepStmtGetOrder = con.prepareStatement(getOrderQuery);
			 PreparedStatement prepStmtGetProductsForOrder =
					 con.prepareStatement(getProductsForOrderQuery))
		{
			ResultSet resultSet;

			prepStmtGetOrder.setInt(1, orderId);
			resultSet = prepStmtGetOrder.executeQuery();
			if (!resultSet.next())
			{
				throw new RepositoryException(
						"No matches for order with id: " + orderId + " found in database!");
			}
			else
			{
				customerName = resultSet.getString("customer_name");
				dateCreated = resultSet.getDate("created");
			}

			prepStmtGetProductsForOrder.setInt(1, orderId);
			resultSet = prepStmtGetProductsForOrder.executeQuery();

			boolean resultSetIsEmpty = true;

			while (resultSet.next())
			{
				resultSetIsEmpty = false;
				productIds.add(resultSet.getInt(1));
			}

			if (resultSetIsEmpty)
			{
				throw new RepositoryException("No products in order with id: " + orderId);
			}

			return new Order(orderId, customerName, productIds, dateCreated);
		}
		catch (SQLException e)
		{
			throw new RepositoryException(
					"Failed to retrieve order data for product with id: " + orderId
							+ " from database!", e);
		}
	}

	@Override
	public void removeOrder(final int orderId) throws RepositoryException
	{
		final String removeOrderQuery = "DELETE FROM " + orderTableName + " WHERE id_order = ?;";

		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement prepStmtRemoveOrder = con.prepareStatement(removeOrderQuery))
		{
			prepStmtRemoveOrder.setInt(1, orderId);
			prepStmtRemoveOrder.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Failed to remove order with id: " + orderId, e);
		}
	}

	@Override
	public List<Order> getOrders(final String customerUsername) throws RepositoryException
	{
		final String getOrderIdsQuery = "SELECT id_order FROM " + orderTableName
				+ "WHERE customer_name = '" + customerUsername + "';";

		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement prepStmtGetOrderIdsQuery = con.prepareStatement(getOrderIdsQuery))
		{
			ResultSet resultSet;
			ArrayList<Order> orderList = new ArrayList<>();
			resultSet = prepStmtGetOrderIdsQuery.executeQuery();

			boolean resultSetIsEmpty = true;
			while (resultSet.next())
			{
				resultSetIsEmpty = false;
				Order retrievedOrder = getOrder(resultSet.getInt(1));
				orderList.add(retrievedOrder);
			}
			if (resultSetIsEmpty)
			{
				throw new RepositoryException(
						"No orders found for customer with username: " + customerUsername);
			}
			return orderList;
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Failed to retrieve orders for customer with username: "
					+ customerUsername, e);
		}
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
