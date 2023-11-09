package graves.tim.powermanagement.common.data;

import lombok.Getter;

public enum RepeatType {
	MONTH("months"), WEEK("weeks"), DAY("days"), HOUR("hours"), MINUTE("minutes"), SECOND("seconds");

	@Getter
	private String pluralName;

	private RepeatType(String pluralName) {
		this.pluralName = pluralName;
	}

	public static RepeatType getByName(String repeatTypeName) {
		for (RepeatType repeatType : RepeatType.values()) {
			if (repeatType.name().equals(repeatTypeName)) {
				return repeatType;
			}
		}
		return null;
	}

	public static String[] getNames() {
		String resp[] = new String[RepeatType.values().length];
		for (int i = 0; i < RepeatType.values().length; i++) {
			resp[i] = RepeatType.values()[i].name();
		}
		return resp;
	}
}
