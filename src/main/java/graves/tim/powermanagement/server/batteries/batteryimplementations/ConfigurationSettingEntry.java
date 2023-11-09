package graves.tim.powermanagement.server.batteries.batteryimplementations;

import graves.tim.powermanagement.common.data.DataType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfigurationSettingEntry {
	private final String privateName;
	private final DataType dataType;
	private final boolean readOnly;
}
