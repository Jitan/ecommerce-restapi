package se.groupone.ecommerce.webservice.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public final class BadLoginException extends WebApplicationException
{
	private static final long serialVersionUID = 3899197009093505203L;

	public BadLoginException(String message)
	{
		super(Response.status(Status.UNAUTHORIZED)
				.entity("Bad login: " + message)
				.build());
	}
}