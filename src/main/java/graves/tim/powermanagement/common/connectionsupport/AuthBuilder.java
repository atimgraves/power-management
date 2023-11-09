package graves.tim.powermanagement.common.connectionsupport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.java.Log;

@Log
public class AuthBuilder implements ClientRequestFilter {
	private String user, password;

	public AuthBuilder(String user, String password) {
		this.user = user;
		this.password = password;
	}

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		MultivaluedMap<String, Object> headers = requestContext.getHeaders();
		// remove any existing authorization
		headers.remove(HttpHeaders.AUTHORIZATION);
		// Build a new authorization header
		String auth = user + ":" + password;
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.ISO_8859_1));
		String authHeader = "Basic " + new String(encodedAuth);
		// apply the new authorization header
		headers.add(HttpHeaders.AUTHORIZATION, authHeader);
		log.finer("Added header " + HttpHeaders.AUTHORIZATION + " with base64 encoded value " + auth);
	}
}