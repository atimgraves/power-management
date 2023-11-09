package graves.tim.powermanagement.server.batteries.manager;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import graves.tim.powermanagement.common.data.BatteryEvent;
import graves.tim.powermanagement.common.data.PerBatterySettings;
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
import graves.tim.powermanagement.common.exceptions.BatteryException;
import graves.tim.powermanagement.common.exceptions.BatteryInvalidParameterException;
import graves.tim.powermanagement.common.exceptions.BatteryNotFoundException;
import graves.tim.powermanagement.common.exceptions.BatteryProgrammingProblemException;
import graves.tim.powermanagement.common.exceptions.BatterySaveStateBadSavePathException;
import graves.tim.powermanagement.common.exceptions.BatterySaveStateSerializationException;
import graves.tim.powermanagement.common.exceptions.BatteryStateException;
import graves.tim.powermanagement.common.exceptions.BatteryUnauthorisedAccessException;
import graves.tim.powermanagement.common.exceptions.BatteryUnknownSettingException;
import graves.tim.powermanagement.common.exceptions.BatteryUnsupportedOperationException;
import graves.tim.powermanagement.server.batteries.BatteryFactory;
import graves.tim.powermanagement.server.batteries.GenericBattery;
import graves.tim.powermanagement.server.batteries.batteryimplementations.system.SystemBattery;
import graves.tim.powermanagement.server.batteries.manager.events.BatteryIndividualSchedulableEventScheduler;
import graves.tim.powermanagement.server.batteries.manager.events.active.BatterySchedulableEventFactory;
import graves.tim.powermanagement.server.batteries.manager.events.core.BatterySchedulableEvent;
import graves.tim.powermanagement.server.connectionsupport.AuthException;
import io.helidon.config.Config;
import io.helidon.config.ConfigValue;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.java.Log;

@Log
@ApplicationScoped
public class BatteryManager {
	public final static String BATTERY_TYPE_SYSTEM = "System"; // this is used as a place to put system wide scheduled
	public final static String SAVE_DIRECTORY_NAME = "saved-data" + File.separator + "batteries";
	// tasks
	private final static DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
	public final static String BATTERY_CONFIG_TREE_NAME = "battery";
	private Map<String, BatteryData> batteries = new TreeMap<>();

	@Inject
	private BatterySchedulableEventFactory batterySchedulableEventFactory;

	@Inject
	private BatteryIndividualSchedulableEventScheduler batteryIndividualSchedulableEventScheduler;

	// private Config config;
	@Inject
	public BatteryManager(Config config) throws BatteryException, AuthException {
		Config batteryTreeConfig = config.get(BATTERY_CONFIG_TREE_NAME);
		if (!batteryTreeConfig.exists()) {
			throw new BatteryException(
					"Config entry tree " + BATTERY_CONFIG_TREE_NAME + " is missing unable to continue");
		}

		ConfigValue<List<Config>> batteryNodesConfig = batteryTreeConfig.asNodeList();
		if (batteryNodesConfig.isPresent()) {
			List<Config> batteryNodes = batteryNodesConfig.get();
			if (batteryNodes.size() == 0) {
				throw new BatteryException(
						"No provided sub nodes of config " + BATTERY_CONFIG_TREE_NAME + " cannot load batteries");
			}
			for (Config batteryConfig : batteryNodes) {
				String batteryConfigName = batteryConfig.name();
				if (isSystemBattery(batteryConfigName)) {
					throw new BatteryException("Batteries cannot be named System as this is a reserved name");
				}
				log.info("Building battery " + batteryConfigName);
				GenericBattery battery = BatteryFactory.build(batteryConfigName, batteryConfig);
				BatteryData batteryData = new BatteryData(batteryConfigName, battery);
				battery.setBatteryData(batteryData);
				batteries.put(batteryConfigName, batteryData);
				// make sure we know the max charge level for the battery if it's not already
				// been setup
				if (!batteryData.hasDataItem(PerBatterySettings.MAXIMUM_BATTERY_LEVEL)) {
					int maxChargeLevel = battery.retrieveMaximumChargeLevel();
					batteryData.addDataItem(PerBatterySettings.MAXIMUM_BATTERY_LEVEL, maxChargeLevel);
				}
			}
			batteries.put(BATTERY_TYPE_SYSTEM, new BatteryData(BATTERY_TYPE_SYSTEM, new SystemBattery()));
		} else {
			throw new BatteryException("Config node " + BATTERY_CONFIG_TREE_NAME
					+ " exists, but is not reporting present when retrieveing subnodes");
		}
	}

	public boolean isSystemBattery(String batteryName) {
		return BATTERY_TYPE_SYSTEM.equalsIgnoreCase(batteryName);
	}

	public Collection<String> getBatteryNames() {
		return new ArrayList<>(batteries.keySet());
	}

	public BatteryData getBatteryDataByName(@NonNull String batteryName) throws BatteryNotFoundException {
		BatteryData batteryData = batteries.get(batteryName);
		if (batteryData == null) {
			throw new BatteryNotFoundException("Cannot locate battery data named " + batteryName);
		}
		return batteryData;
	}

