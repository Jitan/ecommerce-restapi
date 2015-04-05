package se.groupone.ecommerce.test.webservice;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import se.groupone.ecommerce.model.Customer;
import se.groupone.ecommerce.model.Order;
import se.groupone.ecommerce.model.Product;
import se.groupone.ecommerce.model.ProductParameters;
import se.groupone.ecommerce.webservice.util.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static se.groupone.ecommerce.test.webservice.ConnectionConfig.*;

public class CustomerServiceTest
{
	private static final Client client = ClientBuilder.newBuilder()
			.register(CustomerMapper.class)
			.register(IntegerListMapper.class)
			.register(ProductMapper.class)
			.register(ProductParamMapper.class)
			.register(OrderMapper.class)
			.build();

	// Models
	private static final Customer CUSTOMER_ALEX = new Customer("alex", "password", "alex@email.com", "Alexander",
			"Sol", "Banangatan 1", "543211");
	private static final ProductParameters PRODUCT_PARAMETERS_TOMATO = new ProductParameters("Tomato", "Vegetables",
			"Spain", "A beautiful tomato",
			"http://google.com/tomato.jpg", 45, 500);

	// Resource targets
	private static final WebTarget CUSTOMERS_TARGET;
	private static final WebTarget PRODUCTS_TARGET;
	private static final WebTarget ORDERS_TARGET;
	static
	{
		CUSTOMERS_TARGET = client.target(CUSTOMERS_URL);
		PRODUCTS_TARGET = client.target(PRODUCTS_URL);
		ORDERS_TARGET = client.target(ORDERS_URL);
	}

	@Before
	public void init()
	{
		// Truncate repository tables before each test
		WebTarget admin = client.target(URL_BASE + "/admin");
		admin.request().buildPost(Entity.entity("reset-repo", MediaType.TEXT_HTML)).invoke();
	}

	@AfterClass
	public static void tearDown()
	{
		// Truncate repository tables after all tests are done
		WebTarget admin = client.target(ConnectionConfig.URL_BASE + "/admin");
		admin.request().buildPost(Entity.entity("reset-repo", MediaType.TEXT_HTML)).invoke();
	}

