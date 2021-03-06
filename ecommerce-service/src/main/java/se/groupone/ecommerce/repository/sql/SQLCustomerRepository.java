package se.groupone.ecommerce.repository.sql;

import se.groupone.ecommerce.exception.RepositoryException;
import se.groupone.ecommerce.model.Customer;
import se.groupone.ecommerce.model.ShoppingCart;
import se.groupone.ecommerce.repository.CustomerRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class SQLCustomerRepository implements CustomerRepository
{
	private final String customerTableName = "customer";
	private final String customerCartTableName = "customer_cart";

	@Override
	public void addCustomer(final Customer customer) throws RepositoryException
	{
		final String addProductQuery =
				"INSERT INTO " + customerTableName + " "
						+ "(user_name, password, email, first_name, last_name, address, phone) "
						+ "VALUES(?, ?, ?, ?, ?, ?, ?);";

		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement ps = con.prepareStatement(addProductQuery))
		{
			ps.setString(1, customer.getUsername());
			ps.setString(2, customer.getPassword());
			ps.setString(3, customer.getEmail());
			ps.setString(4, customer.getFirstName());
			ps.setString(5, customer.getLastName());
			ps.setString(6, customer.getAddress());
			ps.setString(7, customer.getPhoneNumber());

			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not add customer to database!", e);
		}
	}

	@Override
	public Customer getCustomer(final String username) throws RepositoryException
	{
		Customer customer;
		String getCustomerQuery = "SELECT * FROM " + customerTableName + " WHERE user_name = ?;";
		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement ps = con.prepareStatement(getCustomerQuery))
		{
			ps.setString(1, username);
			ResultSet resultSet = ps.executeQuery();

			if (resultSet.next())
			{
				customer = makeCustomerFromResultSet(resultSet);

				ShoppingCart shoppingCartFromDB = getCustomerCartFromDB(customer.getUsername());
				customer.replaceShoppingCart(shoppingCartFromDB);

				return customer;
			}

			throw new RepositoryException(
					"No matches for customer with username: " + username + " found in database!");
		}
		catch (SQLException e)
		{
			throw new RepositoryException(
					"Failed to retrieve customer with username: " + username
							+ " from database!", e);
		}
	}

	private Customer makeCustomerFromResultSet(ResultSet resultSet)
			throws SQLException, RepositoryException
	{
		Customer customer = new Customer(resultSet.getString("user_name"),
				resultSet.getString("password"),
				resultSet.getString("email"),
				resultSet.getString("first_name"),
				resultSet.getString("last_name"),
				resultSet.getString("address"),
				resultSet.getString("phone"));
		return customer;
	}

	private ShoppingCart getCustomerCartFromDB(String username) throws RepositoryException
	{
		String getCartItemsQuery =
				"SELECT * FROM " + customerCartTableName + " WHERE user_name = ?;";
		ShoppingCart shoppingCart = new ShoppingCart();

		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement ps = con.prepareStatement(getCartItemsQuery))
		{
			ps.setString(1, username);
			ResultSet resultSet = ps.executeQuery();

			while (resultSet.next())
			{
				shoppingCart.addProduct(resultSet.getInt(2));
			}

			return shoppingCart;
		}
		catch (SQLException e)
		{
			throw new RepositoryException(
					"Failed to retrieve customer shoppingCart for customer with username: "
							+ username, e);
		}
	}

	@Override
	public List<Customer> getCustomers() throws RepositoryException
	{
		String getAllCustomersQuery = "SELECT * FROM " + customerTableName + ";";
		List<Customer> customerList = new ArrayList<>();

		try (Connection con = SQLConnector.getConnection();
			 Statement statement = con.createStatement())
		{
			ResultSet resultSet = statement.executeQuery(getAllCustomersQuery);

			boolean resultSetIsEmpty = true;
			while (resultSet.next())
			{
				resultSetIsEmpty = false;
				Customer customer = makeCustomerFromResultSet(resultSet);

				ShoppingCart shoppingCartFromDB = getCustomerCartFromDB(customer.getUsername());
				customer.replaceShoppingCart(shoppingCartFromDB);

				customerList.add(customer);
			}
			if (resultSetIsEmpty)
			{
				throw new RepositoryException("No customers in database!");
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not fetch all customers from database!", e);
		}

		return customerList;
	}

	@Override
	public void updateCustomer(final Customer customer) throws RepositoryException
	{
		final String updateCustomerQuery =
				"UPDATE " + customerTableName
						+ " SET password = ?, email = ?, first_name = ?, "
						+ "last_name = ?, address = ? , phone = ? WHERE user_name = ?;";

		final String deleteOldCartQuery =
				"DELETE FROM " + customerCartTableName + " WHERE user_name = ?;";

		final String insertNewCartItemQuery =
				"INSERT INTO " + customerCartTableName + " (id_product, user_name) VALUES (?, ?)";

		try (Connection con = SQLConnector.getConnection())
		{
			con.setAutoCommit(false);
			try (PreparedStatement prepStmtUpdateCustomer = con
					.prepareStatement(updateCustomerQuery);
				 PreparedStatement prepStmtDeleteOldCart =
						 con.prepareStatement(deleteOldCartQuery);
				 PreparedStatement prepStmtInsertCartItem = con
						 .prepareStatement(insertNewCartItemQuery))
			{
				prepStmtUpdateCustomer.setString(1, customer.getPassword());
				prepStmtUpdateCustomer.setString(2, customer.getEmail());
				prepStmtUpdateCustomer.setString(3, customer.getFirstName());
				prepStmtUpdateCustomer.setString(4, customer.getLastName());
				prepStmtUpdateCustomer.setString(5, customer.getAddress());
				prepStmtUpdateCustomer.setString(6, customer.getPhoneNumber());
				prepStmtUpdateCustomer.setString(7, customer.getUsername());
				prepStmtUpdateCustomer.executeUpdate();

				prepStmtDeleteOldCart.setString(1, customer.getUsername());
				prepStmtDeleteOldCart.executeUpdate();

				final ArrayList<Integer> updatedCartList = customer.getShoppingCart();
				final String customerUsername = customer.getUsername();
				for (int productId : updatedCartList)
				{
					prepStmtInsertCartItem.setInt(1, productId);
					prepStmtInsertCartItem.setString(2, customerUsername);
					prepStmtInsertCartItem.executeUpdate();
				}

				con.commit();
			}
			catch (SQLException e)
			{
				con.rollback();
				throw new RepositoryException(
						"Could not update customer with username: " + customer.getUsername()
								+ " !", e);
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException(
					"Could not get SQL connection when trying to update user!", e);
		}
	}

	@Override
	public void removeCustomer(final String username) throws RepositoryException
	{

		final String removeQuery = "DELETE FROM " + customerTableName + " WHERE user_name = ?;";

		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement prepStmtRemoveCustomer = con.prepareStatement(removeQuery))
		{
			prepStmtRemoveCustomer.setString(1, username);
			prepStmtRemoveCustomer.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not remove customer from database!", e);
		}
	}
}
