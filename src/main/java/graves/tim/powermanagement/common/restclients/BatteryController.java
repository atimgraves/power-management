package graves.tim.powermanagement.common.restclients;

import java.util.Collection;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import graves.tim.powermanagement.common.data.BatteryConfigurationSetting;
import graves.tim.powermanagement.common.data.BatteryEvent;
import graves.tim.powermanagement.common.data.DataItem;
import graves.tim.powermanagement.common.data.DataItemIncorrectBooleanFormatException;
import graves.tim.powermanagement.common.data.DataItemIncorrectIntegerFormatException;
import graves.tim.powermanagement.common.data.DataItemIncorrectTypeException;
import graves.tim.powermanagement.common.data.DataItemNotFoundException;
import graves.tim.powermanagement.common.data.EventDataItems;
import graves.tim.powermanagement.common.exceptions.BatteryEventActiveChildTasksException;
import graves.tim.powermanagement.common.exceptions.BatteryEventEventInProgressException;
import graves.tim.powermanagement.common.exceptions.BatteryEventEventTypeRequiresSystemBattery;
import graves.tim.powermanagement.common.exceptions.BatteryEventEventTypeUnsupportedOnSystemBatteryException;
import graves.tim.powermanagement.common.exceptions.BatteryEventInPastException;
import graves.tim.powermanagement.common.exceptions.BatteryEventIsSystemEventException;
import graves.tim.powermanagement.common.exceptions.BatteryEventNotYetSupportedEventTypeException;
import graves.tim.powermanagement.common.exceptions.BatteryEventOutstandingChildTasksException;
import graves.tim.powermanagement.common.exceptions.BatteryEventUnimplementedEventTypeException;
import graves.tim.powermanagement.common.exceptions.BatteryEventUnknownEventTypeException;
import graves.tim.powermanagement.common.exceptions.BatteryEventUnknownIdException;
import graves.tim.powermanagement.common.exceptions.BatteryInvalidParameterException;
import graves.tim.powermanagement.common.exceptions.BatteryNotFoundException;
import graves.tim.powermanagement.common.exceptions.BatteryProgrammingProblemException;
import graves.tim.powermanagement.common.exceptions.BatteryReadOnlySettingException;
import graves.tim.powermanagement.common.exceptions.BatteryStateException;
import graves.tim.powermanagement.common.exceptions.BatteryUnauthorisedAccessException;
import graves.tim.powermanagement.common.exceptions.BatteryUnexpectedResponseException;
import graves.tim.powermanagement.common.exceptions.BatteryUnknownSettingException;
import graves.tim.powermanagement.common.exceptions.BatteryUnsupportedOperationException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient()
@Path("/battery")
public interface BatteryController {
	@GET
	@Path("/batteries")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<String> getBatteries();