	//  Skapa en ny användare
	@Test
	public void canCreateCustomer()
	{
		// POST - Create customer
		Response response = CUSTOMERS_TARGET.request(MediaType.APPLICATION_JSON)
				.buildPost(Entity.entity(CUSTOMER_ALEX, MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, response.getStatus());

		// GET - Retrieve created customer
		Customer createdCustomer = CUSTOMERS_TARGET.path(CUSTOMER_ALEX.getUsername())
				.request(MediaType.APPLICATION_JSON)
				.get(Customer.class);
		assertEquals(createdCustomer, CUSTOMER_ALEX);
	}

	//  Skapa en ny användare – detta ska returnera en länk till den skapade
	// användaren i Location-headern
	@Test
	public void createCustomerReturnsCorrectLocationHeaderForCreatedCustomer() throws URISyntaxException
	{
		final URI EXPECTED_URI = new URI("http://localhost:8080/ecommerce-webservice/customers/"
				+ CUSTOMER_ALEX.getUsername());
		
		// POST - Create new customer
		Response response = CUSTOMERS_TARGET.request(MediaType.APPLICATION_JSON)
				.buildPost(Entity.entity(CUSTOMER_ALEX, MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, response.getStatus());

		// Check returned location URI
		assertEquals(EXPECTED_URI, response.getLocation());
	}

	//  Uppdatera en användare
	@Test
	public void canUpdateCustomer()
	{
		// POST - create Customer2 in repo
		Response postResponse = CUSTOMERS_TARGET.request(MediaType.APPLICATION_JSON)
				.buildPost(Entity.entity(CUSTOMER_ALEX, MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, postResponse.getStatus());

		// Updated customer2 with changed password.
		Customer updatedCustomer2 = new Customer(CUSTOMER_ALEX.getUsername(), "secret", CUSTOMER_ALEX.getEmail(),
				CUSTOMER_ALEX.getFirstName(), CUSTOMER_ALEX.getLastName(),
				CUSTOMER_ALEX.getAddress(), CUSTOMER_ALEX.getPhoneNumber());
		
		// POST - Update customer
		Response putResponse = CUSTOMERS_TARGET.path(CUSTOMER_ALEX.getUsername())
				.request(MediaType.APPLICATION_JSON)
				.buildPut(Entity.entity(updatedCustomer2, MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(204, putResponse.getStatus());

		// GET - Check that customer is updated
		Customer updatedCustomer2FromRepo = CUSTOMERS_TARGET.path(CUSTOMER_ALEX.getUsername())
				.request(MediaType.APPLICATION_JSON)
				.get(Customer.class);
		assertEquals(updatedCustomer2, updatedCustomer2FromRepo);
	}

	//  Ta bort en användare (eller sätta den som inaktiv)
	@Test
	public void canRemoveCustomer()
	{
		// POST - Create customer
		Response postResponse = CUSTOMERS_TARGET.request(MediaType.APPLICATION_JSON)
				.buildPost(Entity.entity(CUSTOMER_ALEX, MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, postResponse.getStatus());

		// GET - Check that it is in repository
		Response thisShouldSucceedResponse = CUSTOMERS_TARGET.path(CUSTOMER_ALEX.getUsername())
				.request(MediaType.APPLICATION_JSON)
				.get();
		assertEquals(200, thisShouldSucceedResponse.getStatus());

		// DELETE - Delete it
		Response deleteResponse = CUSTOMERS_TARGET.path(CUSTOMER_ALEX.getUsername())
				.request(MediaType.APPLICATION_JSON)
				.buildDelete()
				.invoke();
		assertEquals(204, deleteResponse.getStatus());

		// GET - Try to retrieve deleted customer, should fail
		Response thisShouldFailResponse = CUSTOMERS_TARGET.path(CUSTOMER_ALEX.getUsername())
				.request(MediaType.APPLICATION_JSON)
				.get();
		assertEquals(400, thisShouldFailResponse.getStatus());
	}

	@Test
	public void canAddProductToCart()
	{
		// POST - Create customer
		Response createCustomerAlexResponse = CUSTOMERS_TARGET.request(MediaType.APPLICATION_JSON)
				.buildPost(Entity.entity(CUSTOMER_ALEX, MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, createCustomerAlexResponse.getStatus());

		// POST - Create products
		Response createProductTomatoResponse = PRODUCTS_TARGET.request(MediaType.APPLICATION_JSON)
				.buildPost(Entity.entity(PRODUCT_PARAMETERS_TOMATO, MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, createProductTomatoResponse.getStatus());

		// GET - Get created products
		final Product PRODUCT_TOMATO = client.target(createProductTomatoResponse.getLocation())
				.request(MediaType.APPLICATION_JSON)
				.get(Product.class);

		// POST - Add products to cart
		final Response addProductToCartResponse = CUSTOMERS_TARGET
				.path(CUSTOMER_ALEX.getUsername())
				.path("cart")
				.request()
				.buildPost(Entity.entity(Integer.toString(PRODUCT_TOMATO.getId()), MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, addProductToCartResponse.getStatus());

		// GET - Get cart contents
		final String shoppingCartJson = CUSTOMERS_TARGET
				.path(CUSTOMER_ALEX.getUsername())
				.path("cart")
				.request(MediaType.APPLICATION_JSON)
				.get(String.class);

		// Create gson parser that uses adapter from IntegerListMapper
		Type integerListType = new TypeToken<ArrayList<Integer>>(){}.getType();
		Gson gson = new GsonBuilder().registerTypeAdapter(integerListType, new IntegerListMapper.IntegerListAdapter())
				.create();
		
		// Parse received shoppingCartJson
		JsonObject shoppingCartJsonObject = gson.fromJson(shoppingCartJson, JsonObject.class);
		JsonArray cartJsonArray = shoppingCartJsonObject.get("integerArray").getAsJsonArray();
		ArrayList<Integer> cartArrayList = gson.fromJson(cartJsonArray, integerListType);

		// And verify content
		assertEquals(PRODUCT_TOMATO.getId(), (int) cartArrayList.get(0));
	}

	//  Hämta en användares alla order
	@Test
	public void canGetCustomerOrders()
	{
		// POST - Create customer
		Response createCustomerResponse = CUSTOMERS_TARGET.request(MediaType.APPLICATION_JSON)
				.buildPost(Entity.entity(CUSTOMER_ALEX, MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, createCustomerResponse.getStatus());

		Order orderToBeChecked1 = addOrder(CUSTOMER_ALEX);
		Order orderToBeChecked2 = addOrder(CUSTOMER_ALEX);
		Order orderToBeChecked3 = addOrder(CUSTOMER_ALEX);

		// GET - Retrieve created order
		final String ordersJson = CUSTOMERS_TARGET
				.path(CUSTOMER_ALEX.getUsername())
				.path("orders")
				.request()
				.get(String.class);

		// Verify received ordersJson String
		HashMap<Integer, Order> customerOrders = parseOrderJsonArrayList(ordersJson);

		assertTrue(customerOrders.containsKey(orderToBeChecked1.getId()));
		Order orderFromRepo1 = customerOrders.get(orderToBeChecked1.getId());
		assertEquals(orderToBeChecked1, orderFromRepo1);

		assertTrue(customerOrders.containsKey(orderToBeChecked2.getId()));
		Order orderFromRepo2 = customerOrders.get(orderToBeChecked2.getId());
		assertEquals(orderToBeChecked2, orderFromRepo2);

		assertTrue(customerOrders.containsKey(orderToBeChecked3.getId()));
		Order orderFromRepo3 = customerOrders.get(orderToBeChecked3.getId());
		assertEquals(orderToBeChecked3, orderFromRepo3);
	}

	private HashMap<Integer, Order> parseOrderJsonArrayList(String ordersJson)
	{
		// Create gson parser that uses adapter from OrderMapper
		Gson gson = new GsonBuilder().registerTypeAdapter(Order.class, new OrderMapper.OrderAdapter()).create();
		HashMap<Integer, Order> customerOrders = new HashMap<>();
		
		// Parse received ordersJson String and put it in HashMap
		JsonObject orderJsonObject = gson.fromJson(ordersJson, JsonObject.class);
		JsonArray orderJsonArray = orderJsonObject.get("orderArray").getAsJsonArray();

		for (JsonElement order : orderJsonArray)
		{
			Order newOrder2 = gson.fromJson(order, Order.class);
			customerOrders.put(newOrder2.getId(), newOrder2);
		}
		return customerOrders;
	}

	// Handles POSTs and GET needed to addOrder and returns finished Order object
	private Order addOrder(Customer customer)
	{
		// POST - Create products
		Response createProductResponse1 = PRODUCTS_TARGET.request(MediaType.APPLICATION_JSON)
				.buildPost(Entity.entity(PRODUCT_PARAMETERS_TOMATO, MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, createProductResponse1.getStatus());

		final Product PRODUCT_TOMATO = client.target(createProductResponse1.getLocation())
				.request(MediaType.APPLICATION_JSON)
				.get(Product.class);

		// POST - Add products to cart
		final Response addProductsToCartResponse = CUSTOMERS_TARGET
				.path(customer.getUsername())
				.path("cart")
				.request()
				.buildPost(Entity.entity(Integer.toString(PRODUCT_TOMATO.getId()), MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, addProductsToCartResponse.getStatus());

		// POST - Create order
		final Response createOrderResponse = ORDERS_TARGET
				.request()
				.buildPost(Entity.entity(CUSTOMER_ALEX.getUsername(), MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, createOrderResponse.getStatus());

		// GET - Retrieve created order and check contents
		final Order createdOrder = client.target(createOrderResponse.getLocation())
				.request()
				.get(Order.class);
		assertEquals(PRODUCT_TOMATO.getId(), (int) createdOrder.getProductIds().get(0));

		return createdOrder;
	}
}