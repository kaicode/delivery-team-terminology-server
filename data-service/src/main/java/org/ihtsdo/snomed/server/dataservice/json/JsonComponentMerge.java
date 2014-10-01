package org.ihtsdo.snomed.server.dataservice.json;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JsonComponentMerge {

	private Map<String, String> arrayMemberIdentifierKey;

	public JsonComponentMerge() {
		arrayMemberIdentifierKey = new HashMap();
		arrayMemberIdentifierKey.put("descriptions", "descriptionId");
		arrayMemberIdentifierKey.put("statedRelationships", "type.conceptId,target.conceptId");
		arrayMemberIdentifierKey.put("relationships", "type.conceptId,target.conceptId");
	}

	public String mergeComponent(String baseConcept, String deltaConcept) throws JsonComponentMergeException {
		JSONObject base = new OrderedConceptJsonObject(baseConcept);
		JSONObject delta = new JSONObject(deltaConcept);

		JSONObject merged = mergeObjects(base, delta);

		return merged.toString();
	}

	public String mergeChildList(String baseConceptChildren, String deltaConceptChildren) throws JsonComponentMergeException {
		JSONArray base = new JSONArray(baseConceptChildren);
		JSONArray delta = new JSONArray(deltaConceptChildren);

		JSONArray merged = mergeArrays(base, delta, "Concept child list", "conceptId");

		return merged.toString();
	}

	private JSONObject mergeObjects(JSONObject base, JSONObject delta) throws JsonComponentMergeException {
		Set<String> keys = delta.keySet();
		for (String key : keys) {
			Object baseAttribute = getAttributeOrNull(base, key);
			Object deltaAttribute = delta.get(key);
			if (baseAttribute != null) {
				if (baseAttribute.getClass().equals(deltaAttribute.getClass())) {
					if (deltaAttribute instanceof JSONObject) {
						mergeObjects((JSONObject) baseAttribute, (JSONObject) deltaAttribute);
					} else if (deltaAttribute instanceof JSONArray) {
						JSONArray baseArray = (JSONArray) baseAttribute;
						JSONArray deltaArray = (JSONArray) deltaAttribute;
						mergeArrays(baseArray, deltaArray, key, arrayMemberIdentifierKey.get(key));
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

	private JSONArray mergeArrays(JSONArray baseArray, JSONArray deltaArray, String arrayName, String memberIdentifierKey) throws JsonComponentMergeException {
		for (int a = 0; a < deltaArray.length(); a++) {
			Object deltaArrayMemberObject = deltaArray.get(a);
			if (deltaArrayMemberObject instanceof JSONObject) {
				if (memberIdentifierKey == null) {
					throw new JsonComponentMergeException("No array member identifier key available for array, arrayName:" + arrayName);
				}
				JSONObject deltaArrayMember = (JSONObject) deltaArrayMemberObject;
				String memberId = getArrayMemberIdentifier(memberIdentifierKey, deltaArrayMember);
				JSONObject baseArrayMember = getJSONObjectArrayMember(baseArray, memberIdentifierKey, memberId);
				if (baseArrayMember != null) {
					mergeObjects(baseArrayMember, deltaArrayMember);
				} else {
					baseArray.put(deltaArrayMember);
				}
			} else {
				throw new JsonComponentMergeException("Merging of simple array members not yet implemented, arrayName" + arrayName);
			}
		}
		return baseArray;
	}

	private String getArrayMemberIdentifier(String memberIdentifierKey, JSONObject deltaArrayMember) {
		String memberIdentifier = "";
		String[] keys = memberIdentifierKey.split(",");
		for (String key : keys) {
			String[] keyPathParts = key.split("\\.");
			JSONObject relative = deltaArrayMember;
			for (int i = 0; i < keyPathParts.length - 1; i++) {
				relative = relative.getJSONObject(keyPathParts[i]);
			}
			if (!memberIdentifier.isEmpty()) {
				memberIdentifier += "|";
			}
			memberIdentifier += relative.getString(keyPathParts[keyPathParts.length - 1]);
		}

		return memberIdentifier;
	}

	private JSONObject getJSONObjectArrayMember(JSONArray baseArray, String memberIdentifierKey, String memberId) {
		for (int a = 0; a < baseArray.length(); a++) {
			JSONObject jsonObject = baseArray.getJSONObject(a);
			if (memberId.equals(getArrayMemberIdentifier(memberIdentifierKey, jsonObject))) {
				return jsonObject;
			}
		}
		return null;
	}

	private Object getAttributeOrNull(JSONObject jsonObject, String key) {
		return jsonObject.has(key) ? jsonObject.get(key) : null;
	}
}