	@POST
	@Path("/events/{batteryname}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BatteryEvent addEvent(@PathParam(value = "batteryname") String batteryName, BatteryEvent batteryEvent)
			throws BatteryNotFoundException, BatteryEventUnknownEventTypeException,
			BatteryEventUnimplementedEventTypeException, BatteryEventEventTypeRequiresSystemBattery,
			BatteryEventEventTypeUnsupportedOnSystemBatteryException, BatteryEventNotYetSupportedEventTypeException,
			BatteryEventInPastException;

	@GET
	@Path("/events/{batteryname}")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<BatteryEvent> getOutstandingEvents(@PathParam(value = "batteryname") String batteryName)
			throws BatteryNotFoundException;

	@DELETE
	@Path("/events/{batteryname}/{eventid}")
	@Produces(MediaType.APPLICATION_JSON)
	public BatteryEvent deleteEvent(@PathParam(value = "batteryname") String batteryName,
			@PathParam(value = "eventid") long eventId) throws BatteryNotFoundException, BatteryEventUnknownIdException,
			BatteryEventEventInProgressException, BatteryEventIsSystemEventException,
			BatteryEventOutstandingChildTasksException, BatteryEventActiveChildTasksException;

	@DELETE
	@Path("/events/{batteryname}/{eventid}/{deleteSystemEvent}")
	@Produces(MediaType.APPLICATION_JSON)
	public BatteryEvent deleteEvent(@PathParam(value = "batteryname") String batteryName,
			@PathParam(value = "eventid") long eventId,
			@PathParam(value = "deleteSystemEvent") boolean deleteSystemEvent) throws BatteryNotFoundException,
			BatteryEventUnknownIdException, BatteryEventEventInProgressException, BatteryEventIsSystemEventException,
			BatteryEventOutstandingChildTasksException, BatteryEventActiveChildTasksException;

	@GET
	@Path("/info/batteryCurrentChargeLevel/{batteryname}")
	@Produces(MediaType.APPLICATION_JSON)
	public int getBatteryCurrentChargeLevel(@PathParam(value = "batteryname") String batteryName)
			throws BatteryNotFoundException, BatteryUnauthorisedAccessException, BatteryUnknownSettingException,
			BatteryInvalidParameterException, BatteryUnsupportedOperationException;

	@GET
	@Path("/info/batteryReserveChargeLevel/{batteryname}")
	@Produces(MediaType.APPLICATION_JSON)
	public int getBatteryReserveChargeLevel(@PathParam(value = "batteryname") String batteryName)
			throws BatteryNotFoundException, BatteryUnauthorisedAccessException, BatteryUnknownSettingException,
			BatteryInvalidParameterException, BatteryUnsupportedOperationException, BatteryProgrammingProblemException;

	@GET
	@Path("/tasks/perBatterySettings/{batteryname}")
	@Produces(MediaType.APPLICATION_JSON)
	public EventDataItems getPerBatterySettings(@PathParam(value = "batteryname") String batteryName)
			throws BatteryNotFoundException;

	@GET
	@Path("/tasks/perBatterySetting/{batteryname}/{settingname}")
	@Produces(MediaType.APPLICATION_JSON)
	public DataItem getPerBatterySetting(@PathParam(value = "batteryname") String batteryName,
			@PathParam(value = "settingname") String settingName)
			throws BatteryNotFoundException, DataItemNotFoundException;

	@POST
	@Path("/tasks/perBatterySetting/{batteryname}/{settingname}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public DataItem setPerBatterySetting(@PathParam(value = "batteryname") String batteryName,
			@PathParam(value = "settingname") String settingName, DataItem dataItem) throws BatteryNotFoundException;

	@GET
	@Path("/tasks/configurationSetting/listsupported/{batteryname}")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<BatteryConfigurationSetting> listSupportedConfigurationSettings(
			@PathParam(value = "batteryname") String batteryName) throws BatteryNotFoundException;

	@POST
	@Path("/tasks/configurationSetting/saved/save/{batteryname}/{setting}/{savename}")
	@Produces(MediaType.APPLICATION_JSON)
	public DataItem saveConfigurationToSavedConfiguration(@PathParam(value = "batteryname") String batteryName,
			@PathParam(value = "setting") BatteryConfigurationSetting setting,
			@PathParam(value = "savename") String saveName) throws BatteryNotFoundException, DataItemNotFoundException,
			BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, DataItemIncorrectTypeException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException;

	@GET
	@Path("/tasks/configurationSetting/saved/restore/{batteryname}/{setting}/{savename}")
	@Produces(MediaType.APPLICATION_JSON)
	public DataItem restoreSavedConfiguration(@PathParam(value = "batteryname") String batteryName,
			@PathParam(value = "setting") BatteryConfigurationSetting setting,
			@PathParam(value = "savename") String saveName) throws BatteryNotFoundException, DataItemNotFoundException,
			BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, DataItemIncorrectTypeException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException, BatteryNotFoundException,
			DataItemNotFoundException, BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException, BatteryUnsupportedOperationException, DataItemIncorrectTypeException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException, BatteryNotFoundException,
			DataItemNotFoundException, BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException, BatteryReadOnlySettingException;

	@GET
	@Path("/tasks/configurationSetting/previous/restore/{batteryname}/{setting}")
	@Produces(MediaType.APPLICATION_JSON)
	public DataItem restorePreviousConfiguration(@PathParam(value = "batteryname") String batteryName,
			@PathParam(value = "setting") BatteryConfigurationSetting setting) throws BatteryNotFoundException,
			DataItemNotFoundException, BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException, BatteryUnsupportedOperationException, DataItemIncorrectTypeException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException, BatteryNotFoundException,
			DataItemNotFoundException, BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException, BatteryUnsupportedOperationException, DataItemIncorrectTypeException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException, BatteryNotFoundException,
			DataItemNotFoundException, BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException, BatteryReadOnlySettingException;

	@GET
	@Path("/tasks/configurationSetting/saved/get/{batteryname}/{setting}/{savename}")
	@Produces(MediaType.APPLICATION_JSON)

	public DataItem getSavedConfiguration(@PathParam(value = "batteryname") String batteryName,
			@PathParam(value = "setting") BatteryConfigurationSetting setting,
			@PathParam(value = "savename") String saveName) throws BatteryNotFoundException,
			DataItemIncorrectTypeException, DataItemNotFoundException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectIntegerFormatException, BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException, BatteryUnsupportedOperationException;

	@GET
	@Path("/tasks/configurationSetting/previous/get/{batteryname}/{setting}")
	@Produces(MediaType.APPLICATION_JSON)
	public DataItem getPreviousConfiguration(@PathParam(value = "batteryname") String batteryName,
			@PathParam(value = "setting") BatteryConfigurationSetting setting) throws BatteryNotFoundException,
			DataItemIncorrectTypeException, DataItemNotFoundException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectIntegerFormatException, BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException, BatteryUnsupportedOperationException;

	@GET
	@Path("/tasks/configurationSetting/saved/remove/{batteryname}/{setting}/{savename}")
	@Produces(MediaType.APPLICATION_JSON)
	public DataItem removeSavedConfiguration(@PathParam(value = "batteryname") String batteryName,
			@PathParam(value = "setting") BatteryConfigurationSetting setting,
			@PathParam(value = "savename") String saveName) throws BatteryNotFoundException, DataItemNotFoundException,
			BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, DataItemIncorrectTypeException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException;

	@GET
	@Path("/tasks/configurationSetting/previous/remove/{batteryname}/{setting}")
	@Produces(MediaType.APPLICATION_JSON)
	public DataItem removePreviousConfiguration(@PathParam(value = "batteryname") String batteryName,
			@PathParam(value = "setting") BatteryConfigurationSetting setting) throws BatteryNotFoundException,
			DataItemNotFoundException, BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException, BatteryUnsupportedOperationException, DataItemIncorrectTypeException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException;

	@GET
	@Path("/tasks/configurationSetting/saved/exists/{batteryname}/{setting}/{savename}")
	@Produces(MediaType.APPLICATION_JSON)
	public Boolean savedConfigurationExists(@PathParam(value = "batteryname") String batteryName,
			@PathParam(value = "setting") BatteryConfigurationSetting setting,
			@PathParam(value = "savename") String saveName)
			throws BatteryNotFoundException, BatteryUnknownSettingException;

	@GET
	@Path("/tasks/configurationSetting/saved/list/{batteryname}")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<String> listSavedConfigSettingNames(@PathParam(value = "batteryname") String batteryName)
			throws BatteryNotFoundException, BatteryUnknownSettingException;

	@GET
	@Path("/tasks/configurationSetting/saved/list/{batteryname}/{setting}")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<String> listSavedConfigSettingNames(@PathParam(value = "batteryname") String batteryName,
			@PathParam(value = "setting") BatteryConfigurationSetting setting)
			throws BatteryNotFoundException, BatteryUnknownSettingException;

	@GET
	@Path("/tasks/configurationSetting/previous/exists/{batteryname}/{setting}")
	@Produces(MediaType.APPLICATION_JSON)
	public Boolean previousConfigurationExists(@PathParam(value = "batteryname") String batteryName,
			@PathParam(value = "setting") BatteryConfigurationSetting setting)
			throws BatteryNotFoundException, BatteryUnknownSettingException;

	@GET
	@Path("/tasks/configurationSetting/previous/list/{batteryname}")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<String> listPreviousConfigSettingNames(@PathParam(value = "batteryname") String batteryName)
			throws BatteryNotFoundException, BatteryUnknownSettingException;

	@GET
	@Path("/tasks/operatingModes/{batteryname}")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<String> getOperatingModes(@PathParam(value = "batteryname") String batteryName)
			throws BatteryNotFoundException;

	@GET
	@Path("/tasks/operatingMode/{batteryname}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getOperatingMode(@PathParam(value = "batteryname") String batteryName)
			throws BatteryNotFoundException, DataItemNotFoundException, BatteryUnsupportedOperationException,
			BatteryUnauthorisedAccessException, BatteryUnexpectedResponseException;

	@POST
	@Path("/tasks/operatingMode/{batteryname}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String setOperatingMode(@PathParam(value = "batteryname") String batteryName, String modeName)
			throws BatteryNotFoundException, BatteryInvalidParameterException, BatteryUnsupportedOperationException,
			BatteryUnauthorisedAccessException, BatteryUnexpectedResponseException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, BatteryReadOnlySettingException;

	@POST
	@Path("/tasks/saveState/{batteryname}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String saveBatteryState(@PathParam(value = "batteryname") String batteryName)
			throws BatteryNotFoundException, BatteryStateException;

}
