package se.groupone.ecommerce.test.webservice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import se.groupone.ecommerce.model.Customer;
import se.groupone.ecommerce.model.Product;
import se.groupone.ecommerce.model.ProductParameters;
import se.groupone.ecommerce.webservice.util.CustomerMapper;
import se.groupone.ecommerce.webservice.util.ProductMapper;
import se.groupone.ecommerce.webservice.util.ProductParamMapper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static se.groupone.ecommerce.test.webservice.ConnectionConfig.CUSTOMERS_URL;
import static se.groupone.ecommerce.test.webservice.ConnectionConfig.PRODUCTS_URL;

public final class ProductServiceTest
{
	private static final Client client = ClientBuilder.newBuilder()
			.register(ProductMapper.class)
			.register(ProductParamMapper.class)
			.register(CustomerMapper.class)
			.build();

	// Models
	private static final ProductParameters PRODUCT_PARAMETERS_TOMATO = new ProductParameters("Tomato", "Vegetables",
			"Spain", "A beautiful tomato",
			"http://google.com/tomato.jpg", 45, 500);
	private static final ProductParameters PRODUCT_PARAMETERS_LETTUCE = new ProductParameters("Lettuce", "Vegetables",
			"France", "A mound of lettuce",
			"http://altavista.com/lettuce.jpg", 88, 200);
	private static final Customer CUSTOMER_ALEX = new Customer("alex", "password", "alex@email.com", "Alexander",
			"Sol", "Banangatan 1", "543211");

	// Resource targets
	private static final WebTarget PRODUCTS_TARGET;
	private static final WebTarget CUSTOMERS_TARGET;
	static
	{
		PRODUCTS_TARGET = client.target(PRODUCTS_URL);
		CUSTOMERS_TARGET = client.target(CUSTOMERS_URL);
	}

	@Before
	public void init()
	{
		// Truncate repository tables before tests
		WebTarget admin = client.target(ConnectionConfig.URL_BASE + "/admin");
		admin.request().buildPost(Entity.entity("reset-repo", MediaType.TEXT_HTML)).invoke();
	}

	@AfterClass
	public static void tearDown()
	{
		// Truncate repository tables after all tests are done
		WebTarget admin = client.target(ConnectionConfig.URL_BASE + "/admin");
		admin.request().buildPost(Entity.entity("reset-repo", MediaType.TEXT_HTML)).invoke();
	}

