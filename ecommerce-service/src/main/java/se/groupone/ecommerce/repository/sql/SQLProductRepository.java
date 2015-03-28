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
	private final SQLConnector sql;

	public SQLProductRepository() throws RepositoryException
	{
		try
		{
			sql = new SQLConnector(DBConfig.HOST, DBConfig.PORT, DBConfig.USERNAME,
					DBConfig.PASSWORD, DBConfig.DATABASE);
		}
		catch (SQLException e)
		{
			throw new RepositoryException(
					"Could not construct SQLProduct: Could not construct database object", e);
		}
	}

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
		ResultSet resultSet;
		try (Connection con = SQLConnector.getConnection();
			 PreparedStatement ps = con.prepareStatement(getProductQuery))
		{
			ps.setInt(1, productID);
			resultSet = ps.executeQuery();

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
		ResultSet resultSet;
		List<Product> productList = new ArrayList<>();

		try (Connection con = SQLConnector.getConnection();
			 Statement statement = con.createStatement())
		{
			resultSet = statement.executeQuery(getProductQuery);

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
		try
		{
			final String removeQuery = "DELETE FROM " + DBConfig.DATABASE + "." + dbTable
					+ " WHERE id_product = " + productID + ";";
			sql.query(removeQuery);
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not query removal of Product!", e);
		}
	}

	@Override
	public void updateProduct(Product product) throws RepositoryException
	{
		try
		{
			StringBuilder updateProductQuery = new StringBuilder();
			updateProductQuery
					.append("UPDATE " + DBConfig.DATABASE + "." + dbTable + " SET ");
			updateProductQuery.append("title = '" + product.getTitle() + "', ");
			updateProductQuery.append("category = '" + product.getCategory() + "', ");
			updateProductQuery.append("manufacturer = '" + product.getManufacturer() + "', ");
			updateProductQuery.append("description = '" + product.getDescription() + "', ");
			updateProductQuery.append("img = '" + product.getImg() + "', ");
			updateProductQuery.append("price = " + product.getPrice() + ", ");
			updateProductQuery.append("quantity = " + product.getQuantity() + " ");
			updateProductQuery.append("WHERE id_product = " + product.getId() + ";");

			sql.query(updateProductQuery.toString());
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not query Product update!", e);
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

	@Override
	public int getHighestId() throws RepositoryException
	{
		ResultSet rs;
		try
		{
			final String highestIDQuery =
					"SELECT MAX(id_product) FROM " + DBConfig.DATABASE + "." + dbTable;
			rs = sql.queryResult(highestIDQuery);
			if (!rs.isBeforeFirst())
			{
				throw new RepositoryException(
						"No matches MAX(id_product) in Products!\nSQL QUERY: " + highestIDQuery);
			}
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not get query MAX Product ID!", e);
		}

		try
		{
			rs.next();
			final int highestProductID = rs.getInt(1);
			rs.close();
			return highestProductID;
		}
		catch (SQLException e)
		{
			throw new RepositoryException("Could not parse MAX(id_product) Product database!", e);
		}
	}

	private void productsQuantityChange(final List<Integer> ids, final int quantityChange)
			throws RepositoryException
	{

		Product product[] = new Product[ids.size()];
		for (int i = 0; i < ids.size(); i++)
		{
			product[i] = getProduct(ids.get(i));
		}

		StringBuilder updateQuantityQuery = new StringBuilder();
		for (int i = 0; i < ids.size(); i++)
		{
			updateQuantityQuery
					.append("UPDATE " + DBConfig.DATABASE + "." + dbTable + " SET ");
			updateQuantityQuery
					.append("quantity = " + (product[i].getQuantity() + quantityChange) + " ");
			updateQuantityQuery.append("WHERE id_product = " + product[i].getId() + ";");
			try
			{
				sql.query(updateQuantityQuery.toString());

				updateQuantityQuery.delete(0, updateQuantityQuery.length());
			}
			catch (SQLException e)
			{
				throw new RepositoryException("Could not query Product quantity update!", e);
			}
		}
	}
}
