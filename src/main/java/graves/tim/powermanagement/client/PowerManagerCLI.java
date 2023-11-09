package graves.tim.powermanagement.client;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import graves.tim.powermanagement.common.connectionsupport.AuthBuilder;
import graves.tim.powermanagement.common.data.BatteryConfigurationSetting;
import graves.tim.powermanagement.common.data.BatteryEvent;
import graves.tim.powermanagement.common.data.BatteryEventType;
import graves.tim.powermanagement.common.data.DataItem;
import graves.tim.powermanagement.common.data.DataItemIncorrectBooleanFormatException;
import graves.tim.powermanagement.common.data.DataItemIncorrectIntegerFormatException;
import graves.tim.powermanagement.common.data.DataItemIncorrectTypeException;
import graves.tim.powermanagement.common.data.DataItemNotFoundException;
import graves.tim.powermanagement.common.data.EventDataItems;
import graves.tim.powermanagement.common.data.PerBatterySettings;
import graves.tim.powermanagement.common.data.PlannedPowerCutEvent;
import graves.tim.powermanagement.common.data.RepeatRule;
import graves.tim.powermanagement.common.data.RepeatType;
import graves.tim.powermanagement.common.data.SetDesiredReserveEvent;
import graves.tim.powermanagement.common.data.SetReserveLevelRelativeToCurrentLevelLevelEvent;
import graves.tim.powermanagement.common.data.TestTickEvent;
import graves.tim.powermanagement.common.exceptions.BatteryEventActiveChildTasksException;
import graves.tim.powermanagement.common.exceptions.BatteryEventEventInProgressException;
import graves.tim.powermanagement.common.exceptions.BatteryEventIsSystemEventException;
import graves.tim.powermanagement.common.exceptions.BatteryEventOutstandingChildTasksException;
import graves.tim.powermanagement.common.exceptions.BatteryEventUnknownIdException;
import graves.tim.powermanagement.common.exceptions.BatteryException;
import graves.tim.powermanagement.common.exceptions.BatteryInvalidParameterException;
import graves.tim.powermanagement.common.exceptions.BatteryNotFoundException;
import graves.tim.powermanagement.common.exceptions.BatteryProgrammingProblemException;
import graves.tim.powermanagement.common.exceptions.BatteryReadOnlySettingException;
import graves.tim.powermanagement.common.exceptions.BatteryStateException;
import graves.tim.powermanagement.common.exceptions.BatteryUnauthorisedAccessException;
import graves.tim.powermanagement.common.exceptions.BatteryUnexpectedResponseException;
import graves.tim.powermanagement.common.exceptions.BatteryUnknownSettingException;
import graves.tim.powermanagement.common.exceptions.BatteryUnsupportedOperationException;
import graves.tim.powermanagement.common.restclients.BatteryController;
import lombok.extern.java.Log;
import timgutilities.textio.ChoiceDescription;
import timgutilities.textio.ChoiceDescriptionData;
import timgutilities.textio.RunnableCommand;
import timgutilities.textio.TextIOUtils;
import timgutilities.textio.TextIOUtils.NUM_TYPE;

@Log
public class PowerManagerCLI {
	private BatteryController batteryController;
	private String activeBattery;
	private ZoneId tz = ZoneId.systemDefault();

	public final static void main(String args[]) {
		if (args.length < 3) {
			log.severe("Usage: serverURL, username, password");
			System.exit(-1);
		}
		String serverURL = args[0];
		String username = args[1];
		String password = args[2];
		PowerManagerCLI powerManagerCLI = new PowerManagerCLI(serverURL, username, password);
		try {
			String batteryChoice = powerManagerCLI.choseBattery();
			if (powerManagerCLI.activeBattery == null) {
				log.info("Unfortunately there are no available batteries, cannot continue");
				System.exit(-2);
			}
			TextIOUtils.doOutput(batteryChoice);
			powerManagerCLI.runMainLoop();
		} catch (IOException e) {
			log.severe("IOException processing loop, cannot continue, " + e.getLocalizedMessage());
			System.exit(-100);
		}

	}

