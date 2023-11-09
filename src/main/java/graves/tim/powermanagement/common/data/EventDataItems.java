package graves.tim.powermanagement.common.data;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

// we need the getters, setters and the like
@Data
@SuperBuilder
@NoArgsConstructor
public class EventDataItems {
	@Builder.Default
	private Map<String, DataItem> data = new HashMap<>();

	public DataItem addDataItem(String itemName, DataItem item) {
		if (data == null) {
			data = new HashMap<>();
		}
		synchronized (data) {
			return data.put(itemName, item);
		}
	}

	public DataItem addDataItem(String itemName, String item) {
		return addDataItem(itemName, DataItem.asString(item));
	}

	public DataItem addDataItem(String itemName, Boolean item) {
		return addDataItem(itemName, DataItem.asBoolean(item));
	}

	public DataItem addDataItem(String itemName, Long item) {
		return addDataItem(itemName, DataItem.asLong(item));
	}

	public DataItem addDataItem(String itemName, Integer item) {
		return addDataItem(itemName, DataItem.asInteger(item));
	}

	public DataItem addDataItem(String itemName, Instant item) {
		return addDataItem(itemName, DataItem.asInstant(item));
	}

	public DataItem removeDataItem(String itemName) {
		synchronized (data) {
			return data.remove(itemName);
		}
	}

	public boolean hasDataItem(String name) {
		synchronized (data) {
			return data.containsKey(name);
		}
	}

	public DataItem dataItem(String name) throws DataItemNotFoundException {
		synchronized (data) {
			DataItem item = data.get(name);
			if (item == null) {
				throw new DataItemNotFoundException("Cannot locate data item " + name);
			}
			return item;
		}
	}

	public Collection<String> dataItemNamesStartingWith(String prefix) {
		synchronized (data) {
			return data.entrySet().stream().map(entry -> entry.getKey()).filter(name -> name.startsWith(prefix))
					.toList();
		}
	}

	public Map<String, DataItem> allDataItems() {
		synchronized (data) {
			return Collections.unmodifiableMap(data);
		}
	}

	public String stringValue(String name) throws DataItemIncorrectTypeException, DataItemNotFoundException {
		return dataItem(name).stringValue();
	}

	public String stringValue(String name, String defaultValue) throws DataItemIncorrectTypeException {
		try {
			return dataItem(name).stringValue();
		} catch (DataItemNotFoundException e) {
			return defaultValue;
		}
	}

	public Integer integerValue(String name)
			throws DataItemIncorrectTypeException, DataItemNotFoundException, DataItemIncorrectIntegerFormatException {
		return dataItem(name).integerValue();
	}

	public Integer integerValue(String name, Integer defaultValue)
			throws DataItemIncorrectTypeException, DataItemIncorrectIntegerFormatException {
		try {
			return dataItem(name).integerValue();
		} catch (DataItemNotFoundException e) {
			return defaultValue;
		}
	}

	public Long longValue(String name)
			throws DataItemIncorrectTypeException, DataItemNotFoundException, DataItemIncorrectLongFormatException {
		return dataItem(name).longValue();
	}

	public Long longValue(String name, Long defaultValue)
			throws DataItemIncorrectTypeException, DataItemIncorrectLongFormatException {
		try {
			return dataItem(name).longValue();
		} catch (DataItemNotFoundException e) {
			return defaultValue;
		}
	}

	public Boolean booleanValue(String name)
			throws DataItemIncorrectTypeException, DataItemNotFoundException, DataItemIncorrectBooleanFormatException {
		return dataItem(name).booleanValue();
	}

	public Boolean booleanValue(String name, Boolean defaultValue)
			throws DataItemIncorrectTypeException, DataItemIncorrectBooleanFormatException {
		try {
			return dataItem(name).booleanValue();
		} catch (DataItemNotFoundException e) {
			return defaultValue;
		}
	}

	public Instant instantValue(String name)
			throws DataItemIncorrectTypeException, DataItemNotFoundException, DataItemIncorrectTimeFormatException {
		return dataItem(name).instantValue();
	}

	public Instant instantValue(String name, Instant defaultValue)
			throws DataItemIncorrectTypeException, DataItemIncorrectTimeFormatException {
		try {
			return dataItem(name).instantValue();
		} catch (DataItemNotFoundException e) {
			return defaultValue;
		}
	}

}
