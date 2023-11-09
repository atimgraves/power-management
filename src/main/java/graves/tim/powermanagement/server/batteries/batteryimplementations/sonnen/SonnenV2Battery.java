package graves.tim.powermanagement.server.batteries.batteryimplementations.sonnen;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import graves.tim.powermanagement.common.data.BatteryConfigurationSetting;
import graves.tim.powermanagement.common.data.ChargeTimeSetting;
import graves.tim.powermanagement.common.data.DataItem;
import graves.tim.powermanagement.common.data.DataItemIncorrectBooleanFormatException;
import graves.tim.powermanagement.common.data.DataItemIncorrectIntegerFormatException;
import graves.tim.powermanagement.common.data.DataItemIncorrectTypeException;
import graves.tim.powermanagement.common.data.DataItemNotFoundException;
import graves.tim.powermanagement.common.data.DataType;
import graves.tim.powermanagement.common.exceptions.BatteryException;
import graves.tim.powermanagement.common.exceptions.BatteryInvalidParameterException;
import graves.tim.powermanagement.common.exceptions.BatteryProgrammingProblemException;
import graves.tim.powermanagement.common.exceptions.BatteryReadOnlySettingException;
import graves.tim.powermanagement.common.exceptions.BatteryUnauthorisedAccessException;
import graves.tim.powermanagement.common.exceptions.BatteryUnexpectedResponseException;
import graves.tim.powermanagement.common.exceptions.BatteryUnknownSettingException;
import graves.tim.powermanagement.common.exceptions.BatteryUnsupportedOperationException;
import graves.tim.powermanagement.server.batteries.batteryimplementations.BatteryCore;
import graves.tim.powermanagement.server.batteries.batteryimplementations.ConfigurationSettingEntry;
import graves.tim.powermanagement.server.batteries.manager.BatteryData;
import graves.tim.powermanagement.server.connectionsupport.AuthException;
import io.helidon.config.Config;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
public class SonnenV2Battery extends BatteryCore {
	private static final String STRING_QUOTE = "\"";
	private static boolean mapsConfigured = false;
	protected static Map<String, String> operatingModes = new HashMap<>();
	@Setter
	@Getter
	private BatteryData batteryData;
	private final static String CURRENT_CHARGE_LEVEL = "USOC";
	private final static String MINIMUM_DISHARGE_LEVEL = "EM_USOC";
	private final static String TIME_OF_USE_SCHEDULE = "EM_ToU_Schedule";
	private final static String OPERATING_MODE = "EM_OperatingMode";
	private static final int MAXIMUM_CHARGE_LEVEL = 100;

	private SonnenV2 battery;

