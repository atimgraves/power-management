package graves.tim.powermanagement.server.batteries.manager.events.active;

import graves.tim.powermanagement.common.data.BatteryEvent;
import graves.tim.powermanagement.common.exceptions.BatteryEventEventTypeRequiresSystemBattery;
import graves.tim.powermanagement.common.exceptions.BatteryEventEventTypeUnsupportedOnSystemBatteryException;
import graves.tim.powermanagement.common.exceptions.BatteryEventNotYetSupportedEventTypeException;
import graves.tim.powermanagement.common.exceptions.BatteryEventUnimplementedEventTypeException;
import graves.tim.powermanagement.common.exceptions.BatteryEventUnknownEventTypeException;
import graves.tim.powermanagement.common.exceptions.BatteryNotFoundException;
import graves.tim.powermanagement.server.batteries.manager.BatteryData;
import graves.tim.powermanagement.server.batteries.manager.BatteryManager;
import graves.tim.powermanagement.server.batteries.manager.events.core.BatterySchedulableEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class BatterySchedulableEventFactory {
	@Inject
	private BatteryManager batteryManager;

	public BatterySchedulableEvent buildEvent(BatteryEvent event)
			throws BatteryEventUnknownEventTypeException, BatteryEventUnimplementedEventTypeException,
			BatteryEventEventTypeRequiresSystemBattery, BatteryEventEventTypeUnsupportedOnSystemBatteryException,
			BatteryNotFoundException, BatteryEventNotYetSupportedEventTypeException {
		BatteryData batteryData = batteryManager.getBatteryDataByName(event.getBatteryName());
		BatterySchedulableEvent bse = null;
		switch (event.getEventType()) {
		case TEST_TICK_SYSTEM:
			bse = new TestTickSystemScheduledEvent(batteryData, event);
			break;
		case TEST_TICK_NON_SYSTEM:
			bse = new TestTickNonSystemScheduledEvent(batteryData, event);
			break;
		case GATHER_STATISTICS:
			break;
		case PLANNED_POWER_CUT:
			break;
		case POWER_SAVINGS_EVENT:
			break;
		case PULL_GRID_LOAD_IF_REASONABLE:
			break;
		case SET_RESERVE_POWER_LEVEL:
			bse = new BatterySetReserveLevelEvent(batteryData, event);
			break;
		case SET_RELATIVE_TO_CURRENT_LEVEL_RESERVE_LEVEL:
			bse = new BatterySetReserveLevelRelativeToCurrentLevelLevelEvent(batteryData, event);
			break;
		default:
			throw new BatteryEventUnknownEventTypeException("Provided event type is unknown for event " + event);
		}
		log.info("Battery event is " + bse);
		if (bse == null) {
			throw new BatteryEventUnimplementedEventTypeException(
					"Event is a known type, but there is no implemented processing class for it " + event);
		}
		// is the event targeted at a system battery ? If so is that allowed ?
		boolean typeRequiresSystemBattery = bse.isSystemBatteryEvent();
		boolean systemBattery = batteryData.systemBattery();
		if (typeRequiresSystemBattery && !systemBattery) {
			throw new BatteryEventEventTypeRequiresSystemBattery(
					"Event is only suppported on a system battery " + event);
		}
		if (!typeRequiresSystemBattery && systemBattery) {
			throw new BatteryEventEventTypeUnsupportedOnSystemBatteryException(
					"Event is not suppported on a system battery " + event);
		}
		return bse;
	}

}
