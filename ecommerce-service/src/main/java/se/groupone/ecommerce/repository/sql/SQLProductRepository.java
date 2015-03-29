package se.groupone.ecommerce.repository.sql;

import se.groupone.ecommerce.exception.RepositoryException;
import se.groupone.ecommerce.model.Product;
import se.groupone.ecommerce.model.ProductParameters;
import se.groupone.ecommerce.repository.ProductRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLProductRepository implements ProductRepository
{
	private final String dbTable = "product";

	@Override
	public void addProduct(final Product product) throws RepositoryException
	{
		final String addProductQuery =
				"INSERT INTO " + dbTable + " "
						+ "(id_product, title, category, manufacturer, description, img, price, "
						+ "quantity) " + "VALUES(?, ?, ?, ?, ?, ?, ?, ?);";

		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement ps = con.prepareStatement(addProductQuery))
		{
			ps.setInt(1, product.getId());
			ps.setString(2, product.getTitle());
			ps.setString(3, product.getCategory());
			ps.setString(4, product.getManufacturer());
			ps.setString(5, product.getDescription());
			ps.setString(6, product.getImg());
			ps.setDouble(7, product.getPrice());
			ps.setInt(8, product.getQuantity());

			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not add Product to database!", e);
		}
	}

	@Override
	public Product getProduct(final int productID) throws RepositoryException
	{
		String getProductQuery = "SELECT * FROM " + dbTable + " WHERE id_product = ?;";
		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement ps = con.prepareStatement(getProductQuery))
		{
			ps.setInt(1, productID);
			ResultSet resultSet = ps.executeQuery();

			if (!resultSet.next())
			{
				throw new RepositoryException(
						"No matches for Product with id: " + productID + " found in "
								+ "database!");
			}
			else
			{
				final ProductParameters productParams = new ProductParameters(
						resultSet.getString("title"),
						resultSet.getString("category"),
						resultSet.getString("manufacturer"),
						resultSet.getString("description"),
						resultSet.getString("img"),
						resultSet.getDouble("price"),
						resultSet.getInt("quantity"));

				return new Product(productID, productParams);
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException(
					"Failed to retrieve Product data for product with id: " + productID
							+ " from database!", e);
		}
	}

	@Override
	public List<Product> getProducts() throws RepositoryException
	{
		String getProductQuery = "SELECT * FROM " + dbTable + ";";
		List<Product> productList = new ArrayList<>();

		try (Connection con = SQLConnector.getConnection();
			 Statement statement = con.createStatement())
		{
			ResultSet resultSet = statement.executeQuery(getProductQuery);

			boolean resultSetIsEmpty = true;

			while (resultSet.next())
			{
				resultSetIsEmpty = false;

				final int productID = resultSet.getInt("id_product");
				final ProductParameters productParams = new ProductParameters(
						resultSet.getString("title"),
						resultSet.getString("category"),
						resultSet.getString("manufacturer"),
						resultSet.getString("description"),
						resultSet.getString("img"),
						resultSet.getDouble("price"),
						resultSet.getInt("quantity"));

				productList.add(new Product(productID, productParams));
			}

			if (resultSetIsEmpty)
			{
				throw new RepositoryException("No products in database!");
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not fetch all Products from database!", e);
		}

		return productList;
	}

	@Override
	public void removeProduct(final int productID) throws RepositoryException
	{
		final String removeQuery = "DELETE FROM " + dbTable + " WHERE id_product = ?;";

		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement ps = con.prepareStatement(removeQuery))
		{
			ps.setInt(1, productID);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not add Product to database!", e);
		}
	}

	@Override
	public void updateProduct(Product product) throws RepositoryException
	{
		final String addProductQuery =
				"UPDATE " + dbTable
						+ " SET title= ?, category = ?, manufacturer = ?, description = ?,"
						+ " img = ?, price = ?, quantity = ? WHERE id_product = ?;";

		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement ps = con.prepareStatement(addProductQuery))
		{
			ps.setString(1, product.getTitle());
			ps.setString(2, product.getCategory());
			ps.setString(3, product.getManufacturer());
			ps.setString(4, product.getDescription());
			ps.setString(5, product.getImg());
			ps.setDouble(6, product.getPrice());
			ps.setInt(7, product.getQuantity());
			ps.setInt(8, product.getId());

			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not add Product to database!", e);
		}
	}

	@Override
	public int getHighestId() throws RepositoryException
	{
		final String highestIDQuery =
				"SELECT MAX(id_product) FROM " + dbTable;

		try (Connection con = SQLConnector.getConnection();
			 Statement statement = con.createStatement())
		{
			ResultSet resultSet = statement.executeQuery(highestIDQuery);

			if (!resultSet.next())
			{
				throw new RepositoryException("No products in database!");
			}
			else
			{
				int highestId = resultSet.getInt(1);
				return highestId;
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not get query highest Product ID!", e);
		}
	}

	private void productsQuantityChange(final List<Integer> ids, final int quantityChange)
			throws RepositoryException
	{
		final String updateQuantityQuery =
				"UPDATE " + dbTable + " SET quantity = quantity + ? WHERE id_product = ?;";

		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement ps = con.prepareStatement(updateQuantityQuery))
		{
			for (int id : ids)
			{
				ps.setInt(1, quantityChange);
				ps.setInt(2, id);
				ps.executeUpdate();
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not update Product quantity!", e);
		}
	}

	@Override
	public void decreaseQuantityOfProductsByOne(List<Integer> ids) throws RepositoryException
	{
		productsQuantityChange(ids, -1);
	}

	@Override
	public void increaseQuantityOfProductsByOne(List<Integer> ids) throws RepositoryException
	{
		productsQuantityChange(ids, 1);
	}
}
