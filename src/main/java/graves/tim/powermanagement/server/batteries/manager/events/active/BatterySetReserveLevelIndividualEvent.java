package graves.tim.powermanagement.server.batteries.manager.events.active;

import java.time.ZonedDateTime;

import graves.tim.powermanagement.common.data.DataItemIncorrectBooleanFormatException;
import graves.tim.powermanagement.common.data.DataItemIncorrectIntegerFormatException;
import graves.tim.powermanagement.common.data.DataItemIncorrectTypeException;
import graves.tim.powermanagement.common.data.DataItemNotFoundException;
import graves.tim.powermanagement.common.data.PerBatterySettings;
import graves.tim.powermanagement.common.data.SetDesiredReserveEvent;
import graves.tim.powermanagement.common.exceptions.BatteryEventInPastException;
import graves.tim.powermanagement.common.exceptions.BatteryEventNoRepeatException;
import graves.tim.powermanagement.common.exceptions.BatteryEventRepeatCompletedException;
import graves.tim.powermanagement.common.exceptions.BatteryInvalidParameterException;
import graves.tim.powermanagement.common.exceptions.BatteryProgrammingProblemException;
import graves.tim.powermanagement.common.exceptions.BatteryReadOnlySettingException;
import graves.tim.powermanagement.common.exceptions.BatteryUnauthorisedAccessException;
import graves.tim.powermanagement.common.exceptions.BatteryUnknownSettingException;
import graves.tim.powermanagement.common.exceptions.BatteryUnsupportedOperationException;
import graves.tim.powermanagement.server.batteries.manager.BatteryData;
import graves.tim.powermanagement.server.batteries.manager.BatteryManager;
import graves.tim.powermanagement.server.batteries.manager.events.BatteryIndividualSchedulableEventScheduler;
import graves.tim.powermanagement.server.batteries.manager.events.BatteryIndividualSchedulableEventType;
import graves.tim.powermanagement.server.batteries.manager.events.core.BatteryIndividualSchedulableEvent;
import graves.tim.powermanagement.server.batteries.manager.events.core.BatterySchedulableEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.java.Log;

@Log
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class BatterySetReserveLevelIndividualEvent extends BatteryIndividualSchedulableEvent {

	public BatterySetReserveLevelIndividualEvent(BatteryData batteryData,
			BatterySchedulableEvent parentBatteryScheduledEvent) {
		super(BatteryIndividualSchedulableEventType.SET_RESERVE_LEVEL_ABSOLUTE, batteryData,
				parentBatteryScheduledEvent);
	}

	@Override
	public boolean executeBatteryIndividualSchedulableEvent(BatteryManager batteryManager,
			BatteryIndividualSchedulableEventScheduler batteryIndividualSchedulableEventScheduler)
			throws DataItemIncorrectIntegerFormatException, DataItemIncorrectTypeException, DataItemNotFoundException,
			BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, BatteryProgrammingProblemException,
			DataItemIncorrectBooleanFormatException, BatteryReadOnlySettingException {
		int currentLevel = this.getBatteryData().retrieveBattery().retrieveCurrentChargeLevel();
		int desiredReserveLevel = this.getParentEvent().getBatteryEvent()
				.integerValue(SetDesiredReserveEvent.DESIRED_RESERVE);
		int allowedMinimum = this.getBatteryData().integerValue(PerBatterySettings.MINIMUM_ALLOWED_RESERVE,
				PerBatterySettingsDefaults.MINIMUM_ALLOWED_BATTERY_RESERVE_LEVEL);
		int maximumBatteryLevel = this.getBatteryData().integerValue(PerBatterySettings.MAXIMUM_BATTERY_LEVEL,
				PerBatterySettingsDefaults.MAXIMUM_BATTERY_LEVEL);
		// shouldn't happen, but just in case someone has done something really dumb
		allowedMinimum = allowedMinimum < 0 ? 0 : allowedMinimum;
		allowedMinimum = allowedMinimum > maximumBatteryLevel ? maximumBatteryLevel : allowedMinimum;
		// make sure that the new relative level is within range
		desiredReserveLevel = desiredReserveLevel < allowedMinimum ? allowedMinimum : desiredReserveLevel;
		desiredReserveLevel = desiredReserveLevel > maximumBatteryLevel ? maximumBatteryLevel : desiredReserveLevel;
		int previousReserveLevel = this.getBatteryData().retrieveBattery()
				.applyBatteryReserveLevel(desiredReserveLevel);
		log.info("Set reserve level for battery " + this.getBatteryData().getBatteryName() + "+Current battery level "
				+ currentLevel + ", Previous reserve level " + previousReserveLevel + ", Desired reserve level "
				+ desiredReserveLevel);
		// allow for this to be rescheduled - though most are likely to be one-offs
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
		return true;
	}
}
