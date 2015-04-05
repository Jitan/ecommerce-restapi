package se.groupone.ecommerce.service;

import se.groupone.ecommerce.exception.RepositoryException;
import se.groupone.ecommerce.exception.ShopServiceException;
import se.groupone.ecommerce.model.Customer;
import se.groupone.ecommerce.model.Order;
import se.groupone.ecommerce.model.Product;
import se.groupone.ecommerce.model.ProductParameters;
import se.groupone.ecommerce.repository.CustomerRepository;
import se.groupone.ecommerce.repository.OrderRepository;
import se.groupone.ecommerce.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

public final class ShopService
{
	private final CustomerRepository customerRepository;
	private final ProductRepository productRepository;
	private final OrderRepository orderRepository;

	public ShopService(CustomerRepository customerRepository,
			ProductRepository productRepository,
			OrderRepository orderRepository)
	{
		this.customerRepository = customerRepository;
		this.productRepository = productRepository;
		this.orderRepository = orderRepository;
	}

	public Product addProduct(ProductParameters productParams)
	{
		Product newProduct;
		try
		{
			int newProductId = productRepository.getHighestId() + 1;
			newProduct = new Product(newProductId, productParams);
			productRepository.addProduct(newProduct);
		}
		catch (RepositoryException e)
		{
			throw new ShopServiceException("Could not add product: "
					+ e.getMessage(), e);
		}

		return newProduct;
	}

	// Defaults to quantity: 1 if no amount is provided.
	public void addProductToCustomer(int productId, String customerUsername)
	{
		addProductToCustomer(productId, customerUsername, 1);
	}

	public void addProductToCustomer(int productId, String customerUsername, int amount)
	{
		try
		{
			if (productRepository.getProduct(productId).getQuantity() >= amount)
			{
				Customer customer = customerRepository.getCustomer(customerUsername);

				for (int i = 0; i < amount; i++)
				{
					customer.addProductToShoppingCart(productId);
				}
				// Make the repository record the changes to customer
				customerRepository.updateCustomer(customer);
			}
		}
		catch (RepositoryException e)
		{
			throw new ShopServiceException("Could not add product to customer: " + e.getMessage(),
					e);
		}
	}

	public Product getProductWithId(int productId)
	{
		try
		{
			return productRepository.getProduct(productId);
		}
		catch (RepositoryException e)
		{
			throw new ShopServiceException("Could not getProduct: " + e.getMessage(), e);
		}
	}

	public List<Product> getProducts()
	{
		try
		{
			return productRepository.getProducts();
		}
		catch (RepositoryException e)
		{
			throw new ShopServiceException("Could not get products.: " + e.getMessage(), e);
		}
	}

	public void removeProduct(int productId)
	{
		try
		{
			productRepository.removeProduct(productId);
		}
		catch (RepositoryException e)
		{
			throw new ShopServiceException("Could not remove product: " + e.getMessage(), e);
		}
	}

	public void updateProduct(int productId, ProductParameters productParams)
	{
		try
		{
			productRepository.updateProduct(new Product(productId, productParams));
		}
		catch (RepositoryException e)
		{
			throw new ShopServiceException("Could not updateProduct: " + e.getMessage(), e);
		}
	}

	public void addCustomer(Customer customer)
	{
		try
		{
			customerRepository.addCustomer(customer);
		}
		catch (RepositoryException e)
		{
			throw new ShopServiceException("Could not add customer: " + e.getMessage(), e);
		}
	}

	public Customer getCustomer(String customerUsername)
	{
		try
		{
			return customerRepository.getCustomer(customerUsername);
		}
		catch (RepositoryException e)
		{
			throw new ShopServiceException("Could not get customer: " + e.getMessage(), e);
		}
	}

	public void updateCustomer(Customer customer)
	{
		try
		{
			customerRepository.updateCustomer(customer);
		}
		catch (RepositoryException e)
		{
			throw new ShopServiceException("Could not update customer: " + e.getMessage(), e);
		}
	}

	public void removeCustomer(String customerUsername)
	{
		try
		{
			customerRepository.removeCustomer(customerUsername);
		}
		catch (RepositoryException e)
		{
			throw new ShopServiceException("Could not remove customer: " + e.getMessage(), e);
		}
	}

	public Order createOrder(String customerUsername)
	{
		Order newOrder;
		try
		{
			Customer customer = customerRepository.getCustomer(customerUsername);
			ArrayList<Integer> orderedProductIds = customer.getShoppingCart();
			if (orderedProductIds.isEmpty())
			{
				throw new ShopServiceException("This user has no items in their cart");
			}

			int newOrderId = orderRepository.getHighestId() + 1;

			newOrder = new Order(newOrderId, customerUsername, orderedProductIds);
			orderRepository.addOrder(newOrder);

			customer.getShoppingCart().clear();
			customerRepository.updateCustomer(customer);
		}
		catch (RepositoryException e)
		{
			throw new ShopServiceException("Could not create order: " + e.getMessage(), e);
		}
		return newOrder;
	}

	public Order getOrder(int orderId)
	{
		try
		{
			return orderRepository.getOrder(orderId);
		}
		catch (RepositoryException e)
		{
			throw new ShopServiceException("Could not get order: " + e.getMessage(), e);
		}
	}

	public List<Order> getOrders(String customerUsername)
	{
		try
		{
			customerRepository.getCustomer(customerUsername); // Should throw
			// exception if
			// user
			// does not exist
			return orderRepository.getOrders(customerUsername);
		}
		catch (RepositoryException e)
		{
			throw new ShopServiceException("Could not get orders: " + e.getMessage(), e);
		}
	}

	public void updateOrder(Order order)
	{
		try
		{
			orderRepository.updateOrder(order);
		}
		catch (RepositoryException e)
		{
			throw new ShopServiceException("Could not update order: " + e.getMessage(), e);
		}
	}

	public void removeOrder(int orderId)
	{
		try
		{
			orderRepository.removeOrder(orderId);
		}
		catch (RepositoryException e)
		{
			throw new ShopServiceException("Could not remove order: " + e.getMessage(), e);
		}
	}
}