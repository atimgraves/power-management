package graves.tim.powermanagement.weather.restclients;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "ForecastSolar")
@ApplicationScoped
public interface ForecastSolar {
	@GET
	@Path("/estimate/{lat}/{lon}/{dec}/{az}/kwp")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject getEstimateAllData(@PathParam(value = "lat") String lat, @PathParam(value = "lon") String lon,
			@PathParam(value = "dec") String dec, @PathParam(value = "az") String az,
			@PathParam(value = "kwp") String kwp);
}
