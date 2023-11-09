package graves.tim.powermanagement.server.batteries.batteryimplementations.system;

import java.util.Set;
import java.util.TreeSet;

import graves.tim.powermanagement.common.exceptions.BatteryInvalidParameterException;
import graves.tim.powermanagement.common.exceptions.BatteryUnauthorisedAccessException;
import graves.tim.powermanagement.common.exceptions.BatteryUnexpectedResponseException;
import graves.tim.powermanagement.common.exceptions.BatteryUnsupportedOperationException;
import graves.tim.powermanagement.server.batteries.GenericBattery;
import graves.tim.powermanagement.server.batteries.manager.BatteryData;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
@NoArgsConstructor
public class SystemBattery implements GenericBattery {

	@Setter
	@Getter
	private BatteryData batteryData;

	@Override
	public JsonObject retrieveStatus() throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	@Override
	public JsonArray retrievePowerMeter() throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	@Override
	public JsonObject retrieveLatestData() throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	@Override
	public int retrieveBatteryReserveLevel() throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	@Override
	public int applyBatteryReserveLevel(int reserveLevel) throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	public String setTimeOfUseSchedule(String schedule) throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	public String retrieveTimeOfUseSchedule() throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	@Override
	public int retrieveCurrentChargeLevel() throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	@Override
	public String setConfiguration(String settingName, String settingValue)
			throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	@Override
	public Integer setConfiguration(String settingName, Integer settingValue)
			throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	@Override
	public Boolean setConfiguration(String settingName, Boolean settingValue)
			throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	public JsonObject getConfigurationItemAsJson(String settingName) throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	@Override
	public String getConfigurationItemAsString(String settingName) throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	@Override
	public Integer getConfigurationItemAsInteger(String settingName) throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	@Override
	public Boolean getConfigurationItemAsBoolean(String settingName) throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	@Override
	public int retrieveMaximumChargeLevel() throws BatteryUnsupportedOperationException {
		throw new BatteryUnsupportedOperationException("System battery does not support this operation");
	}

	@Override
	public boolean systemBattery() {
		return true;
	}

	@Override
	public Set<String> getOperatingModes() {
		return new TreeSet<>();
	}

	@Override
	public String getOperatingMode() throws BatteryUnsupportedOperationException,
			BatteryUnauthorisedAccessException, BatteryUnexpectedResponseException {
		throw new BatteryUnsupportedOperationException("System batteries do not support operating modes");
	}

	@Override
	public String setOperatingMode(String modeName)
			throws BatteryInvalidParameterException, BatteryUnsupportedOperationException,
			BatteryUnauthorisedAccessException, BatteryUnexpectedResponseException {
		throw new BatteryUnsupportedOperationException("System batteries do not support operating modes");
	}

}
