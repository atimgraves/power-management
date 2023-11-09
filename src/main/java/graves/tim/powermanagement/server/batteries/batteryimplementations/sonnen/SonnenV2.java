package graves.tim.powermanagement.server.batteries.batteryimplementations.sonnen;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient()
@Path("/api/v2")
public interface SonnenV2 {
	@GET
	@Path("/status")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject getStatus();

	@GET
	@Path("/latestdata")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject getLatestData();

	@GET
	@Path("/powermeter")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonArray getPowerMeter();

	@GET
	@Path("/configurations/{configName}")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject getConfiguration(@PathParam("configName") String configName);

	@PUT
	@Path("/configurations")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public JsonObject setConfiguration(String settingString);
}
