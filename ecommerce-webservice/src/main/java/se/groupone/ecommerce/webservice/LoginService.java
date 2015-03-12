package se.groupone.ecommerce.webservice;

import se.groupone.ecommerce.exception.RepositoryException;
import se.groupone.ecommerce.model.Customer;
import se.groupone.ecommerce.webservice.exception.BadLoginException;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("login")
public class LoginService extends WebShopService
{
	public LoginService() throws RepositoryException
	{
		super();
	}

	@PUT
	public Response verifyUser(@HeaderParam("username") final String username, @HeaderParam("password") final String password)
	{
		Customer customerToBeVerified = shopService.getCustomer(username);

		if(customerToBeVerified.getPassword().equals(password))
		{
			return Response.ok().build();
		}
		else
		{
			throw new BadLoginException("No matching username and password combination, sorry");
		}
	}
}