	public GenericBattery getBatteryByName(@NonNull String batteryName) throws BatteryNotFoundException {
		BatteryData batteryData = batteries.get(batteryName);
		if (batteryData == null) {
			throw new BatteryNotFoundException(
					"Cannot locate battery data which contains the battery named " + batteryName);
		}
		return batteryData.retrieveBattery();
	}

	public Collection<BatteryEvent> getOutstandingTasks(@NonNull String batteryName) throws BatteryNotFoundException {
		BatteryData batteryData = getBatteryDataByName(batteryName);
		return batteryData.getOutstandingBatteryEvents();
	}

	public void addEvent(@NonNull BatteryEvent batteryEvent)
			throws BatteryEventUnknownEventTypeException, BatteryEventUnimplementedEventTypeException,
			BatteryEventEventTypeRequiresSystemBattery, BatteryEventEventTypeUnsupportedOnSystemBatteryException,
			BatteryNotFoundException, BatteryEventInPastException, BatteryEventNotYetSupportedEventTypeException {
		BatterySchedulableEvent bse = batterySchedulableEventFactory.buildEvent(batteryEvent);
		log.info("About to schedule event " + bse);
		bse.schedule(batteryIndividualSchedulableEventScheduler);
	}

	public BatteryEvent deleteTasks(String batteryName, long eventId, boolean deleteSystemEvent)
			throws BatteryNotFoundException, BatteryEventUnknownIdException, BatteryEventEventInProgressException,
			BatteryEventIsSystemEventException, BatteryEventOutstandingChildTasksException,
			BatteryEventActiveChildTasksException {
		BatteryData batteryData = getBatteryDataByName(batteryName);
		BatterySchedulableEvent batterySchedulableEvent = batteryData.getBatterySchedulableEvents().get(eventId);
		if (batterySchedulableEvent == null) {
			throw new BatteryEventUnknownIdException(
					"Cannot locate event with id " + eventId + " for battery named " + batteryName);
		}
		if (batterySchedulableEvent.isSystemEvent() && !deleteSystemEvent) {
			throw new BatteryEventIsSystemEventException(
					"This is a system event, unable to delete it unless you set deleteSystemEvent to true");
		}
		// try to get the event to delete, it may well object if there are active tasks
		// running or child tasks
		batterySchedulableEvent.deleteEvent();
		return batteryData.getBatterySchedulableEvents().remove(eventId).getBatteryEvent();
	}

	public BatteryEvent addEvent(String batteryName, BatteryEvent batteryEvent) throws BatteryNotFoundException,
			BatteryEventUnknownEventTypeException, BatteryEventUnimplementedEventTypeException,
			BatteryEventEventTypeRequiresSystemBattery, BatteryEventEventTypeUnsupportedOnSystemBatteryException,
			BatteryEventNotYetSupportedEventTypeException, BatteryEventInPastException {
		BatteryData batteryData = getBatteryDataByName(batteryName);
		BatterySchedulableEvent bse = batteryData.addEvent(batterySchedulableEventFactory, batteryEvent);
		// the event has been setup, but it needs to be scheduled
		bse.schedule(batteryIndividualSchedulableEventScheduler);
		return bse.getBatteryEvent();
	}

	public int getBatteryCurrentChargeLevel(String batteryName)
			throws BatteryNotFoundException, BatteryUnauthorisedAccessException, BatteryUnknownSettingException,
			BatteryInvalidParameterException, BatteryUnsupportedOperationException {
		BatteryData batteryData = getBatteryDataByName(batteryName);
		return batteryData.retrieveBattery().retrieveCurrentChargeLevel();
	}

	public int getBatteryReserveChargeLevel(String batteryName)
			throws BatteryNotFoundException, BatteryUnauthorisedAccessException, BatteryUnknownSettingException,
			BatteryInvalidParameterException, BatteryUnsupportedOperationException, BatteryProgrammingProblemException {
		BatteryData batteryData = getBatteryDataByName(batteryName);
		return batteryData.retrieveBattery().retrieveBatteryReserveLevel();
	}

	public String saveBatteryData(String batteryName) throws BatteryStateException, BatteryNotFoundException {
		File saveDirectory = new File(SAVE_DIRECTORY_NAME + File.separator + batteryName);
		saveDirectory.mkdirs();
		if (!saveDirectory.isDirectory()) {
			throw new BatterySaveStateBadSavePathException(
					"Provided name " + saveDirectory.getPath() + " is not a directory");
		}
		BatteryData batteryData = getBatteryDataByName(batteryName);
		ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
				.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).registerModule(new JavaTimeModule());
		String resp;
		try {
			resp = mapper.writeValueAsString(batteryData);
			System.out.println(resp);
			return resp;
		} catch (JsonProcessingException e) {
			throw new BatterySaveStateSerializationException("Problem serializing the battery data", e);
		}
	}
}
