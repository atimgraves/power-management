package graves.tim.powermanagement.common.data;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class DataItem {
	@JsonIgnore
	public final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

	private DataType type;
	private String value;

	public static DataItem asNull() {
		return DataItem.builder().type(DataType.NULLDATA).build();
	}

	public static DataItem asString(String value) {
		return DataItem.builder().type(DataType.STRING).value(value).build();
	}

	public static DataItem asLong(Long value) {
		return DataItem.builder().type(DataType.LONG).value("" + value).build();
	}

	public static DataItem asInteger(Integer value) {
		return DataItem.builder().type(DataType.INTEGER).value("" + value).build();
	}

	public static DataItem asBoolean(Boolean value) {
		return DataItem.builder().type(DataType.BOOLEAN).value("" + value).build();
	}

	public static DataItem asInstant(Instant value) {
		String DTGConverted = DATE_TIME_FORMATTER.format(value);
		return DataItem.builder().type(DataType.INSTANT).value(DTGConverted).build();
	}

	public static DataItem asOnderDateTime(ZonedDateTime value) {
		String DTGConverted = value.format(DATE_TIME_FORMATTER);
		return DataItem.builder().type(DataType.ZONED_DATE_TIME).value(DTGConverted).build();
	}

	public Boolean nullData() {
		return type == DataType.NULLDATA;
	}

	public String stringValue() throws DataItemIncorrectTypeException {
		if (type != DataType.STRING) {
			throw new DataItemIncorrectTypeException("Asked for String, but the data type contains " + type);
		}
		return value;
	}

	public Integer integerValue() throws DataItemIncorrectTypeException, DataItemIncorrectIntegerFormatException {
		if (type != DataType.INTEGER) {
			throw new DataItemIncorrectTypeException("Asked for Integer, but the data type contains " + type);
		}
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			throw new DataItemIncorrectIntegerFormatException(
					"Asked for Integer, but the data type value does not parse as an integer " + value);
		}
	}

	public Long longValue() throws DataItemIncorrectTypeException, DataItemIncorrectLongFormatException {
		if (type != DataType.INTEGER) {
			throw new DataItemIncorrectTypeException("Asked for Long, but the data type contains " + type);
		}
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException e) {
			throw new DataItemIncorrectLongFormatException(
					"Asked for Long, but the data type value does not parse as a long " + value);
		}
	}

	public Boolean booleanValue() throws DataItemIncorrectTypeException, DataItemIncorrectBooleanFormatException {
		if (type != DataType.BOOLEAN) {
			throw new DataItemIncorrectTypeException("Asked for Boolean, but the data type contains " + type);
		}
		try {
			return Boolean.valueOf(value);
		} catch (NumberFormatException e) {
			throw new DataItemIncorrectBooleanFormatException(
					"Asked for Boolean, but the data type value does not parse as a boolean " + value);
		}
	}

	public Instant instantValue() throws DataItemIncorrectTypeException, DataItemIncorrectTimeFormatException {
		if (type != DataType.INSTANT) {
			throw new DataItemIncorrectTypeException("Asked for Instant, but the data type contains " + type);
		}
		try {
			return Instant.parse(value);
		} catch (DateTimeParseException e) {
			throw new DataItemIncorrectTimeFormatException(
					"Can't parse " + value + " because " + e.getLocalizedMessage());
		}
	}

	public ZonedDateTime zonedDateTimeValue()
			throws DataItemIncorrectTypeException, DataItemIncorrectTimeFormatException {
		if (type != DataType.ZONED_DATE_TIME) {
			throw new DataItemIncorrectTypeException("Asked for ZonedDateTime, but the data type contains " + type);
		}
		try {
			return ZonedDateTime.parse(value, DATE_TIME_FORMATTER);
		} catch (DateTimeParseException e) {
			throw new DataItemIncorrectTimeFormatException(
					"Can't parse " + value + " because " + e.getLocalizedMessage());
		}
	}
}