	private RunnableCommand mainLoopCommands[] = { new RunnableCommand("Get battery current charge level", () -> {
		return getBatteryCurrentChargeLevel();
	}), new RunnableCommand("Get battery reserve charge level", () -> {
		return getBatteryReserveChargeLevel();
	}), new RunnableCommand("Planned power cut", () -> {
		return planForPowerCut();
	}), new RunnableCommand("Set minimum allowed power reserve", () -> {
		return setMinimumAllowedReservePowerLevel();
	}), new RunnableCommand("Get minimum allowed power reserve", () -> {
		return getMinimumAllowedReservePowerLevel();
	}), new RunnableCommand("Set power reserve level with optional repeat", () -> {
		return setReservePowerLevel();
	}), new RunnableCommand("Set power reserve level relative to the current level when executed", () -> {
		return setRelativeToCurrentLevelReserveLevel();
	}), new RunnableCommand("Get operating modes", () -> {
		return listOperatingModes();
	}), new RunnableCommand("Get operating mode", () -> {
		return getOperatingMode();
	}), new RunnableCommand("Set operating mode", () -> {
		return setOperatingMode();
	}), new RunnableCommand("Access the state management sub menu", () -> {
		return runStateLoop();
	}), new RunnableCommand("Access the events and debug sub menu", () -> {
		return runEventsAndDebugLoop();
	}), new RunnableCommand("Access the client settings sub menu", () -> {
		return runClientSettingsLoop();
	}) };
	private RunnableCommand saveStateLoopCommands[] = { new RunnableCommand("Save the battery state", () -> {
		return saveBatteryState();
	}), new RunnableCommand("Get all per battery settings", () -> {
		return getAllPerBatterySettings();
	}), new RunnableCommand("Save a battery setting for later access", () -> {
		return saveConfigurationToSavedConfiguration();
	}), new RunnableCommand("List all saved settings", () -> {
		return listAllSavedConfigSettingNames();
	}), new RunnableCommand("List specific saved settings", () -> {
		return listSpecificSavedConfigSettingNames();
	}), new RunnableCommand("List all previous settings", () -> {
		return listAllPreviousConfigSettingNames();
	}), new RunnableCommand("Get a saved setting", () -> {
		return getSavedConfigSetting();
	}), new RunnableCommand("Get a previous setting", () -> {
		return getPreviousConfigSetting();
	}), new RunnableCommand("Restore saved setting", () -> {
		return restoreSavedConfigSetting();
	}), new RunnableCommand("Restore previous setting", () -> {
		return restorePreviousConfigSetting();
	}) };
	private RunnableCommand eventsAndDebugLoopCommands[] = { new RunnableCommand("List outstanding events", () -> {
		return listOutstandingEvents();
	}), new RunnableCommand("Delete an outstanding event", () -> {
		return deleteOutstandingEvent();
	}), new RunnableCommand("Start a ticker", () -> {
		return startTicker();
	}), new RunnableCommand("Start a simple (predefined) ticker", () -> {
		return startSimpleTicker();
	}) };
	private RunnableCommand clientSettingsLoopCommands[] = { new RunnableCommand("Chose active battery", () -> {
		return choseBattery();
	}), new RunnableCommand("Change the current default timezone", () -> {
		return changeCurrentDefaultTimezone();
	}) };

	private void runMainLoop() throws IOException {
		ZonedDateTime zdtNow = ZonedDateTime.now();
		TextIOUtils.doOutput("Program starting at " + zdtNow.format(DateTimeFormatter.ISO_DATE_TIME));
		TextIOUtils.selectAndRunLoop("Please chose from", mainLoopCommands, true, true, true);
	}

	private String runStateLoop() throws IOException {
		TextIOUtils.selectAndRunLoop("Please chose from", saveStateLoopCommands, true, false, true);
		return "Exited state sub menu";
	}

	private String runEventsAndDebugLoop() throws IOException {
		TextIOUtils.selectAndRunLoop("Please chose from", eventsAndDebugLoopCommands, true, false, true);
		return "Exited events and debug sub menu";
	}

