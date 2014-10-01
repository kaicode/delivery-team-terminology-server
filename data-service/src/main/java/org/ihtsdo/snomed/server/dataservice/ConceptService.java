package org.ihtsdo.snomed.server.dataservice;

import org.ihtsdo.snomed.server.dataservice.exception.InvalidOperationException;
import org.ihtsdo.snomed.server.dataservice.json.JsonComponentMerge;
import org.ihtsdo.snomed.server.dataservice.json.JsonComponentMergeException;
import org.ihtsdo.snomed.server.dataservice.json.OrderedConceptJsonObject;
import org.ihtsdo.snomed.server.dataservice.util.DemoStorePopulator;
import org.ihtsdo.snomed.server.dataservice.util.StreamUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ConceptService {

	public static final String MASTER = "master";

	private final File conceptStore;
	private final JsonComponentMerge jsonComponentMerge;
	private final Set<String> conceptToChildSummaryAttributeMapping;

	public ConceptService(String conceptStorePath) {
		conceptStore = new File(conceptStorePath);

		jsonComponentMerge = new JsonComponentMerge();

		conceptToChildSummaryAttributeMapping = new HashSet<>();
		conceptToChildSummaryAttributeMapping.add("defaultTerm");
		conceptToChildSummaryAttributeMapping.add("definitionStatus");
		conceptToChildSummaryAttributeMapping.add("active");
		conceptToChildSummaryAttributeMapping.add("module");
	}

	public void init() throws IOException, URISyntaxException {
		new DemoStorePopulator(conceptStore).populateStoreWithDemoProduct();
	}

	public String loadConcept(String conceptId, String branch) throws IOException, JsonComponentMergeException {
		String concept = getConceptString(conceptId, MASTER);

		if (!branch.equals(MASTER)) {
			String branchDelta = getConceptString(conceptId, branch);
			if (branchDelta != null) {
				if (concept != null) {
					concept = jsonComponentMerge.mergeComponent(concept, branchDelta);
				} else {
					concept = branchDelta;
				}
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

			// Merge in branch changes. Active
			JSONArray conceptSummaries = new JSONArray(conceptChildren);
			for (int a = 0; a < conceptSummaries.length(); a++) {
				JSONObject childConceptSummary = conceptSummaries.getJSONObject(a);
				String childConceptSummaryId = childConceptSummary.getString("conceptId");
				String childConceptString = getConceptString(childConceptSummaryId, branch);
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

	public String createConcept(String newConcept, String branch) throws InvalidOperationException, IOException {
		// TODO: validateConceptContent(newConcept)
		if (!branch.equals(MASTER)) {
			JSONObject concept = new JSONObject(newConcept);
			String conceptId = UUID.randomUUID().toString();
			concept.put("conceptId", conceptId); // Set new UUID conceptId
			String conceptString = concept.toString();
			persistConcept(branch, conceptId, conceptString);
			return conceptString;
		} else {
			throw new InvalidOperationException("Can not modify the master branch directly.");
		}
	}

	public String updateConcept(String conceptId, String branch, String conceptUpdates) throws InvalidOperationException, IOException, JsonComponentMergeException {
		// TODO: validateUpdateContent(newConcept)
		if (!branch.equals(MASTER)) {
			String conceptDelta = getConceptString(conceptId, branch);
			if (conceptDelta != null) {
				conceptDelta = jsonComponentMerge.mergeComponent(conceptDelta, conceptUpdates);
			} else {
				// Concept does not exist in branch - check it exists in master
				if (conceptExists(conceptId, MASTER)) {
					conceptDelta = conceptUpdates;
				} else {
					throw new InvalidOperationException("Can not modify a concept which does not exist.");
				}
			}
			persistConcept(branch, conceptId, conceptDelta);
			return loadConcept(conceptId, branch);
		} else {
			throw new InvalidOperationException("Can not modify the master branch directly.");
		}
	}

	private void persistConcept(String branch, String conceptId, String conceptString) throws IOException {
		String conceptPath = getConceptPath(conceptId, branch);
		File file = new File(conceptStore, conceptPath);
		file.createNewFile();
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(conceptString);
		}
	}

	private boolean conceptExists(String conceptId, String branch) {
		return new File(conceptStore, getConceptPath(conceptId, branch)).isFile();
	}

	private String getConceptString(String conceptId, String branch) throws IOException {
		return readFileOrNull(new File(conceptStore, getConceptPath(conceptId, branch)));
	}

	private String getConceptChildrenString(String conceptId, String branch) throws IOException {
		return readFileOrNull(new File(conceptStore, getConceptPath(conceptId, branch, "-children")));
	}
	private String getConceptPath(String conceptId, String branch) {
		return getConceptPath(conceptId, branch, "");
	}

	private String getConceptPath(String conceptId, String branch, String postfix) {
		String branchPath = branch;
		if (!branch.equals(MASTER)) {
			branchPath = "branch-deltas/" + branch;
		}
		return branchPath + "/" + conceptId + postfix + ".json";
	}

	private String readFileOrNull(File file) throws IOException {
		if (file.exists()) {
			return StreamUtils.readStream(new FileInputStream(file));
		} else {
			return null;
		}
	}
}
