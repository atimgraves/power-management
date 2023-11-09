package graves.tim.powermanagement.server.batteries.manager.events.core;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledFuture;

import com.fasterxml.jackson.annotation.JsonIgnore;

import graves.tim.powermanagement.common.exceptions.NotYetImplementedException;
import graves.tim.powermanagement.server.batteries.manager.BatteryData;
import graves.tim.powermanagement.server.batteries.manager.BatteryManager;
import graves.tim.powermanagement.server.batteries.manager.events.BatteryIndividualSchedulableEventScheduler;
import graves.tim.powermanagement.server.batteries.manager.events.BatteryIndividualSchedulableEventType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.java.Log;

@Log
@Data
@SuperBuilder
@EqualsAndHashCode(of = "scheduledEventActivationTime")
public abstract class BatteryIndividualSchedulableEvent
		implements Comparable<BatteryIndividualSchedulableEvent>, Cloneable {
	private static long eventIdCounter = 0;
	private final long eventId = eventIdCounter++;
	public static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
	@ToString.Exclude // we don't want a recursive ping pong between two strings
	@JsonIgnore
	private BatteryData batteryData;
	private BatteryIndividualSchedulableEventType batteryIndividualSchedulableEventType;
	private ZonedDateTime scheduledEventActivationTime;
	private ZonedDateTime plannedEventActivationTime;
	// this is used when running to enable us to manage the parent object
	@ToString.Exclude
	private BatterySchedulableEvent parentEvent;
	// our scheduled future, used if we need to cancel ourselves
	private ScheduledFuture<?> eventFuture;

	public BatteryIndividualSchedulableEvent(
			BatteryIndividualSchedulableEventType batteryIndividualSchedulableEventType, BatteryData targetBattery,
			BatterySchedulableEvent parentEvent) {
		this.batteryIndividualSchedulableEventType = batteryIndividualSchedulableEventType;
		this.batteryData = targetBattery;
		this.parentEvent = parentEvent;
	}

	public BatteryIndividualSchedulableEvent(BatteryIndividualSchedulableEvent other) {
		this.batteryData = other.batteryData;
		this.batteryIndividualSchedulableEventType = other.batteryIndividualSchedulableEventType;
		this.scheduledEventActivationTime = other.scheduledEventActivationTime;
		this.plannedEventActivationTime = other.plannedEventActivationTime;
		this.parentEvent = other.parentEvent;
	}

	public BatteryData getBatteryData() {
		return batteryData;
	}

	public BatteryIndividualSchedulableEvent generateNextScheduledEvent(ZonedDateTime planned,
			ZonedDateTime scheduled) {
		BatteryIndividualSchedulableEvent other;
		try {
			other = (BatteryIndividualSchedulableEvent) this.clone();
		} catch (CloneNotSupportedException e) {
			throw new NotYetImplementedException("Major programmikng problem here, trying to clone but can't");
		}
		other.eventFuture = null;
		other.plannedEventActivationTime = planned;
		other.scheduledEventActivationTime = scheduled;
		return other;
	}

	public BatteryIndividualSchedulableEvent(BatterySchedulableEvent parentEvent,
			BatteryIndividualSchedulableEventType batteryIndividualSchedulableEventType) {
		this.parentEvent = parentEvent;
		this.batteryIndividualSchedulableEventType = batteryIndividualSchedulableEventType;
	}

	@Override
	public int compareTo(BatteryIndividualSchedulableEvent other) {
		return this.scheduledEventActivationTime.compareTo(other.scheduledEventActivationTime);
	}

	// flags that this child events is running, the scheduler will run the
	// actual child events processing
	// (including the stop / start) in a synchronized block on this to ensure that
	// only one task from any parent is running at a time
	public void startProcessing() {
		parentEvent.processingChildEventStart(this);
	}

	// flags that this child events is no longer running, the scheduler will run the
	// actual child events processing
	// (including the stop / start) in a synchronized block on this to ensure that
	// only one task from any parent is running at a time
	public void stopProcessing() {
		parentEvent.processingChildEventEnd(this);
	}

	// actually run the child event, the scheduler will run the
	// actual child events processing
	// (including the stop / start) in a synchronized block on this to ensure that
	// only one task from any parent is running at a time
	public abstract boolean executeBatteryIndividualSchedulableEvent(BatteryManager batteryManager,
			BatteryIndividualSchedulableEventScheduler batteryIndividualSchedulableEventScheduler) throws Exception;

}