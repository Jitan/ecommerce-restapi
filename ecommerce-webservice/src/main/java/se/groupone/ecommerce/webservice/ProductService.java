package se.groupone.ecommerce.webservice;

import se.groupone.ecommerce.exception.RepositoryException;
import se.groupone.ecommerce.model.Product;
import se.groupone.ecommerce.model.ProductParameters;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.ArrayList;

@Path("products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductService extends WebShopService
{
	public ProductService() throws RepositoryException
	{
		super();
	}

	@Context
	private UriInfo uriInfo;

	//  Skapa en ny produkt – detta ska returnera en länk till den skapade
	// produkten i Location-headern
	@POST
	public Response createProduct(ProductParameters productParameters)
	{
		Product product = shopService.addProduct(productParameters);
		final String createdProductIdString = String.valueOf(product.getId());

		final URI location = uriInfo.getAbsolutePathBuilder().path(createdProductIdString).build();
		return Response.created(location).build();
	}

	//  Hämta alla produkter
	@GET
	public Response getProducts()
	{
		ArrayList<Product> products = (ArrayList<Product>) shopService.getProducts();

		// GenericEntity is created for ProductListMapper generic handling
		return Response.ok(new GenericEntity<ArrayList<Product>>(products)
		{
		}).build();
	}

	//  Hämta en produkt med ett visst id
	@GET
	@Path("{productId}")
	public Response getProduct(@PathParam("productId") final String productId)
	{
		try
		{
			int productIdInt = Integer.parseInt(productId);
			Product product = shopService.getProductWithId(productIdInt);

			return Response.ok(product).build();
		}
		catch (NumberFormatException e)
		{
			return Response.status(Status.BAD_REQUEST)
					.entity("Product id must be parsable to an integer.").build();
		}
	}

	//  Uppdatera en produkt
	@PUT
	@Path("{productId}")
	public Response putProduct(@PathParam("productId") final String productId,
			final ProductParameters productParameters)
	{
		try
		{
			int productIdInt = Integer.parseInt(productId);
			shopService.updateProduct(productIdInt, productParameters);
			return Response.status(Status.NO_CONTENT).build();
		}
		catch (NumberFormatException e)
		{
			return Response.status(Status.BAD_REQUEST)
					.entity("Product id must be parsable to an integer.").build();
		}
	}

	//  Ta bort en produkt (eller sätta den som inaktiv)
	@DELETE
	@Path("{productId}")
	public Response deleteProduct(@PathParam("productId") final String productId)
	{
		try
		{
			int productIdInt = Integer.parseInt(productId);
			shopService.removeProduct(productIdInt);
			return Response.noContent().build();
		}
		catch (NumberFormatException e)
		{
			return Response.status(Status.BAD_REQUEST)
					.entity("Product id must be parsable to an integer.").build();
		}
	}
}