package graves.tim.powermanagement.server.batteries.manager.events.active;

import graves.tim.powermanagement.common.data.BatteryEvent;
import graves.tim.powermanagement.common.data.DataItemException;
import graves.tim.powermanagement.common.data.SetReserveLevelRelativeToCurrentLevelLevelEvent;
import graves.tim.powermanagement.common.exceptions.BatteryEventActiveChildTasksException;
import graves.tim.powermanagement.common.exceptions.BatteryEventInPastException;
import graves.tim.powermanagement.common.exceptions.BatteryEventNoRepeatException;
import graves.tim.powermanagement.common.exceptions.BatteryEventOutstandingChildTasksException;
import graves.tim.powermanagement.common.exceptions.BatteryEventRepeatCompletedException;
import graves.tim.powermanagement.server.batteries.manager.BatteryData;
import graves.tim.powermanagement.server.batteries.manager.events.BatteryIndividualSchedulableEventScheduler;
import graves.tim.powermanagement.server.batteries.manager.events.core.BatteryIndividualSchedulableEvent;
import graves.tim.powermanagement.server.batteries.manager.events.core.BatterySchedulableEvent;
import lombok.extern.java.Log;

@Log
public class BatterySetReserveLevelRelativeToCurrentLevelLevelEvent extends BatterySchedulableEvent {
	public BatterySetReserveLevelRelativeToCurrentLevelLevelEvent(BatteryData batteryData, BatteryEvent event) {
		super(batteryData, event);
	}

	@Override
	public boolean isSystemBatteryEvent() {
		return false;
	}

	public void deleteEvent() throws BatteryEventOutstandingChildTasksException, BatteryEventActiveChildTasksException {
		// we can just cancel any remaining tasks
		if (isEventInProgress()) {
			throw new BatteryEventActiveChildTasksException("Cannot delete, Child tasks are running");
		}
		// there are no active child tasks, and we're good to delete any that are
		// scheduled but nto active
		super.clearScheduledChildEvents(false);
		super.deleteEvent();
	}

	@Override
	public void schedule(BatteryIndividualSchedulableEventScheduler batteryIndividualSchedulableEventScheduler)
			throws BatteryEventInPastException {
		BatterySetReserveLevelRelativeToCurrentLevelIndividualEvent batterySetReserveLevelRelativeToCurrentLevelIndividualEvent = new BatterySetReserveLevelRelativeToCurrentLevelIndividualEvent(
				getBatteryData(), this);
		try {
			log.info("Scheduling set reserve level relative to current level on battery "
					+ getBatteryData().getBatteryName() + " using relative reserve "
					+ getBatteryEvent().integerValue(
							SetReserveLevelRelativeToCurrentLevelLevelEvent.RELATIVE_TO_CURRENT_LEVEL_RESERVE)
					+ " targeting event battery  " + getBatteryEvent().getBatteryName());
		} catch (DataItemException e) {
			log.severe("Unable to set reserve for BatterySetReserveLevelRelativeToCurrentLevelIndividualEvent because "
					+ e.getLocalizedMessage());
		}
		super.scheduleStartTimeFromPlanned(batterySetReserveLevelRelativeToCurrentLevelIndividualEvent,
				batteryIndividualSchedulableEventScheduler);
	}

	@Override
	public void reschedule(BatteryIndividualSchedulableEvent batteryIndividualSchedulableEvent,
			BatteryIndividualSchedulableEventScheduler batteryIndividualSchedulableEventScheduler)
			throws BatteryEventInPastException, BatteryEventRepeatCompletedException, BatteryEventNoRepeatException {

		try {
			log.info("Rescheduling set reserve level relative to current level on battery "
					+ getBatteryData().getBatteryName() + " using reserve "
					+ getBatteryEvent().integerValue(
							SetReserveLevelRelativeToCurrentLevelLevelEvent.RELATIVE_TO_CURRENT_LEVEL_RESERVE)
					+ " targeting event battery  " + getBatteryEvent().getBatteryName());
		} catch (DataItemException e) {
			log.severe(
					"Unable to set reserve for BatterySetReserveLevelRelativeToCurrentLevelIndividualEvent event because "
							+ e.getLocalizedMessage());
		}
		super.rescheduleStartTimeFromPlanned(batteryIndividualSchedulableEvent,
				batteryIndividualSchedulableEventScheduler);
	}

}
