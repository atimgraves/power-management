package graves.tim.powermanagement.common.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public class ChargeTimeSetting {
	// these are absolute time numbers, but actually only the time elements will be
	// used.
	private long startTime;
	private long endTime;
	private int maxExternalPower;
	private boolean allowCharging;
}
