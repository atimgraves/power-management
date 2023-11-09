package graves.tim.powermanagement.common.data;

public enum RepeatDays {
	WEEKDAYS, WEEKENDS, EVERYDAY;

	public static RepeatDays getByName(String repeatDayName) {
		for (RepeatDays repeatDay : RepeatDays.values()) {
			if (repeatDay.name().equals(repeatDayName)) {
				return repeatDay;
			}
		}
		return null;
	}
}
