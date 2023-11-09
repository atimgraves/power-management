package graves.tim.powermanagement.server.resources;

import java.util.Collection;

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
import graves.tim.powermanagement.common.restclients.BatteryController;
import graves.tim.powermanagement.server.batteries.GenericBattery;
import graves.tim.powermanagement.server.batteries.manager.BatteryManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class BatteryControllerImpl implements BatteryController {
	private long eventIdCounter = 0;
	@Inject
	private BatteryManager batteryManager;

	@Override
	public Collection<String> getBatteries() {
		return batteryManager.getBatteryNames();
	}

	@Override
	public Collection<BatteryEvent> getOutstandingEvents(String batteryName) throws BatteryNotFoundException {
		return batteryManager.getOutstandingTasks(batteryName);
	}

	@Override
	public BatteryEvent deleteEvent(String batteryName, long eventId) throws BatteryEventUnknownIdException,
			BatteryEventEventInProgressException, BatteryEventIsSystemEventException, BatteryNotFoundException,
			BatteryEventOutstandingChildTasksException, BatteryEventActiveChildTasksException {
		return this.deleteEvent(batteryName, eventId, false);
	}

	@Override
	public BatteryEvent deleteEvent(String batteryName, long eventId, boolean deleteSystemEvent)
			throws BatteryNotFoundException, BatteryEventUnknownIdException, BatteryEventEventInProgressException,
			BatteryEventIsSystemEventException, BatteryEventOutstandingChildTasksException,
			BatteryEventActiveChildTasksException {
		return batteryManager.deleteTasks(batteryName, eventId, deleteSystemEvent);
	}

	@Override
	public BatteryEvent addEvent(String batteryName, BatteryEvent batteryEvent) throws BatteryNotFoundException,
			BatteryEventUnknownEventTypeException, BatteryEventUnimplementedEventTypeException,
			BatteryEventEventTypeRequiresSystemBattery, BatteryEventEventTypeUnsupportedOnSystemBatteryException,
			BatteryEventNotYetSupportedEventTypeException, BatteryEventInPastException {
		batteryEvent.setEventId(eventIdCounter++);
		log.info("Received event " + batteryEvent);
		return batteryManager.addEvent(batteryName, batteryEvent);
	}

	@Override
	public int getBatteryCurrentChargeLevel(String batteryName)
			throws BatteryNotFoundException, BatteryUnauthorisedAccessException, BatteryUnknownSettingException,
			BatteryInvalidParameterException, BatteryUnsupportedOperationException {
		return batteryManager.getBatteryCurrentChargeLevel(batteryName);
	}

	@Override
	public int getBatteryReserveChargeLevel(String batteryName)
			throws BatteryNotFoundException, BatteryUnauthorisedAccessException, BatteryUnknownSettingException,
			BatteryInvalidParameterException, BatteryUnsupportedOperationException, BatteryProgrammingProblemException {
		return batteryManager.getBatteryReserveChargeLevel(batteryName);
	}

	@Override
	public EventDataItems getPerBatterySettings(String batteryName) throws BatteryNotFoundException {
		return batteryManager.getBatteryDataByName(batteryName);
	}

	@Override
	public DataItem getPerBatterySetting(String batteryName, String settingName) throws BatteryNotFoundException {
		try {
			return batteryManager.getBatteryDataByName(batteryName).dataItem(settingName);
		} catch (DataItemNotFoundException e) {
			return null;
		}
	}

	@Override
	public DataItem setPerBatterySetting(String batteryName, String settingName, DataItem dataItem)
			throws BatteryNotFoundException {
		return batteryManager.getBatteryDataByName(batteryName).addDataItem(settingName, dataItem);
	}

	@Override
	public Collection<String> getOperatingModes(String batteryName) throws BatteryNotFoundException {
		return batteryManager.getBatteryByName(batteryName).getOperatingModes();
	}

	@Override
	public String getOperatingMode(String batteryName)
			throws BatteryNotFoundException, DataItemNotFoundException, BatteryUnsupportedOperationException,
			BatteryUnauthorisedAccessException, BatteryUnexpectedResponseException {
		return batteryManager.getBatteryByName(batteryName).getOperatingMode();
	}

	@Override
	public String setOperatingMode(String batteryName, String modeName)
			throws BatteryNotFoundException, BatteryInvalidParameterException, BatteryUnsupportedOperationException,
			BatteryUnauthorisedAccessException, BatteryUnexpectedResponseException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, BatteryReadOnlySettingException {
		return batteryManager.getBatteryByName(batteryName).setOperatingMode(modeName);

	}

	@Override
	public String saveBatteryState(String batteryName) throws BatteryNotFoundException, BatteryStateException {
		return batteryManager.saveBatteryData(batteryName);
	}

	@Override
	public DataItem saveConfigurationToSavedConfiguration(String batteryName, BatteryConfigurationSetting setting,
			String saveName) throws BatteryNotFoundException, DataItemNotFoundException,
			BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, DataItemIncorrectTypeException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException {
		GenericBattery battery = batteryManager.getBatteryByName(batteryName);
		return battery.saveConfigurationToSavedConfiguration(setting, saveName);
	}

	@Override
	public DataItem restoreSavedConfiguration(String batteryName, BatteryConfigurationSetting setting, String saveName)
			throws BatteryNotFoundException, DataItemNotFoundException, BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException, BatteryUnsupportedOperationException,
			DataItemIncorrectTypeException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectIntegerFormatException, BatteryReadOnlySettingException {
		GenericBattery battery = batteryManager.getBatteryByName(batteryName);
		return battery.restoreSavedConfiguration(setting, saveName);
	}

	@Override
	public DataItem restorePreviousConfiguration(String batteryName, BatteryConfigurationSetting setting)
			throws BatteryNotFoundException, DataItemNotFoundException, BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException, BatteryUnsupportedOperationException,
			DataItemIncorrectTypeException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectIntegerFormatException, BatteryReadOnlySettingException {
		GenericBattery battery = batteryManager.getBatteryByName(batteryName);
		return battery.restorePreviousConfiguration(setting);
	}

	@Override
	public DataItem removeSavedConfiguration(String batteryName, BatteryConfigurationSetting setting, String saveName)
			throws BatteryNotFoundException, DataItemNotFoundException, BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException, BatteryUnsupportedOperationException,
			DataItemIncorrectTypeException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectIntegerFormatException {
		GenericBattery battery = batteryManager.getBatteryByName(batteryName);
		return battery.removeSavedConfiguration(setting, saveName);
	}

	@Override
	public DataItem removePreviousConfiguration(String batteryName, BatteryConfigurationSetting setting)
			throws BatteryNotFoundException, DataItemNotFoundException, BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException, BatteryUnsupportedOperationException,
			DataItemIncorrectTypeException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectIntegerFormatException {
		GenericBattery battery = batteryManager.getBatteryByName(batteryName);
		return battery.removePreviousConfiguration(setting);
	}

	@Override
	public DataItem getSavedConfiguration(String batteryName, BatteryConfigurationSetting setting, String saveName)
			throws BatteryNotFoundException, DataItemIncorrectTypeException, DataItemNotFoundException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException,
			BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException {
		GenericBattery battery = batteryManager.getBatteryByName(batteryName);
		return battery.getSavedConfiguration(setting, saveName);
	}

	@Override
	public DataItem getPreviousConfiguration(String batteryName, BatteryConfigurationSetting setting)
			throws BatteryNotFoundException, DataItemIncorrectTypeException, DataItemNotFoundException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException,
			BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException {
		GenericBattery battery = batteryManager.getBatteryByName(batteryName);
		return battery.getPreviousConfiguration(setting);
	}

	@Override
	public Boolean savedConfigurationExists(String batteryName, BatteryConfigurationSetting setting, String saveName)
			throws BatteryNotFoundException, BatteryUnknownSettingException {
		GenericBattery battery = batteryManager.getBatteryByName(batteryName);
		return battery.savedConfigurationExists(setting, saveName);
	}

	@Override
	public Boolean previousConfigurationExists(String batteryName, BatteryConfigurationSetting setting)
			throws BatteryNotFoundException, BatteryUnknownSettingException {
		GenericBattery battery = batteryManager.getBatteryByName(batteryName);
		return battery.previousConfigurationExists(setting);
	}

	@Override
	public Collection<String> listSavedConfigSettingNames(String batteryName)
			throws BatteryNotFoundException, BatteryUnknownSettingException {
		GenericBattery battery = batteryManager.getBatteryByName(batteryName);
		return battery.listSavedConfigSettingNames();
	}

	@Override
	public Collection<String> listSavedConfigSettingNames(String batteryName, BatteryConfigurationSetting setting)
			throws BatteryNotFoundException, BatteryUnknownSettingException {
		GenericBattery battery = batteryManager.getBatteryByName(batteryName);
		return battery.listSavedConfigSettingNames(setting);
	}

	@Override
	public Collection<String> listPreviousConfigSettingNames(String batteryName)
			throws BatteryNotFoundException, BatteryUnknownSettingException {
		GenericBattery battery = batteryManager.getBatteryByName(batteryName);
		return battery.listPreviousConfigSettingNames();
	}

	@Override
	public Collection<BatteryConfigurationSetting> listSupportedConfigurationSettings(String batteryName)
			throws BatteryNotFoundException {
		GenericBattery battery = batteryManager.getBatteryByName(batteryName);
		return battery.listSupportedConfigurationSettings();
	}
}