	private String runClientSettingsLoop() throws IOException {
		TextIOUtils.selectAndRunLoop("Please chose from", clientSettingsLoopCommands, true, false, true);
		return "Exited client settings sub menu";
	}

	private String changeCurrentDefaultTimezone() throws IOException {
		if (TextIOUtils.getYN("The current time zone is " + tz.getId() + " do you want to change it ?", false)) {
			tz = TextIOUtils.getTimeZoneByName("Please select the new timezone", true);
			return "Timezone has been changed and set to " + tz.getId();
		} else {
			return "Timezone unchanged and set to " + tz.getId();
		}
	}

	private String saveBatteryState() throws BatteryNotFoundException, BatteryStateException {
		return batteryController.saveBatteryState(activeBattery);
	}

	private String listOperatingModes() throws BatteryNotFoundException {
		Collection<String> modes = batteryController.getOperatingModes(activeBattery);
		return modes.stream().collect(Collectors.joining("\n"));
	}

	private String getOperatingMode()
			throws BatteryNotFoundException, DataItemNotFoundException, BatteryUnsupportedOperationException,
			BatteryUnauthorisedAccessException, BatteryUnexpectedResponseException {
		return batteryController.getOperatingMode(activeBattery);
	}

	private String setOperatingMode() throws BatteryNotFoundException, BatteryInvalidParameterException,
			BatteryUnsupportedOperationException, BatteryUnauthorisedAccessException, IOException,
			DataItemNotFoundException, BatteryUnexpectedResponseException, DataItemIncorrectBooleanFormatException,
			DataItemIncorrectIntegerFormatException, DataItemIncorrectTypeException, BatteryReadOnlySettingException {
		Collection<String> modes = batteryController.getOperatingModes(activeBattery);
		String currentMode = batteryController.getOperatingMode(activeBattery);
		String newMode = TextIOUtils.getStringChoice("Please chose a mode, the current mode is " + currentMode, modes);
		return batteryController.setOperatingMode(activeBattery, newMode);
	}

	private String saveConfigurationToSavedConfiguration() throws BatteryNotFoundException, IOException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException, DataItemNotFoundException,
			BatteryUnauthorisedAccessException, BatteryInvalidParameterException, BatteryUnknownSettingException,
			BatteryUnsupportedOperationException, DataItemIncorrectTypeException {
		// get the supported options for the battery
		Collection<BatteryConfigurationSetting> supportedSettings = batteryController
				.listSupportedConfigurationSettings(activeBattery);
		ChoiceDescriptionData<BatteryConfigurationSetting> cdd = new ChoiceDescriptionData<>();
		cdd.addChoiceDescription(supportedSettings.stream()
				.map(setting -> new ChoiceDescription<>(setting.getPublicName(), setting)).toList());
		BatteryConfigurationSetting setting = TextIOUtils.getParamChoice("Please select the setting to save", cdd);
		if (setting == null) {
			return "No setting chosen";
		}
		String saveName = TextIOUtils.getString(
				"Please enter the name to save the current setting for " + setting.getPublicName() + " under", "save");
		DataItem oldValue = batteryController.saveConfigurationToSavedConfiguration(activeBattery, setting, saveName);
		if (oldValue == null) {
			return "No previously saved value for " + setting.getPublicName() + " as " + saveName;
		}
		return "Previous saved setting for " + setting.getPublicName() + " as " + saveName + " was "
				+ oldValue.getValue();
	}

	private String listAllSavedConfigSettingNames() throws BatteryNotFoundException, BatteryUnknownSettingException {
		var savedSettingNames = batteryController.listSavedConfigSettingNames(activeBattery);
		return "Currently saved settings are " + savedSettingNames;
	}

