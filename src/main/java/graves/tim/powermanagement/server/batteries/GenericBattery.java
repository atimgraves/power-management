package graves.tim.powermanagement.server.batteries;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import graves.tim.powermanagement.common.data.BatteryConfigurationSetting;
import graves.tim.powermanagement.common.data.DataItem;
import graves.tim.powermanagement.common.data.DataItemIncorrectBooleanFormatException;
import graves.tim.powermanagement.common.data.DataItemIncorrectIntegerFormatException;
import graves.tim.powermanagement.common.data.DataItemIncorrectTypeException;
import graves.tim.powermanagement.common.data.DataItemNotFoundException;
import graves.tim.powermanagement.common.data.DataType;
import graves.tim.powermanagement.common.exceptions.BatteryInvalidParameterException;
import graves.tim.powermanagement.common.exceptions.BatteryProgrammingProblemException;
import graves.tim.powermanagement.common.exceptions.BatteryReadOnlySettingException;
import graves.tim.powermanagement.common.exceptions.BatteryUnauthorisedAccessException;
import graves.tim.powermanagement.common.exceptions.BatteryUnexpectedResponseException;
import graves.tim.powermanagement.common.exceptions.BatteryUnknownSettingException;
import graves.tim.powermanagement.common.exceptions.BatteryUnsupportedOperationException;
import graves.tim.powermanagement.server.batteries.batteryimplementations.ConfigurationSettingEntry;
import graves.tim.powermanagement.server.batteries.manager.BatteryData;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

public interface GenericBattery {

	public static final String CONFIG_PREFIX_DELIM = "/";
	public static final String PREVIOUS_CONFIG_PREFIX = CONFIG_PREFIX_DELIM + "previous" + CONFIG_PREFIX_DELIM;
	public static final String SAVED_CONFIG_PREFIX = CONFIG_PREFIX_DELIM + "saved" + CONFIG_PREFIX_DELIM;
	public Map<BatteryConfigurationSetting, ConfigurationSettingEntry> configurationSettings = new HashMap<>();

	@JsonIgnore
	public void setBatteryData(BatteryData batteryData);

	@JsonIgnore
	public BatteryData getBatteryData();

	/**
	 * Gets the level of charge of the
	 * 
	 * @return
	 */

	public int retrieveCurrentChargeLevel() throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException, BatteryUnsupportedOperationException;

	public int retrieveMaximumChargeLevel() throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException, BatteryUnsupportedOperationException;

	public int retrieveBatteryReserveLevel()
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, BatteryProgrammingProblemException;

	public int applyBatteryReserveLevel(int reserveLevel)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, BatteryProgrammingProblemException, BatteryReadOnlySettingException;

	public JsonObject retrieveStatus() throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException, BatteryUnsupportedOperationException;

	public JsonObject retrieveLatestData() throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException, BatteryUnsupportedOperationException;

	public JsonArray retrievePowerMeter() throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException,
			BatteryUnknownSettingException, BatteryUnsupportedOperationException;

	@JsonIgnore
	public default Map<String, DataType> getBatteryConfigurationSettings() {
		return configurationSettings.entrySet().stream().collect(
				Collectors.toMap(entry -> entry.getKey().getPublicName(), entry -> entry.getValue().getDataType()));
	}

	@JsonIgnore
	public default String getPrivateConfigurationName(BatteryConfigurationSetting setting)
			throws BatteryUnknownSettingException {
		if (configurationSettings.containsKey(setting)) {
			return configurationSettings.get(setting).getPrivateName();
		}
		throw new BatteryUnknownSettingException("This battery does not support the setting " + setting
				+ " supported settings are " + configurationSettings.keySet());
	}

	@JsonIgnore
	public default DataType getPrivateConfigurationDataType(BatteryConfigurationSetting setting)
			throws BatteryUnknownSettingException {
		if (configurationSettings.containsKey(setting)) {
			return configurationSettings.get(setting).getDataType();
		}
		throw new BatteryUnknownSettingException("This battery does not support the setting " + setting);
	}

