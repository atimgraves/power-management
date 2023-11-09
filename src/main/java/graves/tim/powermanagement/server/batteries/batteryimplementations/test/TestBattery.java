package graves.tim.powermanagement.server.batteries.batteryimplementations.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import graves.tim.powermanagement.common.exceptions.BatteryReadOnlySettingException;
import graves.tim.powermanagement.common.exceptions.BatteryUnauthorisedAccessException;
import graves.tim.powermanagement.common.exceptions.BatteryUnexpectedResponseException;
import graves.tim.powermanagement.common.exceptions.BatteryUnknownSettingException;
import graves.tim.powermanagement.common.exceptions.BatteryUnsupportedOperationException;
import graves.tim.powermanagement.server.batteries.GenericBattery;
import graves.tim.powermanagement.server.batteries.batteryimplementations.ConfigurationSettingEntry;
import graves.tim.powermanagement.server.batteries.manager.BatteryData;
import graves.tim.powermanagement.server.connectionsupport.AuthException;
import io.helidon.config.Config;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
public class TestBattery implements GenericBattery {
	@Setter
	@Getter
	private BatteryData batteryData;
	private static final int MAXIMUM_CHARGE_LEVEL = 100;
	private final static String MINIMUM_DISHARGE_LEVEL = "EM_USOC";
	private final static String TIME_OF_USE_SCHEDULE = "EM_ToU_Schedule";
	private Map<String, String> savedData = new HashMap<>();
	private int currentCharge = 100; // this is for simulation purposes
	private int SECS_PER_CHARGE_UNIT = 10; // this is for simulation purposes
	private long startTime = System.currentTimeMillis();
	private static Map<String, String> operatingModes;
	private List<ChargeTimeSetting> chargeTimeSettings = new ArrayList<>(1);
	private String currentOperatingMode = "Mode 1"; // must match one of the values added to the operating modes map

	public TestBattery(String batteryConfigName, Config batteryConfig) throws BatteryException, AuthException {
		savedData.put(MINIMUM_DISHARGE_LEVEL, "30");
		savedData.put(TIME_OF_USE_SCHEDULE, "\"{\"TimeOfUse\":\"There is one\"}");
		ChargeTimeSetting chargeTimeSetting = ChargeTimeSetting.builder().allowCharging(true).maxExternalPower(12000)
				.startTime(System.currentTimeMillis()).endTime(System.currentTimeMillis() + 1).build();
		chargeTimeSettings.add(chargeTimeSetting);
		configurationSettings.put(BatteryConfigurationSetting.RESERVE_LEVEL,
				new ConfigurationSettingEntry(MINIMUM_DISHARGE_LEVEL, DataType.INTEGER, false));
		configurationSettings.put(BatteryConfigurationSetting.TIME_OF_USE_SCHEDULE,
				new ConfigurationSettingEntry(TIME_OF_USE_SCHEDULE, DataType.STRING, false));
	}

