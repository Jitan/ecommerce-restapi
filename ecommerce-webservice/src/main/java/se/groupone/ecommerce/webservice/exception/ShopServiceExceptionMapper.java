package se.groupone.ecommerce.webservice.exception;

import se.groupone.ecommerce.exception.ShopServiceException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public final class ShopServiceExceptionMapper implements ExceptionMapper<ShopServiceException>
{
	@Override
	public Response toResponse(ShopServiceException e)
	{
		e.printStackTrace();
		return Response.status(Status.BAD_REQUEST).entity(e.getMessage())
				.build();
	}
}
