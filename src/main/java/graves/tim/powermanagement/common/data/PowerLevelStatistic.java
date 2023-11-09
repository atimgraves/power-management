package graves.tim.powermanagement.common.data;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum PowerLevelStatistic {
	POWER_REMAINING_AS_PRECENTAGE;

	public static PowerLevelStatistic getByName(String name) {
		for (PowerLevelStatistic type : PowerLevelStatistic.values()) {
			if (type.name().equals(name)) {
				return type;
			}
		}
		return null;
	}

	public List<PowerLevelStatistic> getByNames(String names) {
		return getByNames(names.split(","));
	}

	public List<PowerLevelStatistic> getByNames(String[] split) {
		return Stream.of(split).map(name -> PowerLevelStatistic.getByName(name)).filter(stat -> stat != null)
				.collect(Collectors.toList());
	}
}
