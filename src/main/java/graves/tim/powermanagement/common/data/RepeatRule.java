package graves.tim.powermanagement.common.data;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.java.Log;

@Data
@NoArgsConstructor
@SuperBuilder
@Log
/**
 * By default this will be a event that repeats every day and will carry on
 * repeating forever (set repeatsRemaining to REPEAT_FOREVER to repeat forever,
 * to a number > 0 to repeat that many times)
 * 
 * @author tg13456
 *
 */
public class RepeatRule {
	public final static int REPEAT_FOREVER = -1;
	@Builder.Default
	private RepeatType repeatType = RepeatType.DAY;
	@Builder.Default
	private int repeatInterval = 1;
	@Builder.Default
	private int repeatsRemaining = REPEAT_FOREVER;
	@Builder.Default
	private RepeatDays repeatDays = RepeatDays.EVERYDAY;

	public ZonedDateTime calculateNextRepeat(ZonedDateTime previousScheduledRunTime) {
		return calculateNextRepeat(previousScheduledRunTime, true);
	}

	public ZonedDateTime calculateNextRepeat(ZonedDateTime previousScheduledRunTime, boolean rollForward) {
		ZonedDateTime nextRunTime = previousScheduledRunTime;
		ZonedDateTime now = ZonedDateTime.now(previousScheduledRunTime.getZone());
		// in some cases (e.g. a power cut) the previous run time may have been missed
		// OR
		// it may just have been set in the past
		if (rollForward) {
			while (nextRunTime.isBefore(now)) {
				log.finer("Rolling forward as next runtime " + nextRunTime.format(DateTimeFormatter.ISO_DATE_TIME)
						+ " is before the current time of " + now.format(DateTimeFormatter.ISO_DATE_TIME));
				nextRunTime = determineNextRepeat(nextRunTime);
				if (nextRunTime == null) {
					return null;
				}
			}
			return nextRunTime;
		} else {
			log.finer("Just calculating using next runtime " + nextRunTime.format(DateTimeFormatter.ISO_DATE_TIME));
			return determineNextRepeat(nextRunTime);
		}
	}

	private ZonedDateTime determineNextRepeat(ZonedDateTime previousScheduledRunTime) {
		if (repeatsRemaining > 0) {
			repeatsRemaining--;
		}
		if (repeatsRemaining == 0) {
			return null;
		}

		log.finest(() -> "Calculating next repeat from "
				+ previousScheduledRunTime.format(DateTimeFormatter.ISO_DATE_TIME) + " using " + this);

		ZonedDateTime calculatedStep;
		switch (repeatType) {
		case DAY:
			calculatedStep = processDailyRepeat(previousScheduledRunTime);
			break;
		case HOUR:
			calculatedStep = previousScheduledRunTime.plusHours(repeatInterval);
			break;
		case MINUTE:
			calculatedStep = previousScheduledRunTime.plusMinutes(repeatInterval);
			break;
		case MONTH:
			calculatedStep = previousScheduledRunTime.plusMonths(repeatInterval);
			break;
		case SECOND:
			calculatedStep = previousScheduledRunTime.plusSeconds(repeatInterval);
			break;
		case WEEK:
			calculatedStep = previousScheduledRunTime.plusWeeks(repeatInterval);
			break;
		default:
			log.severe("Unknown repeat type " + repeatType + ", will ignore this repeat reaquest");
			return null;
		}
		log.finest(() -> "Calculated repeat is " + calculatedStep.format(DateTimeFormatter.ISO_DATE_TIME));
		return calculatedStep;
	}

	private ZonedDateTime processDailyRepeat(ZonedDateTime previousScheduledRunTime) {
		// This should really do clever things based on the repeat spec E.g. figure out
		// repeat on tuesdays only or something, there's probabaly a library that can do
		// that, but for now just add the required number of days.
		return previousScheduledRunTime.plusDays(repeatInterval);
	}
}