	@Override
	public JsonObject retrieveStatus() throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("StatusSettingName", "StatusSettingValue");
		return builder.build();
	}

	@Override
	public JsonArray retrievePowerMeter() throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException {
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("PowerSettingName", "PowerSettingValue");
		arrayBuilder.add(builder);
		return arrayBuilder.build();
	}

	@Override
	public JsonObject retrieveLatestData() throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("SettingName", "SettingValue");
		return builder.build();
	}

	@Override
	public int retrieveBatteryReserveLevel() throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {
		return getConfigurationItemAsInteger(MINIMUM_DISHARGE_LEVEL);
	}

	@Override
	public int applyBatteryReserveLevel(int reserveLevel) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {
		int oldLevel = retrieveBatteryReserveLevel();
		setConfiguration(MINIMUM_DISHARGE_LEVEL, reserveLevel);
		return oldLevel;
	}

	public String setTimeOfUseSchedule(String schedule) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException, BatteryUnsupportedOperationException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, BatteryReadOnlySettingException {
		return updateConfiguration(BatteryConfigurationSetting.TIME_OF_USE_SCHEDULE, DataItem.asString(schedule))
				.stringValue();
	}

	public String retrieveTimeOfUseSchedule() throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {
		return getConfigurationItemAsString(TIME_OF_USE_SCHEDULE);
	}

	@Override
	public int retrieveCurrentChargeLevel() throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException {
		return 100 - ((int) ((System.currentTimeMillis() - startTime) / (1000 * SECS_PER_CHARGE_UNIT)));
	}

	@Override
	public String setConfiguration(String settingName, String settingValue) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {
		log.fine("Setting string configuration " + settingName + " to " + settingValue);
		savedData.put(settingName, settingValue);
		return "{\"" + settingName + "\":\"" + settingValue + "\"}";
	}

	@Override
	public Integer setConfiguration(String settingName, Integer settingValue) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {

		log.fine("Setting integer configuration " + settingName + " to " + settingValue);
		savedData.put(settingName, "" + settingValue);
		return settingValue;
	}

	@Override
	public Boolean setConfiguration(String settingName, Boolean settingValue) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {

		log.fine("Setting boolean configuration " + settingName + " to " + settingValue);

		savedData.put(settingName, "" + settingValue);
		return settingValue;
	}

	public JsonObject getConfigurationItemAsJson(String settingName) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {
		String settingValue = savedData.get(settingName);
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add(settingName, settingValue);
		return builder.build();
	}

	@Override
	public String getConfigurationItemAsString(String settingName) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {
		JsonValue settingValue = getConfigurationItemAsJson(settingName).get(settingName);
		if (settingValue == null) {
			throw new BatteryInvalidParameterException("Unable to locate setting " + settingName);
		}
		String setting = settingValue.toString();
		log.fine("Setting " + settingName + " (String) has value " + setting);
		return setting;
	}

	@Override
	public Integer getConfigurationItemAsInteger(String settingName) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {
		JsonValue settingValue = getConfigurationItemAsJson(settingName).get(settingName);
		if (settingValue == null) {
			throw new BatteryInvalidParameterException("Unable to locate setting " + settingName);
		}
		Integer setting = Integer.parseInt(settingValue.toString().replaceAll("\"", "").trim());
		log.fine("Setting " + settingName + " (Integer) has value " + setting);
		return setting;
	}

	@Override
	public Boolean getConfigurationItemAsBoolean(String settingName) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException {
		JsonValue settingValue = getConfigurationItemAsJson(settingName).get(settingName);
		if (settingValue == null) {
			throw new BatteryUnknownSettingException("Unable to locate setting " + settingName);
		}
		Boolean setting = getConfigurationItemAsJson(settingName).getBoolean(settingName);
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
	public Set<String> getOperatingModes() {
		if (operatingModes == null) {
			setupOperatingModes();
		}
		return operatingModes.keySet();
	}

	private static void setupOperatingModes() {
		operatingModes = new HashMap<>();
		operatingModes.put("Mode 1", "M1");
		operatingModes.put("Mode 4", "M4");
		operatingModes.put("Mode 7", "M7");
		operatingModes.put("Mode 9", "M9-a");
	}

	@Override
	public String getOperatingMode() throws BatteryUnsupportedOperationException, BatteryUnauthorisedAccessException,
			BatteryUnexpectedResponseException {
		return currentOperatingMode;
	}

	@Override
	public String setOperatingMode(String modeName)
			throws BatteryInvalidParameterException, BatteryUnsupportedOperationException,
			BatteryUnauthorisedAccessException, BatteryUnexpectedResponseException {
		if (operatingModes == null) {
			setupOperatingModes();
		}
		if (operatingModes.containsKey(modeName)) {
			String oldMode = currentOperatingMode;
			currentOperatingMode = modeName;
			return oldMode;
		} else {
			throw new BatteryInvalidParameterException(
					"Operating mode specified (" + modeName + ") is not a supported option");
		}
	}

}
