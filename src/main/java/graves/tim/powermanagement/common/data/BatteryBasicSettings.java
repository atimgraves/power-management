package graves.tim.powermanagement.common.data;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public class BatteryBasicSettings {
	private int hourlyWattsUsed;
	private int wattsWhenFull;
	@Builder.Default
	private List<ChargeTimeSetting> chargeTimeSettings = new ArrayList<>();
}
