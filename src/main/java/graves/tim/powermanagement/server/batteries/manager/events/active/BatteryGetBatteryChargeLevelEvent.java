package graves.tim.powermanagement.server.batteries.manager.events.active;

import graves.tim.powermanagement.common.exceptions.NotYetImplementedException;
import graves.tim.powermanagement.server.batteries.manager.BatteryData;
import graves.tim.powermanagement.server.batteries.manager.BatteryManager;
import graves.tim.powermanagement.server.batteries.manager.events.BatteryIndividualSchedulableEventScheduler;
import graves.tim.powermanagement.server.batteries.manager.events.BatteryIndividualSchedulableEventType;
import graves.tim.powermanagement.server.batteries.manager.events.core.BatteryIndividualSchedulableEvent;
import graves.tim.powermanagement.server.batteries.manager.events.core.BatterySchedulableEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.java.Log;

@Log
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class BatteryGetBatteryChargeLevelEvent extends BatteryIndividualSchedulableEvent {
	public BatteryGetBatteryChargeLevelEvent(BatteryIndividualSchedulableEvent batteryIndividualSchedulableEvent) {
		super(batteryIndividualSchedulableEvent);
	}

	public BatteryGetBatteryChargeLevelEvent(BatteryData batteryData,
			BatterySchedulableEvent parentBatteryScheduledEvent) {
		super(BatteryIndividualSchedulableEventType.GET_BATTERY_CHARGE_LEVEL, batteryData, parentBatteryScheduledEvent);
	}

	@Override
	public boolean executeBatteryIndividualSchedulableEvent(BatteryManager batteryManager,
			BatteryIndividualSchedulableEventScheduler batteryIndividualSchedulableEventScheduler) {
		throw new NotYetImplementedException();
	}
}