	private String listSpecificSavedConfigSettingNames()
			throws BatteryNotFoundException, BatteryUnknownSettingException, IOException {
		// get the supported options for the battery
		Collection<BatteryConfigurationSetting> supportedSettings = batteryController
				.listSupportedConfigurationSettings(activeBattery);
		ChoiceDescriptionData<BatteryConfigurationSetting> cdd = new ChoiceDescriptionData<>();
		supportedSettings.stream().map(setting -> new ChoiceDescription<>(setting.getPublicName(), setting)).toList();
		cdd.addChoiceDescription(supportedSettings.stream()
				.map(setting -> new ChoiceDescription<>(setting.getPublicName(), setting)).toList());
		BatteryConfigurationSetting setting = TextIOUtils.getParamChoice("Please select the setting to list", cdd);
		var savedSettingNames = batteryController.listSavedConfigSettingNames(activeBattery, setting);
		if (setting == null) {
			return "No setting chosen";
		}
		return "Currently saved settings for " + setting.getPublicName() + " are" + savedSettingNames;
	}

	private String listAllPreviousConfigSettingNames() throws BatteryNotFoundException, BatteryUnknownSettingException {
		var savedSettingNames = batteryController.listPreviousConfigSettingNames(activeBattery);
		return "Current previous settings are " + savedSettingNames;
	}

	private String getSavedConfigSetting() throws BatteryNotFoundException, BatteryUnknownSettingException, IOException,
			DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnsupportedOperationException {
		// get the supported options for the battery
		Collection<BatteryConfigurationSetting> supportedSettings = batteryController
				.listSupportedConfigurationSettings(activeBattery);
		ChoiceDescriptionData<BatteryConfigurationSetting> cdd = new ChoiceDescriptionData<>();
		cdd.addChoiceDescription(supportedSettings.stream()
				.map(setting -> new ChoiceDescription<>(setting.getPublicName(), setting)).toList());
		BatteryConfigurationSetting setting = TextIOUtils.getParamChoice("Please select the setting to get", cdd);
		if (setting == null) {
			return "No setting chosen";
		}
		String saveName = TextIOUtils
				.getString("Please enter the name the setting " + setting.getPublicName() + " is saved under", "save");
		DataItem savedValue = batteryController.getSavedConfiguration(activeBattery, setting, saveName);
		if (savedValue == null) {
			return "No saved value for " + setting.getPublicName() + " as " + saveName;
		}
		return "Saved setting for " + setting.getPublicName() + " as " + saveName + " was " + savedValue.getValue()
				+ " (" + savedValue.getType() + ")";
	}

	private String getPreviousConfigSetting() throws BatteryNotFoundException, BatteryUnknownSettingException,
			IOException, DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnsupportedOperationException {
		// get the supported options for the battery
		Collection<BatteryConfigurationSetting> supportedSettings = batteryController
				.listSupportedConfigurationSettings(activeBattery);
		ChoiceDescriptionData<BatteryConfigurationSetting> cdd = new ChoiceDescriptionData<>();
		cdd.addChoiceDescription(supportedSettings.stream()
				.map(setting -> new ChoiceDescription<>(setting.getPublicName(), setting)).toList());
		BatteryConfigurationSetting setting = TextIOUtils.getParamChoice("Please select the setting to get", cdd);
		if (setting == null) {
			return "No setting chosen";
		}
		DataItem previousValue = batteryController.getPreviousConfiguration(activeBattery, setting);
		if (previousValue == null) {
			return "No previous value for " + setting.getPublicName();
		}
		return "Previous setting for " + setting.getPublicName() + " was " + previousValue.getValue() + " ("
				+ previousValue.getType() + ")";
	}

	private String restoreSavedConfigSetting() throws BatteryNotFoundException, BatteryUnknownSettingException,
			IOException, DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnsupportedOperationException, BatteryReadOnlySettingException {
// get the supported options for the battery
		Collection<BatteryConfigurationSetting> supportedSettings = batteryController
				.listSupportedConfigurationSettings(activeBattery);
		ChoiceDescriptionData<BatteryConfigurationSetting> cdd = new ChoiceDescriptionData<>();
		cdd.addChoiceDescription(supportedSettings.stream()
				.map(setting -> new ChoiceDescription<>(setting.getPublicName(), setting)).toList());
		BatteryConfigurationSetting setting = TextIOUtils.getParamChoice("Please select the setting to restore", cdd);
		if (setting == null) {
			return "No setting chosen";
		}
		String saveName = TextIOUtils
				.getString("Please enter the name the setting " + setting.getPublicName() + " is saved under", "save");
		DataItem previousValue = batteryController.restoreSavedConfiguration(activeBattery, setting, saveName);
		if (previousValue == null) {
			return "No previous value for " + setting.getPublicName();
		}
		return "Previous setting for " + setting.getPublicName() + " was " + previousValue.getValue() + " ("
				+ previousValue.getType() + ")";
	}

