package se.groupone.ecommerce.repository.sql;

import se.groupone.ecommerce.exception.RepositoryException;
import se.groupone.ecommerce.model.Order;
import se.groupone.ecommerce.repository.OrderRepository;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SQLOrderRepository implements OrderRepository
{
	private final String orderTableName = "`order`";
	private final String productTableName = "product";
	private final String productOrderTableName = "product_order";
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

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
				+ "WHERE customer_name = ?;";

		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement prepStmtGetOrderIdsQuery = con.prepareStatement(getOrderIdsQuery))
		{
			ResultSet resultSet;
			ArrayList<Order> orderList = new ArrayList<>();

			prepStmtGetOrderIdsQuery.setString(1, customerUsername);
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
	public void updateOrder(final Order order) throws RepositoryException
	{
		final String dateCreatedString = sdf.format(order.getDateCreated());

		final String updateOrderQuery = "UPDATE " + orderTableName + " SET "
				+ "created = ? WHERE id_order = ?;";
		final String deleteOrderItemsQuery =
				"DELETE FROM " + productOrderTableName + " WHERE id_order = ?;";
		final String addOrderItemQuery =
				"INSERT INTO " + productOrderTableName + "(id_order, id_product) "
				+ "VALUES(?, ?);";

		try (Connection con = SQLConnector.getConnection())
		{
			con.setAutoCommit(false);

			try (PreparedStatement prepStmtUpdateOrder = con.prepareStatement(updateOrderQuery);
				 PreparedStatement prepStmtDeleteOrderItems =
						 con.prepareStatement(deleteOrderItemsQuery);
				 PreparedStatement prepStmtAddOrderItem = con.prepareStatement(addOrderItemQuery))
			{

				int orderId = order.getId();
				ArrayList<Integer> orderProductList = order.getProductIds();

				prepStmtUpdateOrder.setString(1, dateCreatedString);
				prepStmtUpdateOrder.setInt(2, orderId);
				prepStmtUpdateOrder.executeUpdate();

				prepStmtDeleteOrderItems.setInt(1, orderId);
				prepStmtDeleteOrderItems.executeUpdate();

				for (int productId : orderProductList)
				{
					prepStmtAddOrderItem.setInt(1, orderId);
					prepStmtAddOrderItem.setInt(2, productId);
					prepStmtAddOrderItem.executeUpdate();
				}

				con.commit();
			}
			catch (SQLException e)
			{
				con.rollback();
				throw new RepositoryException("Could not update order with id: " + order.getId(),
						e);
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException(
					"Could not get SQL connection when trying to update order!", e);
		}
	}

	@Override
	public int getHighestId() throws RepositoryException
	{
		final String highestIdQuery =
				"SELECT MAX(id_order) FROM " + orderTableName;

		try (Connection con = SQLConnector.getConnection();
			 Statement stmtGetHighestId = con.createStatement())
		{
			ResultSet resultSet = stmtGetHighestId.executeQuery(highestIdQuery);

			if (!resultSet.next())
			{
				throw new RepositoryException("No orders in database!");
			}
			else
			{
				int highestId = resultSet.getInt(1);
				return highestId;
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not get highest order id!", e);
		}
	}
}
