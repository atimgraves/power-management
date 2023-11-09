package graves.tim.powermanagement.common.data;

import java.time.ZonedDateTime;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(of = "eventId", callSuper = true)
@ToString(callSuper = true)
public class BatteryEvent extends EventDataItems implements Comparable<BatteryEvent> {
	@Builder.Default
	private long eventId = 0; // the server will assign the eventId when it returns the completed data
								// structure
	private BatteryEventType eventType;
	private String batteryName;
	private RepeatRule repeatRule;
	private ZonedDateTime plannedStartTime;
	@Builder.Default
	private boolean systemEvent = false; // if this is a system event then it's immune from being deleted unless an
											// override is specified

	@Builder.Default
	private boolean processOldEvent = false; // if this is set to true then if the provided time is past the event will
												// still be processed, initially as soon as possible then any repeats
												// based on repeats from the origional time, however is this is set to
												// false then
												// if the current time is later than the specified time the events calls
												// in the past will just be dropped

	public void applyPlannedStartTime(ZonedDateTime plannedStartTime) {
		this.plannedStartTime = plannedStartTime;
	}

	@Override
	public int compareTo(BatteryEvent other) {
		return Long.compare(eventId, other.eventId);
	}

}