	private String restorePreviousConfigSetting() throws BatteryNotFoundException, BatteryUnknownSettingException,
			IOException, DataItemIncorrectBooleanFormatException, DataItemIncorrectIntegerFormatException,
			DataItemIncorrectTypeException, DataItemNotFoundException, BatteryUnauthorisedAccessException,
			BatteryInvalidParameterException, BatteryUnsupportedOperationException, BatteryReadOnlySettingException {
		// get the supported options for the battery
		Collection<BatteryConfigurationSetting> supportedSettings = batteryController
				.listSupportedConfigurationSettings(activeBattery);
		ChoiceDescriptionData<BatteryConfigurationSetting> cdd = new ChoiceDescriptionData<>();
		cdd.addChoiceDescription(supportedSettings.stream()
				.map(setting -> new ChoiceDescription<>(setting.getPublicName(), setting)).toList());
		BatteryConfigurationSetting setting = TextIOUtils
				.getParamChoice("Please select the previous setting to restore", cdd);
		if (setting == null) {
			return "No setting chosen";
		}
		DataItem previousValue = batteryController.restorePreviousConfiguration(activeBattery, setting);
		if (previousValue == null) {
			return "No previous value for " + setting.getPublicName();
		}
		return "Previous setting for " + setting.getPublicName() + " was " + previousValue.getValue() + " ("
				+ previousValue.getType() + ")";
	}

	private String listOutstandingEvents() throws BatteryNotFoundException {
		Collection<BatteryEvent> events = batteryController.getOutstandingEvents(activeBattery);
		return events.stream().map(event -> event.toString()).collect(Collectors.joining("\n"));
	}

	private String deleteOutstandingEvent()
			throws IOException, BatteryNotFoundException, BatteryEventOutstandingChildTasksException,
			BatteryEventUnknownIdException, BatteryEventEventInProgressException, BatteryEventIsSystemEventException,
			BatteryEventActiveChildTasksException {
		long eventId = TextIOUtils.getLong("Please enter the event ID to delete");
		boolean deleteSystemEvent = TextIOUtils.getYN("If this is a system event do you want to force delete it ?",
				false);
		return "Delete response is " + batteryController.deleteEvent(activeBattery, eventId, deleteSystemEvent);

	}

	private String startTicker() throws IOException, BatteryException {
		BatteryEventType batteryEventType = TextIOUtils.getYN("Require system battery ?", false)
				? BatteryEventType.TEST_TICK_SYSTEM
				: BatteryEventType.TEST_TICK_NON_SYSTEM;
		String message = TextIOUtils.getString("Please enter the message to use", "I'm a message");
		String startDate = TextIOUtils.getISODate("the events planned start");
		String startTime = TextIOUtils.getISOTime("the events planned start");
		String tzOffset = TextIOUtils.getISOTimeZoneOffset("the events planned start");
		String expectedStart = startDate + "T" + startTime + tzOffset;
		DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
		ZonedDateTime zdt = ZonedDateTime.from(dtf.parseBest(expectedStart, ZonedDateTime::from, LocalDateTime::from));
		log.info("Parsed and reformatted start time is " + zdt.format(dtf));
		boolean processOldStart = TextIOUtils.getYN("If the start is in the past do you want to allow it to run ?",
				false);
		RepeatRule repeatRule;
		if (TextIOUtils.getYN("Do you want a repeat ?", true)) {
			repeatRule = getRepeatRule("");
		} else {
			repeatRule = null;
		}
		BatteryEvent event = BatteryEvent.builder().eventType(batteryEventType).batteryName(activeBattery)
				.plannedStartTime(zdt).processOldEvent(processOldStart).repeatRule(repeatRule).build();
		event.addDataItem(TestTickEvent.MESSAGE, message);
		event.addDataItem(PlannedPowerCutEvent.EXPECTED_START_TIME, expectedStart);
		return "Assigned EventId info is " + batteryController.addEvent(activeBattery, event);
	}

