package org.ihtsdo.snomed.server.dataservice.json;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class OrderedConceptJsonObject extends JSONObject {

	private static final String[] ATTRIBUTE_ORDER = new String[]{"_id", "conceptId", "effectiveTime", "active", "module",
			"definitionStatus", "fsn", "defaultTerm", "isLeafInferred", "isLeafStated", "descriptions"};

	private LinkedHashSet<String> orderedKeys;

	public OrderedConceptJsonObject(String source) throws JSONException {
		super(source);
	}

	@Override
	public String toString() {
		orderKeys();
		return super.toString();
	}

	@Override
	public Iterator keys() {
		if (orderedKeys != null) {
			return orderedKeys.iterator();
		} else {
			return super.keys();
		}
	}

	private void orderKeys() {
		Set<String> keys = keySet();
		LinkedHashSet<String> orderedKeys = new LinkedHashSet<>();
		for (String key : ATTRIBUTE_ORDER) {
			if (keys.contains(key)) {
				orderedKeys.add(key);
			}
		}

		// Add remaining
		for (String key : keys) {
			if (!orderedKeys.contains(key)) {
				orderedKeys.add(key);
			}
		}
		this.orderedKeys = orderedKeys;
	}

}
