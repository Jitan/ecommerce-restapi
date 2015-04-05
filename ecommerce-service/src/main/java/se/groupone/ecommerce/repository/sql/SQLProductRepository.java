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
	private final String productTableName = "product";

	@Override
	public void addProduct(final Product product) throws RepositoryException
	{

		final String addProductQuery =
				"INSERT INTO " + productTableName + " "
						+ "(id_product, title, category, manufacturer, description, img, price, "
						+ "quantity) " + "VALUES(?, ?, ?, ?, ?, ?, ?, ?);";

		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement prepStmtAddProduct = con.prepareStatement(addProductQuery))
		{
			prepStmtAddProduct.setInt(1, product.getId());
			prepStmtAddProduct.setString(2, product.getTitle());
			prepStmtAddProduct.setString(3, product.getCategory());
			prepStmtAddProduct.setString(4, product.getManufacturer());
			prepStmtAddProduct.setString(5, product.getDescription());
			prepStmtAddProduct.setString(6, product.getImg());
			prepStmtAddProduct.setDouble(7, product.getPrice());
			prepStmtAddProduct.setInt(8, product.getQuantity());

			prepStmtAddProduct.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not add product to database!", e);
		}
	}

	@Override
	public Product getProduct(final int productId) throws RepositoryException
	{
		String getProductQuery = "SELECT * FROM " + productTableName + " WHERE id_product = ?;";
		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement prepStmtGetProduct = con.prepareStatement(getProductQuery))
		{
			prepStmtGetProduct.setInt(1, productId);
			ResultSet resultSet = prepStmtGetProduct.executeQuery();

			if (!resultSet.next())
			{
				throw new RepositoryException(
						"No matches for product with id: " + productId + " found in "
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

				return new Product(productId, productParams);
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException(
					"Failed to retrieve product data for product with id: " + productId
							+ " from database!", e);
		}
	}

	@Override
	public List<Product> getProducts() throws RepositoryException
	{
		String getAllProductsQuery = "SELECT * FROM " + productTableName + ";";
		List<Product> productList = new ArrayList<>();

		try (Connection con = SQLConnector.getConnection();
			 Statement stmtGetProducts = con.createStatement())
		{
			ResultSet resultSet = stmtGetProducts.executeQuery(getAllProductsQuery);

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
			throw new RepositoryException("Could not fetch all products from database!", e);
		}

		return productList;
	}

	@Override
	public void removeProduct(final int productID) throws RepositoryException
	{
		final String removeProductQuery = "DELETE FROM " + productTableName + " WHERE id_product = ?;";

		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement prepStmtRemoveProduct = con.prepareStatement(removeProductQuery))
		{
			prepStmtRemoveProduct.setInt(1, productID);
			prepStmtRemoveProduct.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not add product to database!", e);
		}
	}

	@Override
	public void updateProduct(Product product) throws RepositoryException
	{
		final String addProductQuery =
				"UPDATE " + productTableName
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
			throw new RepositoryException("Could not add product to database!", e);
		}
	}

	@Override
	public int getHighestId() throws RepositoryException
	{
		final String highestIdQuery =
				"SELECT MAX(id_product) FROM " + productTableName;

		try (Connection con = SQLConnector.getConnection();
			 Statement stmtGetHighestId = con.createStatement())
		{
			ResultSet resultSet = stmtGetHighestId.executeQuery(highestIdQuery);

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
			throw new RepositoryException("Could not get highest product id!", e);
		}
	}

	// TODO This is now unused and can be removed?
	private void productsQuantityChange(final List<Integer> ids, final int quantityChange)
			throws RepositoryException
	{
		final String updateQuantityQuery =
				"UPDATE " + productTableName + " SET quantity = quantity + ? WHERE id_product = ?;";

		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement prepStmtUpdateQuantity = con.prepareStatement(updateQuantityQuery))
		{
			for (int id : ids)
			{
				prepStmtUpdateQuantity.setInt(1, quantityChange);
				prepStmtUpdateQuantity.setInt(2, id);
				prepStmtUpdateQuantity.executeUpdate();
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not update product quantity!", e);
		}
	}
}
