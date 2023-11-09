package graves.tim.powermanagement.common.data;

public interface PlannedPowerCutEvent extends EventSettingBaseNames {
	public String EXPECTED_HOURLY_CONSUMPTION_DURING_CUT = "expectedHourlyConsumptionDuringCut";
	public String DESIRED_RESERVE_AFTER_EVENT = "desiredReserveAfterEvent";
	public String EXPECTED_START_TIME = "expectedStartTime";
	public String EXPECTED_END_TIME = "expectedEndTime";
}
