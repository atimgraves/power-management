package graves.tim.powermanagement.server.batteries.manager.events;

import lombok.ToString;

@ToString
public enum BatteryIndividualSchedulableEventType {
	TEST_TICK, SET_RESERVE_LEVEL_ABSOLUTE, GET_BATTERY_CHARGE_LEVEL, SET_RESERVE_LEVEL_RELATIVE_TO_CURRENT_CHARGE_LEVEL, SAVE_POWER_INFO;

	public static BatteryIndividualSchedulableEventType getByName(String batteryEventTypeName) {
		for (BatteryIndividualSchedulableEventType batteryIndividualSchedulableEventType : BatteryIndividualSchedulableEventType
				.values()) {
			if (batteryIndividualSchedulableEventType.name().equals(batteryEventTypeName)) {
				return batteryIndividualSchedulableEventType;
			}
		}
		return null;
	}
}
