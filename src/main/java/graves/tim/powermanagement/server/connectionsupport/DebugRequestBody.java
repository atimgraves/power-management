package graves.tim.powermanagement.server.connectionsupport;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import lombok.extern.java.Log;

@Log
public class DebugRequestBody implements ClientRequestFilter {

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		Object entity = requestContext.getEntity();
		String entityContents = entity == null ? "No entity contents" : entity.toString();
		String entityClass = entity == null ? "No entity contents, no class"
				: requestContext.getEntityClass().getName();
		log.finer("Entity class is " + entityClass + "  with contents " + entityContents);
	}
}