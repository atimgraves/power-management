package graves.tim.powermanagement.server.batteries.batteryimplementations.sonnen;

import java.net.URI;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import graves.tim.powermanagement.common.exceptions.BatteryException;
import graves.tim.powermanagement.server.connectionsupport.AuthException;
import graves.tim.powermanagement.server.connectionsupport.AuthFilterSelector;
import graves.tim.powermanagement.server.connectionsupport.DebugRequestBody;
import io.helidon.config.Config;
import jakarta.ws.rs.client.ClientRequestFilter;
import lombok.extern.java.Log;

@Log
public class SonnenV2BatteryClientFactory {
	public final static String BATTERY_HOST = "host";
	public final static String BATTERY_PORT = "port";
	public final static int BATTERY_PORT_DEFAULT = 80;
	public final static String BATTERY_CONNECTION_HTTPS = "connectionHttps";
	public final static String HTTPS = "https";
	public final static String HTTP = "http";
	public final static String BATTERY_CONNECTION_DEBUG = "connectionDebug";

	public static SonnenV2 buildClient(String batteryConfigName, Config batteryConfig)
			throws BatteryException, AuthException {
		log.info("working on battery config " + batteryConfig.name());
		if (!batteryConfig.get(BATTERY_HOST).exists()) {
			throw new BatteryException(
					"Unable to locate battery host details for batteryConfig named " + batteryConfigName);
		}
		String batteryHost = batteryConfig.get(BATTERY_HOST).asString().get();
		int batteryPort;
		if (!batteryConfig.get(BATTERY_PORT).exists()) {
			batteryPort = BATTERY_PORT_DEFAULT;
		} else {
			batteryPort = batteryConfig.get(BATTERY_PORT).asInt().get();
		}
		String batteryConnectionType = HTTP;
		if (batteryConfig.get(BATTERY_CONNECTION_HTTPS).exists()) {
			batteryConnectionType = batteryConfig.get(BATTERY_CONNECTION_HTTPS).asBoolean().get() ? HTTPS : HTTP;
		}

		boolean debugRequestBodies = false;

		if (batteryConfig.get(BATTERY_CONNECTION_DEBUG).exists()) {
			debugRequestBodies = batteryConfig.get(BATTERY_CONNECTION_DEBUG).asBoolean().get();
		}
		String batteryBaseURI = batteryConnectionType + "://" + batteryHost + ":" + batteryPort;
		log.info("Connecting to " + batteryBaseURI);
		ClientRequestFilter authFilter = AuthFilterSelector.getAuthFilter(batteryConfigName, batteryConfig);
		RestClientBuilder builder = RestClientBuilder.newBuilder().baseUri(URI.create(batteryBaseURI))
				.register(authFilter);
		if (debugRequestBodies) {
			builder = builder.register(new DebugRequestBody());
		}
		return builder.build(SonnenV2.class);

	}
}
