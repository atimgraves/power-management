package graves.tim.powermanagement.server.batteries.manager.events.core;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import graves.tim.powermanagement.common.data.BatteryEvent;
import graves.tim.powermanagement.common.data.EventDataItems;
import graves.tim.powermanagement.common.data.RepeatRule;
import graves.tim.powermanagement.common.exceptions.BatteryEventActiveChildTasksException;
import graves.tim.powermanagement.common.exceptions.BatteryEventInPastException;
import graves.tim.powermanagement.common.exceptions.BatteryEventNoRepeatException;
import graves.tim.powermanagement.common.exceptions.BatteryEventOutstandingChildTasksException;
import graves.tim.powermanagement.common.exceptions.BatteryEventRepeatCompletedException;
import graves.tim.powermanagement.server.batteries.manager.BatteryData;
import graves.tim.powermanagement.server.batteries.manager.events.BatteryIndividualSchedulableEventScheduler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.java.Log;

@Log
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public abstract class BatterySchedulableEvent extends EventDataItems {
	public static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
	private Map<Long, BatteryIndividualSchedulableEvent> childEvents = new HashMap<>();
	private boolean eventInProgress = false; // when a child task starts this will be set to true, then reverted to
												// false when the child task is finished, this will stop the main task
												// from being dropped while there is an active child task
	@ToString.Exclude // we don't want a recursive ping pong between two strings
	@JsonIgnore
	private BatteryData batteryData;
	private BatteryEvent batteryEvent;

	public BatterySchedulableEvent(BatteryData batteryData, BatteryEvent batteryEvent) {
		this.batteryData = batteryData;
		this.batteryEvent = batteryEvent;

	}

	public BatteryData getBatteryData() {
		return batteryData;
	}

	// this is the basic version, super classes may do more.
	// the super class is responsible for removing this event from anywhere it may
	// be referenced
	public void deleteEvent() throws BatteryEventOutstandingChildTasksException, BatteryEventActiveChildTasksException {
		if (isEventInProgress()) {
			throw new BatteryEventActiveChildTasksException(
					"Can't delete this event " + this + " as it has an active child task");
		}
		if (hasChildTasksRemaining()) {
			throw new BatteryEventOutstandingChildTasksException(
					"Can't delete this event " + this + " as it has " + childEvents.size() + " remaining child tasks");
		}
	}

	public void clearScheduledChildEvents(boolean cancelActiveEvent) {
		childEvents.values().stream().forEach(event -> event.getEventFuture().cancel(cancelActiveEvent));
		childEvents.clear();
	}

	public boolean hasChildTasksRemaining() {
		// if this is a multi task event then it may have child tasks remaining to be
		// processed for example setting then re-setting power levels. In this case
		// the child tasks will need to run to completion or the sequence may end up
		// breaking the usage in practice this will really only be relevant for
		// modifications to the data, not retrieval
		return childEvents.size() > 0;
	}

	// flags that one of the child events is running, the scheduler will run the
	// actuall chile events
	// (including the stop / start) in a synchronized block on this to ensure that
	// only one task from any parent is running at a time
	public void processingChildEventStart(BatteryIndividualSchedulableEvent batteryIndividualSchedulableEvent) {
		setEventInProgress(true);
	}

	// flags that one of the child events is no longer running and then remove it
	// from the list of child events, the scheduler will run the actual child
	// events
	// (including the stop / start) in a synchronized block on this to ensure that
	// only one task from any parent is running at a time
	public void processingChildEventEnd(BatteryIndividualSchedulableEvent batteryIndividualSchedulableEvent) {
		setEventInProgress(false);
		synchronized (childEvents) {
			childEvents.remove(batteryIndividualSchedulableEvent.getEventId());
		}
	}

	// allows us to ensure that system wide events can only be applied to system
	// batteries and non system events will only be applied to real batteries
	public abstract boolean isSystemBatteryEvent();

	public void populateInitialSchedulingSettings(BatteryIndividualSchedulableEvent bies)
			throws BatteryEventInPastException {
		// build the initial timings from the provided details in the source event
		ZonedDateTime plannedStartTime = getBatteryEvent().getPlannedStartTime();
		bies.setPlannedEventActivationTime(plannedStartTime);
		ZonedDateTime scheduledStartTime = getScheduledStartTimeFromPlanned(bies, plannedStartTime);
		bies.setScheduledEventActivationTime(scheduledStartTime);
	}

	private ZonedDateTime getScheduledStartTimeFromPlanned(BatteryIndividualSchedulableEvent bies,
			@NonNull ZonedDateTime plannedStartZDT) throws BatteryEventInPastException {
		log.info("plannedStartZDT is " + plannedStartZDT);
		ZonedDateTime futureLimitZDT = ZonedDateTime.now();

		if (bies.getParentEvent().getBatteryEvent().isProcessOldEvent()) {
			// we allow processing old events.
			ZonedDateTime scheduledStartZDT = plannedStartZDT.isAfter(futureLimitZDT) ? plannedStartZDT
					: futureLimitZDT;
			return scheduledStartZDT;
		} else {
			// don;t allow historical events
			if (plannedStartZDT.isAfter(futureLimitZDT)) {
				return plannedStartZDT;
			} else {
				// we don't allow historical events and the planned time is in the past, error
				// on it
				throw new BatteryEventInPastException(
						"Cannot initially schedule event as it's start time is in the past and processOldeEvent is false "
								+ bies.getParentEvent().getBatteryEvent());
			}
		}
	}

	public abstract void schedule(BatteryIndividualSchedulableEventScheduler batteryIndividualSchedulableEventScheduler)
			throws BatteryEventInPastException;

	public abstract void reschedule(BatteryIndividualSchedulableEvent batteryIndividualSchedulableEvent,
			BatteryIndividualSchedulableEventScheduler batteryIndividualSchedulableEventScheduler)
			throws BatteryEventInPastException, BatteryEventRepeatCompletedException, BatteryEventNoRepeatException;

	public void scheduleStartTimeFromPlanned(BatteryIndividualSchedulableEvent batteryIndividualSchedulableEvent,
			BatteryIndividualSchedulableEventScheduler batteryIndividualSchedulableEventScheduler)
			throws BatteryEventInPastException {
		populateInitialSchedulingSettings(batteryIndividualSchedulableEvent);
		batteryIndividualSchedulableEventScheduler.addFutureEvent(batteryIndividualSchedulableEvent, false);
	}

	// this is the basic version that just rolls an event forward, for more complex
	// event types then other things may get handled in higher level implementations
	public void rescheduleStartTimeFromPlanned(BatteryIndividualSchedulableEvent batteryIndividualSchedulableEvent,
			BatteryIndividualSchedulableEventScheduler batteryIndividualSchedulableEventScheduler)
			throws BatteryEventInPastException, BatteryEventRepeatCompletedException, BatteryEventNoRepeatException {
		// setup the repeat based on what we last did
		RepeatRule repeatRule = batteryEvent.getRepeatRule();
		if (repeatRule == null) {
			log.info("No repeat rule on " + batteryEvent + " not reschedulable");
			throw new BatteryEventNoRepeatException("No repeat rule on " + batteryEvent + " not reschedulable");
		}
		ZonedDateTime potentialRestartTime = repeatRule
				.calculateNextRepeat(batteryIndividualSchedulableEvent.getPlannedEventActivationTime());
		if (potentialRestartTime == null) {
			log.info("Completed repeat cycle for " + batteryEvent);
			throw new BatteryEventRepeatCompletedException("Completed repeat cycle for " + batteryEvent);
		}
		// get the next event, from the current one
		BatteryIndividualSchedulableEvent nextEvent = batteryIndividualSchedulableEvent.generateNextScheduledEvent(
				potentialRestartTime,
				getScheduledStartTimeFromPlanned(batteryIndividualSchedulableEvent, potentialRestartTime));
		batteryIndividualSchedulableEventScheduler.addFutureEvent(nextEvent, false);
	}

	public boolean isSystemEvent() {
		return batteryEvent.isSystemEvent();
	}
}