	private String startSimpleTicker() throws IOException, BatteryException {
		if (!TextIOUtils.getYN(
				"Create a simple ticker starting in 20 seconds, repeating 5 times at 15 sec interval on the current battery",
				false)) {
			return "OK, not continuing";
		}
		BatteryEventType batteryEventType = TextIOUtils.getYN("Is the battery " + activeBattery + " a system battery ?",
				false) ? BatteryEventType.TEST_TICK_SYSTEM : BatteryEventType.TEST_TICK_NON_SYSTEM;
		String message = TextIOUtils.getString("Please enter the message to use", "I'm a message");
		ZonedDateTime zdt = ZonedDateTime.now(ZoneId.systemDefault());
		zdt = zdt.plusSeconds(20);
		log.info("Planned start time is " + zdt + " (as instant " + zdt.format(DateTimeFormatter.ISO_DATE_TIME) + ")");
		int repeatInterval = 15;
		int repeatCount = 5;
		RepeatRule repeatRule = RepeatRule.builder().repeatType(RepeatType.SECOND).repeatInterval(repeatInterval)
				.repeatsRemaining(repeatCount).build();
		BatteryEvent event = BatteryEvent.builder().eventType(batteryEventType).batteryName(activeBattery)
				.plannedStartTime(zdt).processOldEvent(true).repeatRule(repeatRule).build();
		event.addDataItem(TestTickEvent.MESSAGE, message);
		log.info("Sending BatteryEvent: " + event);
		return "Assigned EventId info is " + batteryController.addEvent(activeBattery, event);
	}

	private RepeatRule getRepeatRule(String string) throws IOException {
		int secs = TextIOUtils.getInt("How often in seconds do you wnt this to be repeated ?", 60);
		int repeatCount = TextIOUtils.getInt("How many times do you want this to repeat for (0 means indefinately)",
				secs);
		return RepeatRule.builder().repeatType(RepeatType.SECOND).repeatInterval(secs).repeatsRemaining(repeatCount)
				.build();
	}

	private String planForPowerCut() throws IOException, BatteryException {
		String expectedStart = TextIOUtils.getISODateTimeTimeZone("power cut start", tz);
		String expectedEnd = TextIOUtils.getISODateTimeTimeZone("power cut end", tz);
		ZonedDateTime zdt = ZonedDateTime.parse(expectedStart);
		int expectedPowerUsage = TextIOUtils.getInt(
				"Please enter the expected battery consumption per hour in percent during this cut", NUM_TYPE.RANGE, 1,
				50, 10);
		int desiredPowerRemaining = TextIOUtils
				.getInt("Please enter the desired battery level in percent after this cut", NUM_TYPE.RANGE, 1, 100, 5);
		BatteryEvent event = BatteryEvent.builder().eventType(BatteryEventType.PLANNED_POWER_CUT).plannedStartTime(zdt)
				.build();
		event.addDataItem(PlannedPowerCutEvent.EXPECTED_START_TIME, expectedStart);
		event.addDataItem(PlannedPowerCutEvent.EXPECTED_END_TIME, expectedEnd);
		event.addDataItem(PlannedPowerCutEvent.EXPECTED_HOURLY_CONSUMPTION_DURING_CUT, expectedPowerUsage);
		event.addDataItem(PlannedPowerCutEvent.DESIRED_RESERVE_AFTER_EVENT, desiredPowerRemaining);
		return "Assigned EventId info is " + batteryController.addEvent(activeBattery, event);
	}

