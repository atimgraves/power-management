package graves.tim.powermanagement.server.batteries.manager.events.active;

import java.time.ZonedDateTime;

import graves.tim.powermanagement.common.data.DataItemIncorrectTypeException;
import graves.tim.powermanagement.common.data.DataItemNotFoundException;
import graves.tim.powermanagement.common.data.TestTickEvent;
import graves.tim.powermanagement.common.exceptions.BatteryEventInPastException;
import graves.tim.powermanagement.common.exceptions.BatteryEventNoRepeatException;
import graves.tim.powermanagement.common.exceptions.BatteryEventRepeatCompletedException;
import graves.tim.powermanagement.server.batteries.manager.BatteryData;
import graves.tim.powermanagement.server.batteries.manager.BatteryManager;
import graves.tim.powermanagement.server.batteries.manager.events.BatteryIndividualSchedulableEventScheduler;
import graves.tim.powermanagement.server.batteries.manager.events.BatteryIndividualSchedulableEventType;
import graves.tim.powermanagement.server.batteries.manager.events.core.BatteryIndividualSchedulableEvent;
import graves.tim.powermanagement.server.batteries.manager.events.core.BatterySchedulableEvent;
import lombok.extern.java.Log;

@Log
public class TestTickIndividualScheduledEvent extends BatteryIndividualSchedulableEvent {

	public TestTickIndividualScheduledEvent(BatteryIndividualSchedulableEvent batteryIndividualSchedulableEvent) {
		super(batteryIndividualSchedulableEvent);
	}

	public TestTickIndividualScheduledEvent(BatteryData batteryData, BatterySchedulableEvent batterySchedulableEvent) {
		super(BatteryIndividualSchedulableEventType.TEST_TICK, batteryData, batterySchedulableEvent);
	}

	@Override
	public boolean executeBatteryIndividualSchedulableEvent(BatteryManager batteryManager,
			BatteryIndividualSchedulableEventScheduler batteryIndividualSchedulableEventScheduler)
			throws DataItemIncorrectTypeException, DataItemNotFoundException {
		log.info("Executing Ticker on battery " + getBatteryData().getBatteryName() + " with message "
				+ getParentEvent().getBatteryEvent().stringValue(TestTickEvent.MESSAGE));
		// get the parent schedulable event to run
		try {
			getParentEvent().reschedule(this, batteryIndividualSchedulableEventScheduler);
			return false;
		} catch (BatteryEventRepeatCompletedException e) {
			log.info("Repeat completed");
		} catch (BatteryEventNoRepeatException e) {
			log.info("No Repeat");
		} catch (BatteryEventInPastException e) {
			log.severe("Event was scheduled into the past which should not happen, scheduled time was "
					+ dateTimeFormatter.format(ZonedDateTime.now()));
		}
		// return true to remove the parent event from the scheduler
		return true;
	}

}
