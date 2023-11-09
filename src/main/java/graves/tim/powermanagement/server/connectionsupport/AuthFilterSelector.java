package graves.tim.powermanagement.server.connectionsupport;

import graves.tim.powermanagement.common.connectionsupport.AuthTokenBuilder;
import io.helidon.config.Config;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.ClientRequestFilter;
import lombok.extern.java.Log;

@Log
@ApplicationScoped
public final class AuthFilterSelector {
	public final static String AUTH_TYPE = "authType";
	public final static String AUTH_TYPE_TOKEN = "TOKEN";
	public final static String AUTH_TOKEN = "authToken";

	public static ClientRequestFilter getAuthFilter(String batteryConfigName, Config batteryConfig)
			throws AuthException {
		if (!batteryConfig.get(AUTH_TYPE).exists()) {
			throw new AuthException("Config entry " + batteryConfigName + "." + AUTH_TYPE
					+ " is missing from the config battery tree, cannot continue");
		} else {
			String authType = batteryConfig.get(AUTH_TYPE).asString().get();
			if (authType.equalsIgnoreCase(AUTH_TYPE_TOKEN)) {
				// this type requires a token
				if (!batteryConfig.get(AUTH_TOKEN).exists()) {
					throw new AuthException("Config entry " + batteryConfigName + "." + AUTH_TOKEN
							+ " is missing from the config battery tree when auth type is set to token, cannot continue");
				} else {
					String authToken = batteryConfig.get(AUTH_TOKEN).asString().get();
					log.info(batteryConfigName + " type token value using token " + authToken);
					return new AuthTokenBuilder(authToken);
				}
			} else {
				throw new AuthException(batteryConfigName + "." + AUTH_TYPE + " in the config battery tree has value "
						+ authType + " it is unknown, cannot continue");
			}
		}
	}
}
