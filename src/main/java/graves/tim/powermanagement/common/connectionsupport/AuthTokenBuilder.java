package graves.tim.powermanagement.common.connectionsupport;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.java.Log;

@Log
public class AuthTokenBuilder implements ClientRequestFilter {
	public final static String AUTH_TOKEN = "Auth-Token";
	private String token;

	public AuthTokenBuilder(String token) {
		this.token = token;
	}

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		MultivaluedMap<String, Object> headers = requestContext.getHeaders();
		// remove any existing authorization
		headers.remove(AUTH_TOKEN);
		// apply the new authorization header
		headers.add(AUTH_TOKEN, token);
		log.finer("Added token " + AUTH_TOKEN + " with value " + token);
	}
}