	@JsonIgnore
	public default boolean isPrivateConfigurationReadOnly(BatteryConfigurationSetting setting)
			throws BatteryUnknownSettingException {
		if (configurationSettings.containsKey(setting)) {
			return configurationSettings.get(setting).isReadOnly();
		}
		throw new BatteryUnknownSettingException("This battery does not support the setting " + setting);
	}

	public String setConfiguration(String settingName, String settingValue) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException, BatteryUnsupportedOperationException;

	public Integer setConfiguration(String settingName, Integer settingValue) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException, BatteryUnsupportedOperationException;

	public Boolean setConfiguration(String settingName, Boolean settingValue) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException, BatteryUnsupportedOperationException;

	public default DataItem setConfiguration(BatteryConfigurationSetting setting, DataItem dataItem)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, DataItemIncorrectIntegerFormatException,
			BatteryReadOnlySettingException {
		if (isPrivateConfigurationReadOnly(setting)) {
			throw new BatteryReadOnlySettingException("Setting " + setting.getPublicName()
					+ " is read only for battery " + getBatteryData().getBatteryName());
		}
		String privateName = getPrivateConfigurationName(setting);
		DataType dataType = getPrivateConfigurationDataType(setting);
		if (dataType != dataItem.getType()) {
			throw new BatteryInvalidParameterException("Setting " + setting + " is for data type " + dataType
					+ " but the data item you supplied is of type " + dataItem.getType());
		}
		DataItem oldValue;
		switch (dataType) {
		case BOOLEAN:
			oldValue = DataItem.asBoolean(setConfiguration(privateName, dataItem.booleanValue()));
			break;
		case INTEGER:
			oldValue = DataItem.asInteger(setConfiguration(privateName, dataItem.integerValue()));
			break;
		case STRING:
			oldValue = DataItem.asString(setConfiguration(privateName, dataItem.stringValue()));
			break;
		case INSTANT:
		case LONG:
		case NULLDATA:
		case ZONED_DATE_TIME:
		default:
			throw new BatteryInvalidParameterException("Setting " + setting + " want's to return an DataItem of type "
					+ dataType + " but that's not a currently supported configuration setting type");
		}
		getBatteryData().addDataItem(setting.getPublicName(), oldValue);
		return oldValue;
	}

	public default String setConfiguration(BatteryConfigurationSetting setting, String settingValue)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException {
		String privateName = getPrivateConfigurationName(setting);
		DataType dataType = getPrivateConfigurationDataType(setting);
		if (dataType != DataType.STRING) {
			throw new BatteryInvalidParameterException("BatteryConfigurationSetting " + setting + " requires type "
					+ dataType + " but it's been called it with a String");
		}
		return setConfiguration(privateName, settingValue);
	}

	public default Integer setConfiguration(BatteryConfigurationSetting setting, Integer settingValue)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException {
		String privateName = getPrivateConfigurationName(setting);
		DataType dataType = getPrivateConfigurationDataType(setting);
		if (dataType != DataType.INTEGER) {
			throw new BatteryInvalidParameterException("BatteryConfigurationSetting " + setting + " requires type "
					+ dataType + " but it's been called it with an Integer");
		}
		return setConfiguration(privateName, settingValue);
	}

	public default Boolean setConfiguration(BatteryConfigurationSetting setting, Boolean settingValue)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException {
		String privateName = getPrivateConfigurationName(setting);
		DataType dataType = getPrivateConfigurationDataType(setting);
		if (dataType != DataType.BOOLEAN) {
			throw new BatteryInvalidParameterException("BatteryConfigurationSetting " + setting + " requires type "
					+ dataType + " but it's been called it with a Boolean");
		}
		return setConfiguration(privateName, settingValue);
	}

	public String getConfigurationItemAsString(String settingName) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException, BatteryUnsupportedOperationException;

	public Integer getConfigurationItemAsInteger(String settingName) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException, BatteryUnsupportedOperationException;

