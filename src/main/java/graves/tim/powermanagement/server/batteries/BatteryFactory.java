package graves.tim.powermanagement.server.batteries;

import graves.tim.powermanagement.common.exceptions.BatteryException;
import graves.tim.powermanagement.server.batteries.batteryimplementations.sonnen.SonnenV2Battery;
import graves.tim.powermanagement.server.batteries.batteryimplementations.test.TestBattery;
import graves.tim.powermanagement.server.connectionsupport.AuthException;
import io.helidon.config.Config;
import lombok.extern.java.Log;

@Log
public class BatteryFactory {
	public final static String BATTERY_CONFIG_TYPE_NAME = "type";
	public final static String BATTERY_TYPE_SONNEN_V2 = "SonnenV2";
	public final static String BATTERY_TYPE_TEST = "Test";

	public static GenericBattery build(String batteryConfigName, Config batteryConfig)
			throws BatteryException, AuthException {
		Config batteryConfigType = batteryConfig.get(BATTERY_CONFIG_TYPE_NAME);
		if (!batteryConfigType.exists()) {
			throw new BatteryException("Unable to load battery type for battery " + batteryConfigName);
		}
		String batteryConfigTypeName = batteryConfigType.asString().get();
		log.info("Battery " + batteryConfigName + " is type " + batteryConfigTypeName);
		if (batteryConfigTypeName.equalsIgnoreCase(BATTERY_TYPE_SONNEN_V2)) {
			return new SonnenV2Battery(batteryConfigName, batteryConfig);
		} else if (batteryConfigTypeName.equalsIgnoreCase(BATTERY_TYPE_TEST)) {
			return new TestBattery(batteryConfigName, batteryConfig);
		} else {
			throw new BatteryException(
					"Config " + batteryConfig + " has unknown battery type of " + batteryConfigTypeName);
		}
	}

}
