package edu.bu.metcs662.machidahiroaki_final_project;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A class to store the results of performance test.
 * 
 * @author hiroakimachida
 */
public class Results {
	HashMap<String, LinkedHashMap<String, Float>> result = new HashMap<String, LinkedHashMap<String, Float>>();

	/**
	 * Constructor.
	 */
	public Results() {
		super();
		for (String type : HWUtil.SENSOR_TYPES) {
			LinkedHashMap<String, Float> entry = new LinkedHashMap<String, Float>();
			result.put(type, entry);
		}
	}

	/**
	 * @param key Sensor type
	 * @return Map of number of days and average time elapsed.
	 */
	public Map<String, Float> get(String key) {
		return result.get(key);
	}

}
