package org.ihtsdo.snomed.server.json;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JsonComponentMerge {

	private Map<String, String> arrayMemberIdentifiers;

	public JsonComponentMerge() {
		arrayMemberIdentifiers = new HashMap();
		arrayMemberIdentifiers.put("descriptions", "descriptionId");
	}

	public String mergeConcept(String baseConcept, String deltaConcept) throws JsonComponentMergeException {
		JSONObject base = new JSONObject(baseConcept);
		JSONObject delta = new JSONObject(deltaConcept);

		JSONObject merged = mergeObjects(base, delta);

		return merged.toString();
	}

	private JSONObject mergeObjects(JSONObject base, JSONObject delta) throws JsonComponentMergeException {
		Set<String> keys = delta.keySet();
		for (String key : keys) {
			Object baseAttribute = base.get(key);
			Object deltaAttribute = delta.get(key);
			if (baseAttribute != null) {
				if (baseAttribute.getClass().equals(deltaAttribute.getClass())) {
					if (deltaAttribute instanceof JSONObject) {
						mergeObjects((JSONObject) baseAttribute, (JSONObject) deltaAttribute);
					} else if (deltaAttribute instanceof JSONArray) {
						JSONArray baseArray = (JSONArray) baseAttribute;
						JSONArray deltaArray = (JSONArray) deltaAttribute;
						String memberIdentifierKey = null;
						for (int a = 0; a < deltaArray.length(); a++) {
							Object deltaArrayMemberObject = deltaArray.get(a);
							if (deltaArrayMemberObject instanceof JSONObject) {
								if (memberIdentifierKey == null) {
									memberIdentifierKey = arrayMemberIdentifiers.get(key);
									if (memberIdentifierKey == null) {
										throw new JsonComponentMergeException("No array member identifier attribute name set for array, arrayName:" + key);
									}
								}
								JSONObject deltaArrayMember = (JSONObject) deltaArrayMemberObject;
								String memberId = deltaArrayMember.getString(memberIdentifierKey);
								JSONObject baseArrayMember = getJSONObjectArrayMember(baseArray, memberIdentifierKey, memberId);
								if (baseArrayMember != null) {
									mergeObjects(baseArrayMember, deltaArrayMember);
								} else {
									baseArray.put(deltaArrayMember);
								}
							} else {
								throw new JsonComponentMergeException("Merging of simple array members not yet implemented, arrayName" + key);
							}
						}
					} else {
						base.put(key, deltaAttribute);
					}
				} else {
					throw new JsonComponentMergeException("Delta has attribute of different type, key:" + key);
				}
			} else {
				base.put(key, deltaAttribute);
			}
		}
		return base;
	}

	private JSONObject getJSONObjectArrayMember(JSONArray baseArray, String memberIdentifierKey, String memberId) {
		for (int a = 0; a < baseArray.length(); a++) {
			JSONObject jsonObject = baseArray.getJSONObject(a);
			if (memberId.equals(jsonObject.getString(memberIdentifierKey))) {
				return jsonObject;
			}
		}
		return null;
	}

}
