package se.groupone.ecommerce.repository;

import se.groupone.ecommerce.exception.RepositoryException;
import se.groupone.ecommerce.model.Product;

import java.util.List;

public interface ProductRepository
{
	public void addProduct(Product product) throws RepositoryException;

	public Product getProduct(int id) throws RepositoryException;

	public List<Product> getProducts() throws RepositoryException;

	public void removeProduct(int id) throws RepositoryException;

	public void updateProduct(Product product) throws RepositoryException;
	
	public void decreaseQuantityOfProductsByOne(List<Integer> ids) throws RepositoryException;
	
	public void increaseQuantityOfProductsByOne(List<Integer> ids) throws RepositoryException;
	
	public int getHighestId() throws RepositoryException;

}