	public Boolean getConfigurationItemAsBoolean(String settingName) throws BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnknownSettingException, BatteryUnsupportedOperationException;

	public default DataItem getConfiguration(BatteryConfigurationSetting setting)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, DataItemIncorrectIntegerFormatException {
		String privateName = getPrivateConfigurationName(setting);
		DataType dataType = getPrivateConfigurationDataType(setting);
		switch (dataType) {
		case BOOLEAN:
			return DataItem.asBoolean(getConfigurationItemAsBoolean(privateName));
		case INTEGER:
			return DataItem.asInteger(getConfigurationItemAsInteger(privateName));
		case STRING:
			return DataItem.asString(getConfigurationItemAsString(privateName));
		case INSTANT:
		case LONG:
		case NULLDATA:
		case ZONED_DATE_TIME:
		default:
			throw new BatteryInvalidParameterException("Setting " + setting + " want's to return an DataItem of type "
					+ dataType + " but that's not a currently supported configuration setting type");
		}
	}

	public default DataItem saveConfigurationToSavedConfiguration(BatteryConfigurationSetting setting, String saveName)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectIntegerFormatException, DataItemIncorrectTypeException, DataItemNotFoundException {
		String name = setting.getPublicName();
		String settingSaveName = getSavedConfigSettingName(name, saveName);
		DataItem current = getConfiguration(setting);
		getBatteryData().addDataItem(settingSaveName, current);
		return current;
	}

	public default Boolean savedConfigurationExists(BatteryConfigurationSetting setting, String saveName)
			throws BatteryUnknownSettingException {
		String name = setting.getPublicName();
		String settingSaveName = getSavedConfigSettingName(name, saveName);
		return getBatteryData().hasDataItem(settingSaveName);
	}

	public default Boolean previousConfigurationExists(BatteryConfigurationSetting setting)
			throws BatteryUnknownSettingException {
		String name = setting.getPublicName();
		String previousSaveName = getPreviousConfigSettingName(name);
		return getBatteryData().hasDataItem(previousSaveName);
	}

	public default String getConfigurationItemAsString(BatteryConfigurationSetting setting)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException {
		String name = getPrivateConfigurationName(setting);
		DataType dataType = getPrivateConfigurationDataType(setting);
		if (dataType != DataType.STRING) {
			throw new BatteryInvalidParameterException("BatteryConfigurationSetting " + setting + " returns type "
					+ dataType + " but it's been called asking for a String");
		}
		return getConfigurationItemAsString(name);
	}

	public default Integer getConfigurationItemAsInteger(BatteryConfigurationSetting setting)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException {
		String name = getPrivateConfigurationName(setting);
		DataType dataType = getPrivateConfigurationDataType(setting);
		if (dataType != DataType.INTEGER) {
			throw new BatteryInvalidParameterException("BatteryConfigurationSetting " + setting + " returns type "
					+ dataType + " but it's been called asking for an Integer");
		}
		return getConfigurationItemAsInteger(name);
	}

	public default Boolean getConfigurationItemAsBoolean(BatteryConfigurationSetting setting)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException {
		String name = getPrivateConfigurationName(setting);
		DataType dataType = getPrivateConfigurationDataType(setting);
		if (dataType != DataType.BOOLEAN) {
			throw new BatteryInvalidParameterException("BatteryConfigurationSetting " + setting + " returns type "
					+ dataType + " but it's been called askoing for a Boolean");
		}
		return getConfigurationItemAsBoolean(name);
	}

	public boolean systemBattery();

	@JsonIgnore
	public Collection<String> getOperatingModes();

	@JsonIgnore
	public String getOperatingMode() throws BatteryUnsupportedOperationException, BatteryUnauthorisedAccessException,
			BatteryUnexpectedResponseException;

	@JsonIgnore
	public String setOperatingMode(String modeName)
			throws BatteryInvalidParameterException, BatteryUnsupportedOperationException,
			BatteryUnauthorisedAccessException, BatteryUnexpectedResponseException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, BatteryReadOnlySettingException;

