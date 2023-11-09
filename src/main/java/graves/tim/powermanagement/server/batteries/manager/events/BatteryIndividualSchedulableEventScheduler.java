package graves.tim.powermanagement.server.batteries.manager.events;

import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import graves.tim.powermanagement.common.exceptions.BatteryEventActiveChildTasksException;
import graves.tim.powermanagement.common.exceptions.BatteryEventInPastException;
import graves.tim.powermanagement.common.exceptions.BatteryEventOutstandingChildTasksException;
import graves.tim.powermanagement.server.batteries.manager.BatteryManager;
import graves.tim.powermanagement.server.batteries.manager.events.core.BatteryIndividualSchedulableEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.java.Log;

@Log
@ApplicationScoped
public class BatteryIndividualSchedulableEventScheduler {
	public final static int MINIMUM_SECONDS_BEFORE_RUNNING = 1;
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	@Inject
	private BatteryManager batteryManager;

	public void addFutureEvent(BatteryIndividualSchedulableEvent batteryIndividualSchedulableEvent,
			boolean executePastEvent) throws BatteryEventInPastException {
		ZonedDateTime now = ZonedDateTime.now();
		if (!executePastEvent) {
			if (batteryIndividualSchedulableEvent.getScheduledEventActivationTime().isBefore(now)) {
				throw new BatteryEventInPastException("Cannot schedule " + batteryIndividualSchedulableEvent
						+ " as it's activation time is in the past");
			}
		}
		// we will not schedule things immediataly, work out how far in the future this
		// event must be
		ZonedDateTime futureMinimum = now.plusSeconds(MINIMUM_SECONDS_BEFORE_RUNNING);
		ZonedDateTime scheduleTime = batteryIndividualSchedulableEvent.getScheduledEventActivationTime().isBefore(
				futureMinimum) ? futureMinimum : batteryIndividualSchedulableEvent.getScheduledEventActivationTime();
		// in how many seconds is this event ?
		long timeToNextEvent = scheduleTime.toEpochSecond() - now.toEpochSecond();
		ScheduledFuture<?> eventFuture = executor.schedule(() -> {
			// synchronize on the parent event to ensure that only one of it's tasks is
			// running at any point in time
			synchronized (batteryIndividualSchedulableEvent.getParentEvent()) {
				boolean removeParentBatteryEvent;
				batteryIndividualSchedulableEvent.startProcessing();
				try {
					removeParentBatteryEvent = batteryIndividualSchedulableEvent
							.executeBatteryIndividualSchedulableEvent(batteryManager, this);
				} catch (Exception e) {
					log.warning("Problem performing operation " + batteryIndividualSchedulableEvent + " problem is "
							+ e.getLocalizedMessage());
					e.printStackTrace();
					removeParentBatteryEvent = true;
				}
				// tell anything looking that we've stopped doing something in this specific
				// event
				batteryIndividualSchedulableEvent.stopProcessing();
				if (removeParentBatteryEvent) {
					try {
						// stop any child events, maybe the process has stopped part way through for
						// some valid reason.
						batteryIndividualSchedulableEvent.getParentEvent().clearScheduledChildEvents(false);
						batteryIndividualSchedulableEvent.getBatteryData()
								.removeEvent(batteryIndividualSchedulableEvent.getParentEvent().getBatteryEvent());
					} catch (BatteryEventOutstandingChildTasksException e) {
						log.severe("excuting " + batteryIndividualSchedulableEvent + " said to remove the parent event "
								+ batteryIndividualSchedulableEvent.getParentEvent()
								+ " however this failed as it has outstanding child tasks, probabaly programming error");
					} catch (BatteryEventActiveChildTasksException e) {
						log.severe("excuting " + batteryIndividualSchedulableEvent + " said to remove the parent event "
								+ batteryIndividualSchedulableEvent.getParentEvent()
								+ " however this failed as it has active child tasks, probabaly programming error");
						e.printStackTrace();
					}
				}
			}
		}, timeToNextEvent, TimeUnit.SECONDS);
		log.info("Scheduler has added lambda to run in " + timeToNextEvent + " seconds for event "
				+ batteryIndividualSchedulableEvent);
		batteryIndividualSchedulableEvent.setEventFuture(eventFuture);
	}
}
