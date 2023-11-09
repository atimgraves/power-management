package graves.tim.powermanagement.server.batteries.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import graves.tim.powermanagement.common.data.BatteryEvent;
import graves.tim.powermanagement.common.data.EventDataItems;
import graves.tim.powermanagement.common.exceptions.BatteryEventActiveChildTasksException;
import graves.tim.powermanagement.common.exceptions.BatteryEventEventTypeRequiresSystemBattery;
import graves.tim.powermanagement.common.exceptions.BatteryEventEventTypeUnsupportedOnSystemBatteryException;
import graves.tim.powermanagement.common.exceptions.BatteryEventNotYetSupportedEventTypeException;
import graves.tim.powermanagement.common.exceptions.BatteryEventOutstandingChildTasksException;
import graves.tim.powermanagement.common.exceptions.BatteryEventUnimplementedEventTypeException;
import graves.tim.powermanagement.common.exceptions.BatteryEventUnknownEventTypeException;
import graves.tim.powermanagement.common.exceptions.BatteryNotFoundException;
import graves.tim.powermanagement.server.batteries.GenericBattery;
import graves.tim.powermanagement.server.batteries.manager.events.active.BatterySchedulableEventFactory;
import graves.tim.powermanagement.server.batteries.manager.events.core.BatterySchedulableEvent;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;

@Data
@EqualsAndHashCode(callSuper = true)
@Log
@NoArgsConstructor
public class BatteryData extends EventDataItems {
	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	private GenericBattery battery;
	private String batteryName;
	private Map<Long, BatterySchedulableEvent> batterySchedulableEvents = new HashMap<>();

	public BatteryData(String batteryName, GenericBattery battery) {
		this.batteryName = batteryName;
		this.battery = battery;
	}

	public GenericBattery retrieveBattery() {
		return battery;
	}

	public BatteryEvent retrieveEvent(long eventId) {
		return batterySchedulableEvents.get(eventId).getBatteryEvent();
	}

	public BatteryEvent removeEvent(long eventId)
			throws BatteryEventOutstandingChildTasksException, BatteryEventActiveChildTasksException {
		BatterySchedulableEvent bseOld = batterySchedulableEvents.get(eventId);
		if (bseOld == null) {
			return null;
		} else {
			bseOld.deleteEvent();
			return batterySchedulableEvents.remove(eventId).getBatteryEvent();
		}
	}

	public BatterySchedulableEvent addEvent(BatterySchedulableEventFactory batterySchedulableEventFactory,
			BatteryEvent event)
			throws BatteryEventUnknownEventTypeException, BatteryEventUnimplementedEventTypeException,
			BatteryEventEventTypeRequiresSystemBattery, BatteryEventEventTypeUnsupportedOnSystemBatteryException,
			BatteryNotFoundException, BatteryEventNotYetSupportedEventTypeException {
		BatterySchedulableEvent bse = batterySchedulableEventFactory.buildEvent(event);
		log.info("Battery " + batteryName + " about to add BatterySchedulableEvent " + bse);
		batterySchedulableEvents.put(event.getEventId(), bse);
		return bse;
	}

	@JsonIgnore
	public Collection<BatteryEvent> getOutstandingBatteryEvents() {
		synchronized (this) {
			return batterySchedulableEvents.values().stream().map(scheduledEvent -> scheduledEvent.getBatteryEvent())
					.sorted().collect(Collectors.toList());
		}
	}

	public void removeEvent(BatteryEvent batteryEvent)
			throws BatteryEventOutstandingChildTasksException, BatteryEventActiveChildTasksException {
		removeEvent(batteryEvent.getEventId());
	}

	public boolean systemBattery() {
		return battery.systemBattery();
	}

}
