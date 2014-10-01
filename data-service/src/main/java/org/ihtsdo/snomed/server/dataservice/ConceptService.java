package org.ihtsdo.snomed.server.dataservice;

import org.ihtsdo.snomed.server.dataservice.json.JsonComponentMerge;
import org.ihtsdo.snomed.server.dataservice.json.JsonComponentMergeException;
import org.ihtsdo.snomed.server.dataservice.json.OrderedConceptJsonObject;
import org.ihtsdo.snomed.server.dataservice.util.StreamUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class ConceptService {

	public static final String MASTER = "master";

	private final JsonComponentMerge jsonComponentMerge;
	private final Set<String> conceptToChildSummaryAttributeMapping;

	public ConceptService() {
		jsonComponentMerge = new JsonComponentMerge();

		conceptToChildSummaryAttributeMapping = new HashSet<>();
		conceptToChildSummaryAttributeMapping.add("defaultTerm");
		conceptToChildSummaryAttributeMapping.add("definitionStatus");
		conceptToChildSummaryAttributeMapping.add("active");
		conceptToChildSummaryAttributeMapping.add("module");
	}

	public String loadConcept(String conceptId, String branch) throws IOException, JsonComponentMergeException {
		String concept = getConceptString(conceptId, MASTER);

		if (!branch.equals(MASTER)) {
			String branchDelta = getConceptString(conceptId, getBranchPath(branch));
			if (branchDelta != null) {
				concept = jsonComponentMerge.mergeComponent(concept, branchDelta);
			}
		} else {
			// Order attributes of non-merged concept
			// todo - perform ordering on concept import to prevent this
			concept = new OrderedConceptJsonObject(concept).toString();
		}
		return concept;
	}

	public String loadConceptChildren(String conceptId, String branch) throws IOException, JsonComponentMergeException {
		String conceptChildren = getConceptChildrenString(conceptId, MASTER);

		if (!branch.equals(MASTER)) {
			String branchPath = getBranchPath(branch);

			// Merge in branch changes. Active
			JSONArray conceptSummaries = new JSONArray(conceptChildren);
			for (int a = 0; a < conceptSummaries.length(); a++) {
				JSONObject childConceptSummary = conceptSummaries.getJSONObject(a);
				String childConceptSummaryId = childConceptSummary.getString("conceptId");
				String childConceptString = getConceptString(childConceptSummaryId, branchPath);
				if (childConceptString != null) {
					// This concept is modified in this branch. Let's apply any relevant changes.
					JSONObject childConcept = new JSONObject(childConceptString);
					for (String attributeName : conceptToChildSummaryAttributeMapping) {
						if (childConcept.has(attributeName)) {
							childConceptSummary.put(attributeName, childConcept.get(attributeName));
						}
					}
				}
			}
			conceptChildren = conceptSummaries.toString();
		}
		return conceptChildren;
	}

	private String getBranchPath(String branch) {
		return "branch-deltas/" + branch;
	}

	private String getConceptString(String conceptId, String branchPath) throws IOException {
		return readStreamOrNull(getClass().getResourceAsStream("/demo-store/" + branchPath + "/" + conceptId + ".json"));
	}

	private String getConceptChildrenString(String conceptId, String branchPath) throws IOException {
		return readStreamOrNull(getClass().getResourceAsStream("/demo-store/" + branchPath + "/" + conceptId + "-children.json"));
	}

	private String readStreamOrNull(InputStream inputStream) throws IOException {
		if (inputStream != null) {
			return StreamUtils.readStream(inputStream);
		} else {
			return null;
		}
	}
}
