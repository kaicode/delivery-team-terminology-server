package org.ihtsdo.snomed.server.dataservice;

import org.ihtsdo.snomed.server.dataservice.json.JsonComponentMerge;
import org.ihtsdo.snomed.server.dataservice.json.JsonComponentMergeException;
import org.ihtsdo.snomed.server.dataservice.json.OrderedConceptJsonObject;
import org.ihtsdo.snomed.server.dataservice.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;

public class ConceptService {

	public static final String MASTER = "master";
	private final JsonComponentMerge jsonComponentMerge;

	public ConceptService() {
		jsonComponentMerge = new JsonComponentMerge();
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
			String branchDelta = getConceptChildrenString(conceptId, getBranchPath(branch));
			if (branchDelta != null) {
				conceptChildren = jsonComponentMerge.mergeComponent(conceptChildren, branchDelta);
			}
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
