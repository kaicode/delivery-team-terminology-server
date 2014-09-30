package org.ihtsdo.snomed.server.dataservice;

import org.ihtsdo.snomed.server.dataservice.json.JsonComponentMerge;
import org.ihtsdo.snomed.server.dataservice.json.JsonComponentMergeException;
import org.ihtsdo.snomed.server.dataservice.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;

public class ConceptService {

	private final JsonComponentMerge jsonComponentMerge;

	public ConceptService() {
		jsonComponentMerge = new JsonComponentMerge();
	}

	public String loadConcept(String id, String branch) throws IOException, JsonComponentMergeException {
		String concept = getConceptString(id, "master");

		if (!branch.equals("master")) {
			String branchDelta = getConceptString(id, "branch-deltas/" + branch);
			if (branchDelta != null) {
				concept = jsonComponentMerge.mergeConcept(concept, branchDelta);
			}
		}
		return concept;
	}

	private String getConceptString(String id, String branchPath) throws IOException {
		InputStream stream = getClass().getResourceAsStream("/demo-store/" + branchPath + "/" + id + ".json");
		if (stream != null) {
			return StreamUtils.readStream(stream);
		}
		return null;
	}

}