	private String setMinimumAllowedReservePowerLevel()
			throws BatteryException, IOException, DataItemNotFoundException {
		DataItem minimum = batteryController.getPerBatterySetting(activeBattery,
				PerBatterySettings.MINIMUM_ALLOWED_RESERVE);
		int currentMinumum;
		try {
			currentMinumum = minimum == null ? 5 : minimum.integerValue();
		} catch (DataItemIncorrectIntegerFormatException | DataItemIncorrectTypeException e) {
			return "Programming problem = " + e.getLocalizedMessage();
		}
		int newMinimum = TextIOUtils.getInt("Please enter the minimum reserve power level allowed in percent.",
				NUM_TYPE.RANGE, 0, 100, currentMinumum);
		DataItem minimumUpdated = batteryController.setPerBatterySetting(activeBattery,
				PerBatterySettings.MINIMUM_ALLOWED_RESERVE, DataItem.asInteger(newMinimum));
		return "Old minimum is " + minimumUpdated + " new minimum is " + newMinimum;
	}

	private String getMinimumAllowedReservePowerLevel()
			throws BatteryException, IOException, DataItemNotFoundException {
		DataItem minimum = batteryController.getPerBatterySetting(activeBattery,
				PerBatterySettings.MINIMUM_ALLOWED_RESERVE);
		try {
			return "Minimum allowed reserve power level is "
					+ (minimum == null ? "Not Set" : "" + minimum.integerValue());
		} catch (DataItemIncorrectIntegerFormatException | DataItemIncorrectTypeException e) {
			return "Programming problem = " + e.getLocalizedMessage();
		}
	}

	private String getAllPerBatterySettings() throws BatteryException, IOException {
		EventDataItems eventDataItems = batteryController.getPerBatterySettings(activeBattery);
		return eventDataItems.allDataItems().toString();
	}

	private String setRelativeToCurrentLevelReserveLevel() throws BatteryException, IOException {
		BatteryEvent event = BatteryEvent.builder()
				.eventType(BatteryEventType.SET_RELATIVE_TO_CURRENT_LEVEL_RESERVE_LEVEL).batteryName(activeBattery)
				.build();
		int currentLevel = batteryController.getBatteryReserveChargeLevel(activeBattery);
		int relativeLevelToSet = TextIOUtils.getInt(
				"Please enter the relative reserve power level to the current power level when run.\n"
						+ "  A setting of 0 will mean set to the then current level, a negative number will allow some continuing level of discharge until the new reserve is reached\n"
						+ "  but will not go below the minimum battery reserve, a postive number will start charging up to the new reserve as\"+"
						+ "  soon as the event runs up to a maximum of 100%"
						+ "For your information the current reserve level is " + currentLevel,
				NUM_TYPE.RANGE, -100, 100, 0);

		event.addDataItem(SetReserveLevelRelativeToCurrentLevelLevelEvent.RELATIVE_TO_CURRENT_LEVEL_RESERVE,
				relativeLevelToSet);
		if (TextIOUtils.getYN(
				"Do you want to set an optional minimum level which will be applied if higher than the calculated level above ?",
				false)) {
			int optionalMinimumLevel = TextIOUtils.getInt("What minimum level do you want to apply ?", NUM_TYPE.RANGE,
					0, 100, false, 0);
			event.addDataItem(SetReserveLevelRelativeToCurrentLevelLevelEvent.OPTIONAL_MINIMUM_RESERVE_RESERVE,
					optionalMinimumLevel);
		}
		if (TextIOUtils.getYN("Set in 10 seconds time ?")) {
			event.applyPlannedStartTime(ZonedDateTime.now().plusSeconds(10));
		} else {
			String expectedStart = TextIOUtils.getISODateTimeTimeZone("when you want this setting to activate");
			DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
			ZonedDateTime zdt = ZonedDateTime
					.from(dtf.parseBest(expectedStart, ZonedDateTime::from, LocalDateTime::from));
			event.applyPlannedStartTime(zdt);
		}
		if (TextIOUtils.getYN("Do you want to repeat setting the relative reserve power level ?", false)) {
			event.setRepeatRule(getRepeat("Please set the repeat information"));
		}
		log.info("Requesting " + event);
		return "Assigned event info is " + batteryController.addEvent(activeBattery, event);
	}

