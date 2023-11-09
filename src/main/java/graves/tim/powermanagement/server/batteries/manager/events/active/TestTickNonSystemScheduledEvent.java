package graves.tim.powermanagement.server.batteries.manager.events.active;

import graves.tim.powermanagement.common.data.BatteryEvent;
import graves.tim.powermanagement.common.data.DataItemException;
import graves.tim.powermanagement.common.data.TestTickEvent;
import graves.tim.powermanagement.common.exceptions.BatteryEventInPastException;
import graves.tim.powermanagement.common.exceptions.BatteryEventNoRepeatException;
import graves.tim.powermanagement.common.exceptions.BatteryEventRepeatCompletedException;
import graves.tim.powermanagement.server.batteries.manager.BatteryData;
import graves.tim.powermanagement.server.batteries.manager.events.BatteryIndividualSchedulableEventScheduler;
import graves.tim.powermanagement.server.batteries.manager.events.core.BatteryIndividualSchedulableEvent;
import graves.tim.powermanagement.server.batteries.manager.events.core.BatterySchedulableEvent;
import lombok.extern.java.Log;

@Log
public class TestTickNonSystemScheduledEvent extends BatterySchedulableEvent {
	public TestTickNonSystemScheduledEvent(BatteryData batteryData, BatteryEvent event) {
		super(batteryData, event);
	}

	@Override
	public boolean isSystemBatteryEvent() {
		return true;
	}

	@Override
	public void schedule(BatteryIndividualSchedulableEventScheduler batteryIndividualSchedulableEventScheduler)
			throws BatteryEventInPastException {
		TestTickIndividualScheduledEvent testTickIndividualScheduledEvent = new TestTickIndividualScheduledEvent(
				getBatteryData(), this);
		try {
			log.info("Scheduling ticker on non system battery " + getBatteryData().getBatteryName()
					+ " using message string " + getBatteryEvent().stringValue(TestTickEvent.MESSAGE)
					+ " targeting event battery  " + getBatteryEvent().getBatteryName());
		} catch (DataItemException e) {
			log.severe("Unable to get message for test tick event because " + e.getLocalizedMessage());
		}
		super.scheduleStartTimeFromPlanned(testTickIndividualScheduledEvent,
				batteryIndividualSchedulableEventScheduler);
	}

	@Override
	public void reschedule(BatteryIndividualSchedulableEvent batteryIndividualSchedulableEvent,
			BatteryIndividualSchedulableEventScheduler batteryIndividualSchedulableEventScheduler)
			throws BatteryEventInPastException, BatteryEventRepeatCompletedException, BatteryEventNoRepeatException {

		TestTickIndividualScheduledEvent testTickIndividualScheduledEvent = new TestTickIndividualScheduledEvent(
				batteryIndividualSchedulableEvent);
		try {
			log.info("Rescheduling ticker on battery "
					+ batteryIndividualSchedulableEvent.getParentEvent().getBatteryData().getBatteryName()
					+ " using message string "
					+ batteryIndividualSchedulableEvent.getParentEvent().getBatteryEvent()
							.stringValue(TestTickEvent.MESSAGE)
					+ " targeting event battery  "
					+ batteryIndividualSchedulableEvent.getParentEvent().getBatteryEvent().getBatteryName());
		} catch (DataItemException e) {
			log.severe("Unable to get message for test tick event because " + e.getLocalizedMessage());
		}
		super.rescheduleStartTimeFromPlanned(testTickIndividualScheduledEvent,
				batteryIndividualSchedulableEventScheduler);
	}

}