	public SonnenV2Battery(String batteryConfigName, Config batteryConfig) throws BatteryException, AuthException {
		battery = SonnenV2BatteryClientFactory.buildClient(batteryConfigName, batteryConfig);
		log.info("Built battery");
		int powerLevel = -1;
		try {
			powerLevel = retrieveCurrentChargeLevel();
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("Battery current power level is " + powerLevel);

		synchronized (SonnenV2Battery.class) {
			if (!mapsConfigured) {
				setupSupportedConfigurations();
				setupOperatingModes();
				mapsConfigured = true;
			}
		}
	}

	/**
	 * 
	 */
	private static void setupSupportedConfigurations() {
		configurationSettings.put(BatteryConfigurationSetting.RESERVE_LEVEL,
				new ConfigurationSettingEntry(MINIMUM_DISHARGE_LEVEL, DataType.INTEGER, false));
		configurationSettings.put(BatteryConfigurationSetting.TIME_OF_USE_SCHEDULE,
				new ConfigurationSettingEntry(TIME_OF_USE_SCHEDULE, DataType.STRING, false));
		configurationSettings.put(BatteryConfigurationSetting.OPERATING_MODE,
				new ConfigurationSettingEntry(OPERATING_MODE, DataType.STRING, false));
		log.info("Configued settings are " + configurationSettings.keySet());
	}

	private void setupOperatingModes() {
		operatingModes.put("Manual", "1");
		operatingModes.put("Automatic - Self-Consumption", "2");
		operatingModes.put("Battery-Module-Extension (30%)", "6");
		operatingModes.put("Time-Of-Use", "10");
	}

	@Override
	public JsonObject retrieveStatus() throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException {
		log.fine("Getting status");
		return battery.getStatus();
	}

	@Override
	public JsonArray retrievePowerMeter() throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException {
		log.fine("Getting power meter");
		return battery.getPowerMeter();
	}

	@Override
	public JsonObject retrieveLatestData() throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException {
		log.fine("Getting latest data");
		return battery.getLatestData();
	}

	@Override
	public int retrieveBatteryReserveLevel()
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, BatteryProgrammingProblemException {
		try {
			return getConfiguration(BatteryConfigurationSetting.RESERVE_LEVEL).integerValue();
		} catch (DataItemIncorrectIntegerFormatException | DataItemIncorrectBooleanFormatException
				| DataItemIncorrectTypeException | DataItemNotFoundException e) {
			throw new BatteryProgrammingProblemException(
					"Problem retrieving the battery reserve level " + e.getLocalizedMessage(), e);
		}
	}

	@Override
	public int applyBatteryReserveLevel(int reserveLevel)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, BatteryProgrammingProblemException, BatteryReadOnlySettingException {
		int oldLevel = retrieveBatteryReserveLevel();
		try {
			updateConfiguration(BatteryConfigurationSetting.RESERVE_LEVEL, DataItem.asInteger(reserveLevel));
			return oldLevel;
		} catch (DataItemIncorrectIntegerFormatException | DataItemIncorrectBooleanFormatException
				| DataItemIncorrectTypeException | DataItemNotFoundException e) {
			throw new BatteryProgrammingProblemException(
					"Problem updating the battery reserve level " + e.getLocalizedMessage(), e);
		}
	}

	public String retrieveTimeOfUseSchedule() throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {
		return getConfigurationItemAsString(TIME_OF_USE_SCHEDULE);
	}

	@Override
	public int retrieveCurrentChargeLevel() throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException, BatteryUnsupportedOperationException {
		return retrieveLatestData().getInt(CURRENT_CHARGE_LEVEL);
	}

	@Override
	public String setConfiguration(String settingName, String settingValue) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {
		log.info("Setting string configuration " + settingName + " to " + settingValue);
		String oldValue = battery.getConfiguration(settingName).getString(settingName);
		String setPayload = "{\"" + settingName + "\":\"" + settingValue + "\"}";
		log.info("Setting string configuration old value for " + settingName + " was " + settingValue + " set data is "
				+ setPayload);
		String newValue = battery.setConfiguration(setPayload).getString(settingName);
		log.info("Set " + settingName + " to " + settingValue + " old value is " + oldValue
				+ " value returned from set is " + newValue);
		return oldValue;
	}

	@Override
	public Integer setConfiguration(String settingName, Integer settingValue) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {

		log.info("Setting integer configuration " + settingName + " to " + settingValue);
		int oldValue = Integer.parseInt(
				battery.getConfiguration(settingName).get(settingName).toString().replaceAll(STRING_QUOTE, "").trim());
		String setPayload = "{\"" + settingName + "\":\"" + settingValue + "\"}";
		log.info("Setting int configuration old value for " + settingName + " was " + settingValue + " set data is "
				+ setPayload);
		int newValue = Integer.parseInt(
				battery.setConfiguration(setPayload).get(settingName).toString().replaceAll(STRING_QUOTE, "").trim());
		log.info("Set " + settingName + " to " + settingValue + " old value is " + oldValue
				+ " value returned from set is " + newValue);
		return oldValue;
	}

	@Override
	public Boolean setConfiguration(String settingName, Boolean settingValue) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {

		log.fine("Setting boolean configuration " + settingName + " to " + settingValue);
		boolean oldValue = battery.getConfiguration(settingName).getBoolean(settingName);
		String setPayload = "{\"" + settingName + "\":\"" + settingValue + "\"}";
		log.info("Setting boolean configuration old value for " + settingName + " was " + settingValue + " set data is "
				+ setPayload);
		boolean newValue = battery.setConfiguration(setPayload).getBoolean(settingName);

		log.fine("Set " + settingName + " to " + settingValue + " old value is " + oldValue
				+ " value returned from set is " + newValue);
		return oldValue;
	}

	public JsonObject getConfigurationItemAsJson(String settingName) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {
		log.fine("Getting configuration item " + settingName);
		return battery.getConfiguration(settingName);
	}

	@Override
	public String getConfigurationItemAsString(String settingName) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {
		JsonValue settingValue = battery.getConfiguration(settingName).get(settingName);
		if (settingValue == null) {
			throw new BatteryUnknownSettingException("Unable to locate setting " + settingName);
		}
		String setting = settingValue.toString();
		// remove and leading or trailing quotes
		if (setting.startsWith(STRING_QUOTE)) {
			setting = setting.substring(STRING_QUOTE.length());
		}
		if (setting.endsWith(STRING_QUOTE)) {
			setting = setting.substring(0, setting.length() - STRING_QUOTE.length());
		}
		log.fine("Setting " + settingName + " (String) has value " + setting);
		return setting;
	}

	@Override
	public Integer getConfigurationItemAsInteger(String settingName) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {
		Integer setting = Integer.parseInt(getConfigurationItemAsString(settingName));
		log.fine("Setting " + settingName + " (Integer) has value " + setting);
		return setting;
	}

	@Override
	public Boolean getConfigurationItemAsBoolean(String settingName) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {
		Boolean setting = Boolean.parseBoolean(getConfigurationItemAsString(settingName));
		log.fine("Setting " + settingName + " (Boolean) has value " + setting);
		return setting;
	}

	@Override
	public int retrieveMaximumChargeLevel() throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException {
		return MAXIMUM_CHARGE_LEVEL;
	}

	private String convertTOUSchedule(ChargeTimeSetting chargeTimeSetting) {
		return chargeTimeSetting.toString();
	}

	private ChargeTimeSetting convertTOUSchedule(String chargeTimeSetting) {
		return null;
	}

	@Override
	public boolean systemBattery() {
		return false;
	}

	@Override
	public Collection<String> getOperatingModes() {
		return Collections.unmodifiableCollection(operatingModes.keySet());
	}

	@Override
	@JsonIgnore
	public String getOperatingMode() throws BatteryUnsupportedOperationException, BatteryUnauthorisedAccessException,
			BatteryUnexpectedResponseException {
		try {
			String mode = getConfigurationItemAsString(BatteryConfigurationSetting.OPERATING_MODE);
			Optional<String> firstModeFound = operatingModes.entrySet().stream()
					.filter(entry -> entry.getValue().equals(mode)).map(entry -> entry.getKey()).findFirst();
			if (firstModeFound.isEmpty()) {
				throw new BatteryUnexpectedResponseException(
						"Configuration for operating mode returned mode " + mode + " which is unknown");
			} else {
				return firstModeFound.get();
			}
		} catch (BatteryUnknownSettingException | BatteryInvalidParameterException e) {
			throw new BatteryUnsupportedOperationException("Can't get the battey mode");
		}
	}

	@Override
	public String setOperatingMode(String modeName)
			throws BatteryInvalidParameterException, BatteryUnsupportedOperationException,
			BatteryUnauthorisedAccessException, BatteryUnexpectedResponseException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, BatteryReadOnlySettingException {
		log.info("Sonnen Battery setting operating mode to " + modeName);
		if (operatingModes.containsKey(modeName)) {
			String mode = operatingModes.get(modeName);
			log.info("Sonnen Battery battery mode for battery is " + mode);
			String oldMode = getOperatingMode();
			try {
				log.info("Sonnen Battery calling update configuration to set mode to " + mode);
				updateConfiguration(BatteryConfigurationSetting.OPERATING_MODE, DataItem.asString(mode));
			} catch (BatteryUnknownSettingException e) {
				throw new BatteryUnsupportedOperationException("battery doesn't know how to set " + OPERATING_MODE);
			}
			return oldMode;
		} else {
			log.warning("Cannot locate mode " + modeName + " to set sonnen battery");
			throw new BatteryInvalidParameterException(
					"Operating mode specified (" + modeName + ") is not a supported option");
		}
	}

}