	private String setReservePowerLevel() throws BatteryException, IOException {
		BatteryEvent event = BatteryEvent.builder().eventType(BatteryEventType.SET_RESERVE_POWER_LEVEL)
				.batteryName(activeBattery).build();
		int currentLevel = batteryController.getBatteryCurrentChargeLevel(activeBattery);
		int levelToSet = TextIOUtils.getInt(
				"Please enter the new reserve power level (if higher than the current charge level of " + currentLevel
						+ " when activated this will result in charging regardless of the time of use setting)",
				NUM_TYPE.RANGE, 0, 100, currentLevel);

		event.addDataItem(SetDesiredReserveEvent.DESIRED_RESERVE, levelToSet);
		if (TextIOUtils.getYN("Set in 10 seconds time ?")) {
			event.applyPlannedStartTime(ZonedDateTime.now().plusSeconds(10));
		} else {
			String expectedStart = TextIOUtils.getISODateTimeTimeZone("when you want this setting to activate");
			DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
			ZonedDateTime zdt = ZonedDateTime
					.from(dtf.parseBest(expectedStart, ZonedDateTime::from, LocalDateTime::from));
			event.applyPlannedStartTime(zdt);
		}
		if (TextIOUtils.getYN("Do you want to repeat setting the desired power level ?", false)) {
			event.setRepeatRule(getRepeat("Please set the repeat information"));
		}
		log.info("Requesting " + event);
		return "Assigned event info is " + batteryController.addEvent(activeBattery, event);
	}

	private RepeatRule getRepeat(String prompt) throws IOException {
		String repeatNames[] = RepeatType.getNames();
		String repeatName = TextIOUtils.getString("Please chose the repeat type from", repeatNames);
		RepeatType repeatType = RepeatType.valueOf(repeatName);
		int repeatInterval = TextIOUtils.getInt(
				"How many " + repeatType.getPluralName() + " do you want to wait for the next repeat ?", NUM_TYPE.RANGE,
				1, 100);
		int repeatCount;
		if (TextIOUtils.getYN("Do you want to repeat this forever?", false)) {
			repeatCount = RepeatRule.REPEAT_FOREVER;
		} else {
			repeatCount = TextIOUtils.getInt("How many times do you you want this to repeat", NUM_TYPE.RANGE, 1, 10000,
					1);
		}
		return RepeatRule.builder().repeatType(repeatType).repeatInterval(repeatInterval).repeatsRemaining(repeatCount)
				.build();
	}

	private String getBatteryReserveChargeLevel()
			throws BatteryNotFoundException, BatteryUnauthorisedAccessException, BatteryUnknownSettingException,
			BatteryInvalidParameterException, BatteryUnsupportedOperationException, BatteryProgrammingProblemException {
		return "Reserve charge level for " + activeBattery + " is "
				+ batteryController.getBatteryReserveChargeLevel(activeBattery);
	}

	private String getBatteryCurrentChargeLevel() throws BatteryNotFoundException, BatteryUnauthorisedAccessException,
			BatteryUnknownSettingException, BatteryInvalidParameterException, BatteryUnsupportedOperationException {
		return "Current charge level for " + activeBattery + " is "
				+ batteryController.getBatteryCurrentChargeLevel(activeBattery);
	}

	private String choseBattery() throws IOException {
		List<String> batteryNames = getBatteryNames();
		if (batteryNames.size() == 0) {
			TextIOUtils.doOutput("No available batteries, cannot continue");
			this.activeBattery = null;
		}
		if (batteryNames.size() == 1) {
			TextIOUtils.doOutput("Only one available battery, selecting it by default");
			this.activeBattery = batteryNames.get(0);
		}
		this.activeBattery = TextIOUtils.getStringChoice("Please chose the battery to operate on", getBatteryNames());
		return "You have chosen battery " + activeBattery;
	}

	private List<String> getBatteryNames() {
		return batteryController.getBatteries().stream().sorted().collect(Collectors.toList());
	}

	public PowerManagerCLI(String serverURL, String username, String password) {
		this.batteryController = RestClientBuilder.newBuilder().baseUri(URI.create(serverURL))
				.register(new AuthBuilder(username, password)).build(BatteryController.class);
	}

}
