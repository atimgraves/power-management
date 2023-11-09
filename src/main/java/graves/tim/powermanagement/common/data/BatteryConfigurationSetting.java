package graves.tim.powermanagement.common.data;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

import graves.tim.powermanagement.common.exceptions.BatteryUnknownSettingException;
import lombok.Getter;

public enum BatteryConfigurationSetting {
	RESERVE_LEVEL("Reserve level"), TIME_OF_USE_SCHEDULE("Time of use schedule"), OPERATING_MODE("Operating mode"),
	CURRENT_CHARGE_LEVEL("Current charge level");

	private static Collection<String> settingsNames;
	@Getter
	private String publicName;

	private BatteryConfigurationSetting(String publicName) {
		this.publicName = publicName;
	}

	public static BatteryConfigurationSetting getByPublicName(String publicName) throws BatteryUnknownSettingException {
		for (BatteryConfigurationSetting value : values()) {
			if (value.getPublicName().equals(publicName)) {
				return value;
			}
		}
		throw new BatteryUnknownSettingException(
				"Public name of " + publicName + " does not match a known battery configuration setting");
	}

	public static Collection<String> getAvailableSettings() {
		if (settingsNames == null) {
			Collection<String> tmpNames = new TreeSet<>();
			for (BatteryConfigurationSetting value : values()) {
				tmpNames.add(value.getPublicName());
			}
			settingsNames = Collections.unmodifiableCollection(tmpNames);
		}
		return settingsNames;
	}
}