	public default Collection<BatteryConfigurationSetting> listSupportedConfigurationSettings() {
		return Collections.unmodifiableCollection(configurationSettings.keySet());
	}

	public default DataItem updateConfiguration(BatteryConfigurationSetting setting, DataItem value)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectIntegerFormatException, DataItemIncorrectTypeException, DataItemNotFoundException,
			BatteryReadOnlySettingException {
		String name = setting.getPublicName();
		DataItem previousValue = setConfiguration(setting, value);
		String previousDataItemName = getPreviousConfigSettingName(name);
		getBatteryData().addDataItem(previousDataItemName, previousValue);
		return previousValue;
	}

	public default DataItem restoreSavedConfiguration(BatteryConfigurationSetting setting, String saveName)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, DataItemIncorrectIntegerFormatException,
			BatteryReadOnlySettingException {
		String name = setting.getPublicName();
		String previousDataItemName = getSavedConfigSettingName(name, saveName);
		DataItem previousSetting = getBatteryData().dataItem(previousDataItemName);
		return updateConfiguration(setting, previousSetting);
	}

	public default DataItem restorePreviousConfiguration(BatteryConfigurationSetting setting)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, DataItemIncorrectIntegerFormatException,
			BatteryReadOnlySettingException {
		String name = setting.getPublicName();
		String previousDataItemName = getPreviousConfigSettingName(name);
		DataItem previousSetting = getBatteryData().dataItem(previousDataItemName);
		return updateConfiguration(setting, previousSetting);
	}

	public default DataItem removeSavedConfiguration(BatteryConfigurationSetting setting, String saveName)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, DataItemIncorrectIntegerFormatException {
		String name = setting.getPublicName();
		String previousDataItemName = getSavedConfigSettingName(name, saveName);
		return getBatteryData().removeDataItem(previousDataItemName);

	}

	public default DataItem removePreviousConfiguration(BatteryConfigurationSetting setting)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, DataItemIncorrectIntegerFormatException {
		String name = setting.getPublicName();
		String previousDataItemName = getPreviousConfigSettingName(name);
		return getBatteryData().removeDataItem(previousDataItemName);

	}

	public default DataItem getSavedConfiguration(BatteryConfigurationSetting setting, String saveName)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, DataItemIncorrectIntegerFormatException {
		String name = setting.getPublicName();
		String previousDataItemName = getSavedConfigSettingName(name, saveName);
		return getBatteryData().dataItem(previousDataItemName);
	}

	public default DataItem getPreviousConfiguration(BatteryConfigurationSetting setting)
			throws BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, DataItemIncorrectIntegerFormatException {
		String name = setting.getPublicName();
		String previousDataItemName = getPreviousConfigSettingName(name);
		return getBatteryData().dataItem(previousDataItemName);
	}

	public default Collection<String> listSavedConfigSettingNames() {
		return getBatteryData().dataItemNamesStartingWith(SAVED_CONFIG_PREFIX);
	}

	public default Collection<String> listSavedConfigSettingNames(BatteryConfigurationSetting setting)
			throws BatteryUnknownSettingException {
		String name = setting.getPublicName();
		return getBatteryData().dataItemNamesStartingWith(getSavedConfigSettingPrefix(name));
	}

	public static String getSavedConfigSettingName(String settingName, String saveName) {
		return getSavedConfigSettingPrefix(settingName) + saveName;
	}

	public static String getSavedConfigSettingPrefix(String settingName) {
		return SAVED_CONFIG_PREFIX + settingName + CONFIG_PREFIX_DELIM;
	}

	public default Collection<String> listPreviousConfigSettingNames() {
		return getBatteryData().dataItemNamesStartingWith(getPreviousConfigSettingPrefix());
	}

	public static String getPreviousConfigSettingName(String settingName) {
		return getPreviousConfigSettingPrefix() + settingName;
	}

	public static String getPreviousConfigSettingPrefix() {
		return PREVIOUS_CONFIG_PREFIX;
	}
}