	//  Hämta en produkt med ett visst id
	//  Skapa en ny produkt – detta ska returnera en länk till den skapade
	// produkten i Location-headern
	@Test
	public void canCreateAndGetProduct()
	{
		// POST - Create products
		Response createProductResponse = PRODUCTS_TARGET.request(MediaType.APPLICATION_JSON)
				.buildPost(Entity.entity(PRODUCT_PARAMETERS_TOMATO, MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, createProductResponse.getStatus());

		final Product PRODUCT_TOMATO = client.target(createProductResponse.getLocation())
				.request(MediaType.APPLICATION_JSON)
				.get(Product.class);

		// GET
		WebTarget createdTarget = client.target(createProductResponse.getLocation());
		Product createdProduct = createdTarget.request(MediaType.APPLICATION_JSON)
				.get(Product.class);
		assertThat(createdProduct, is(PRODUCT_TOMATO));
	}

	//  Hämta alla produkter
	@Test
	public void canGetAllProducts() throws IOException
	{
		// POST
		Response tomatoResponse = PRODUCTS_TARGET.request(MediaType.APPLICATION_JSON)
				.buildPost(Entity.entity(PRODUCT_PARAMETERS_TOMATO, MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, tomatoResponse.getStatus());

		final Product tomatoProduct = client.target(tomatoResponse.getLocation())
				.request(MediaType.APPLICATION_JSON)
				.get(Product.class);

		// POST
		Response lettuceResponse = PRODUCTS_TARGET.request(MediaType.APPLICATION_JSON)
				.buildPost(Entity.entity(PRODUCT_PARAMETERS_LETTUCE, MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, lettuceResponse.getStatus());

		final Product lettuceProduct = client.target(lettuceResponse.getLocation())
				.request(MediaType.APPLICATION_JSON)
				.get(Product.class);

		String productJson = PRODUCTS_TARGET.request(MediaType.APPLICATION_JSON).get(String.class);
		HashMap<Integer, Product> productMap = parseProductJson(productJson);
		
		assertEquals(tomatoProduct, productMap.get(tomatoProduct.getId()));
		assertEquals(lettuceProduct, productMap.get(lettuceProduct.getId()));
	}

	private HashMap<Integer, Product> parseProductJson(String productJson)
	{
		// Create gson parser that uses adapter from ProductMapper
		Gson gson = new GsonBuilder().registerTypeAdapter(Product.class, new ProductMapper.ProductAdapter()).create();
		HashMap<Integer, Product> productMap = new HashMap<>();

		// Parse received ordersJson String and put it in HashMap
		JsonObject productsJsonObject = gson.fromJson(productJson, JsonObject.class);
		Set<Map.Entry<String, JsonElement>> productSet = productsJsonObject.entrySet();

		for (Map.Entry<String, JsonElement> jsonEntry : productSet)
		{
			Product newProduct = gson.fromJson(jsonEntry.getValue(), Product.class);
			productMap.put(newProduct.getId(), newProduct);
		}
		return productMap;
	}

	//  Uppdatera en produkt
	@Test
	public void canUpdateAProduct()
	{
		// POST - Create products
		Response createProductResponse = PRODUCTS_TARGET.request(MediaType.APPLICATION_JSON)
				.buildPost(Entity.entity(PRODUCT_PARAMETERS_TOMATO, MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, createProductResponse.getStatus());

		final Product PRODUCT_TOMATO = client.target(createProductResponse.getLocation())
				.request(MediaType.APPLICATION_JSON)
				.get(Product.class);

		// GET
		WebTarget createdTarget = client.target(createProductResponse.getLocation());
		Product createdProduct = createdTarget
				.request(MediaType.APPLICATION_JSON).get(Product.class);
		assertThat(createdProduct, is(PRODUCT_TOMATO));

		// PUT
		createdTarget.request(MediaType.APPLICATION_JSON)
				.buildPut(Entity.entity(PRODUCT_PARAMETERS_LETTUCE, MediaType.APPLICATION_JSON))
				.invoke();

		// GET
		Product updatedProduct = createdTarget
				.request(MediaType.APPLICATION_JSON).get(Product.class);
		assertThat(updatedProduct.getTitle(), is(PRODUCT_PARAMETERS_LETTUCE.getTitle()));
		assertThat(updatedProduct.getQuantity(), is(PRODUCT_PARAMETERS_LETTUCE.getQuantity()));
	}

	//  Ta bort en produkt (eller sätta den som inaktiv)
	@Test
	public void canRemoveProduct() throws IOException
	{
		// POST - Create customer
		Response response = CUSTOMERS_TARGET.request(MediaType.APPLICATION_JSON)
				.buildPost(Entity.entity(CUSTOMER_ALEX, MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, response.getStatus());

		// POST - Create products
		Response createProductResponse = PRODUCTS_TARGET.request(MediaType.APPLICATION_JSON)
				.buildPost(Entity.entity(PRODUCT_PARAMETERS_TOMATO, MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, createProductResponse.getStatus());

		final Product PRODUCT_TOMATO = client.target(createProductResponse.getLocation())
				.request(MediaType.APPLICATION_JSON)
				.get(Product.class);

		// POST - Add products to cart
		final Response addProductsToCartResponse = CUSTOMERS_TARGET
				.path(CUSTOMER_ALEX.getUsername())
				.path("cart")
				.request()
				.buildPost(Entity.entity(Integer.toString(PRODUCT_TOMATO.getId()),
						MediaType.APPLICATION_JSON))
				.invoke();
		assertEquals(201, addProductsToCartResponse.getStatus());

		// GET
		Product createdProduct = client.target(createProductResponse.getLocation())
				.request(MediaType.APPLICATION_JSON).get(Product.class);
		assertThat(createdProduct, is(PRODUCT_TOMATO));

		// DELETE
		Response deleteProductResponse = client.target(createProductResponse.getLocation())
				.request()
				.delete();
		assertThat(deleteProductResponse.getStatus(), is(Status.NO_CONTENT.getStatusCode()));

		// GET
		Response getDeletedProductResponse = client.target(createProductResponse.getLocation())
				.request(MediaType.APPLICATION_JSON).get();
		assertThat(getDeletedProductResponse.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));

		// GET - Check that product has also been removed from customer Cart
		Customer createdCustomer = CUSTOMERS_TARGET.path(CUSTOMER_ALEX.getUsername())
				.request(MediaType.APPLICATION_JSON)
				.get(Customer.class);
		assertEquals(createdCustomer, CUSTOMER_ALEX);

		assertTrue(createdCustomer.getShoppingCart().isEmpty());
	}
}