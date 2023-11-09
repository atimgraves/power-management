package graves.tim.powermanagement.server.batteries.manager.events;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import graves.tim.powermanagement.server.batteries.manager.events.core.BatteryIndividualSchedulableEvent;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BatteryIndividualSchedulableEventsQueue extends TreeSet<BatteryIndividualSchedulableEvent> {
	private static final long serialVersionUID = -964674002782084890L;

	public List<BatteryIndividualSchedulableEvent> getPastEvents() {
		return getPastEvents(System.currentTimeMillis());
	}

	public List<BatteryIndividualSchedulableEvent> getPastEvents(long cutoffTimestamp) {
		ZonedDateTime currentTime = ZonedDateTime.now();
		return super.stream().takeWhile(event -> event.getScheduledEventActivationTime().isBefore(currentTime))
				.collect(Collectors.toList());
	}

	public String toJson() throws JsonProcessingException {
		return toJson(this);
	}

	public static String toJson(BatteryIndividualSchedulableEventsQueue batteryIndividualSchedulableEventsQueue)
			throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(batteryIndividualSchedulableEventsQueue);
	}
}
