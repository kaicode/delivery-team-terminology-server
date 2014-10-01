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
	public static final String IS_A_SCTID = "116680003";

	private final File conceptStore;
	private final JsonComponentMerge jsonComponentMerge;
	private final Set<String> childSummaryAttributes;
	private final Set<String> childSummaryUpdateAttributes;

	public ConceptService(String conceptStorePath) {
		conceptStore = new File(conceptStorePath);

		jsonComponentMerge = new JsonComponentMerge();

		childSummaryUpdateAttributes = new HashSet<>();
		childSummaryUpdateAttributes.add("defaultTerm");
		childSummaryUpdateAttributes.add("definitionStatus");
		childSummaryUpdateAttributes.add("active");
		childSummaryUpdateAttributes.add("module");

		childSummaryAttributes = new HashSet<>(childSummaryUpdateAttributes);
		childSummaryAttributes.add("conceptId");
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

			String branchConceptChildren = getConceptChildrenString(conceptId, branch);
			if (branchConceptChildren != null) {
				if (conceptChildren != null) {
					conceptChildren = jsonComponentMerge.mergeChildList(conceptChildren, branchConceptChildren);
				} else {
					conceptChildren = branchConceptChildren;
				}
			}

			if (conceptChildren != null) {
				// Merge in concept changes in branch
				JSONArray conceptSummaries = new JSONArray(conceptChildren);
				for (int a = 0; a < conceptSummaries.length(); a++) {
					JSONObject childConceptSummary = conceptSummaries.getJSONObject(a);
					String childConceptSummaryId = childConceptSummary.getString("conceptId");
					String childConceptString = getConceptString(childConceptSummaryId, branch);
					if (childConceptString != null) {
						// This concept is modified in this branch. Let's apply any relevant changes.
						JSONObject childConcept = new JSONObject(childConceptString);
						for (String attributeName : childSummaryUpdateAttributes) {
							if (childConcept.has(attributeName)) {
								childConceptSummary.put(attributeName, childConcept.get(attributeName));
							}
						}
					}
				}
				conceptChildren = conceptSummaries.toString();
			}
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

			String fullConcept = loadConcept(conceptId, branch);
			updateParentChildLists(conceptId, conceptUpdates, fullConcept, branch);
			return fullConcept;
		} else {
			throw new InvalidOperationException("Can not modify the master branch directly.");
		}
	}

	private void updateParentChildLists(String conceptId, String conceptUpdatesString, String fullConceptString, String branch) throws InvalidOperationException, IOException {
		JSONObject conceptDelta = new JSONObject(conceptUpdatesString);
		if (conceptDelta.has("statedRelationships")) {
			JSONArray statedRelationships = conceptDelta.getJSONArray("statedRelationships");
			for (int a = 0; a < statedRelationships.length(); a++) {
				JSONObject statedRelationship = statedRelationships.getJSONObject(a);
				if (IS_A_SCTID.equals(statedRelationship.getJSONObject("type").getString("conceptId"))) {
					String targetConceptId = statedRelationship.getJSONObject("target").getString("conceptId");
					if (!conceptId.equals(targetConceptId)) {
						String parentConceptChildrenDeltaString = getConceptChildrenString(targetConceptId, branch);
						JSONArray parentConceptChildren;
						boolean childOfParentFound = false;
						if (parentConceptChildrenDeltaString != null) {
							parentConceptChildren = new JSONArray(parentConceptChildrenDeltaString);
							for (int i = 0; !childOfParentFound && i < parentConceptChildren.length(); i++) {
								JSONObject parentConceptChild = parentConceptChildren.getJSONObject(i);
								if (parentConceptChild.getString("conceptId").equals("conceptId")) {
									childOfParentFound = true;
								}
							}
						} else {
							parentConceptChildren = new JSONArray();
						}
						if (!childOfParentFound) {
							// Add child
							JSONObject newChild = new JSONObject();
							copyAttributes(new JSONObject(fullConceptString), newChild, childSummaryAttributes);
							parentConceptChildren.put(newChild);
							persistConceptChildren(branch, targetConceptId, parentConceptChildren.toString());
						}
					} else {
						throw new InvalidOperationException("You can not relate a concept to itself.");
					}
				}
			}
		}
	}

	private void copyAttributes(JSONObject source, JSONObject target, Set<String> attributes) {
		for (String attribute : attributes) {
			if (source.has(attribute)) {
				target.put(attribute, source.get(attribute));
			}
		}
	}

	private void persistConcept(String branch, String conceptId, String conceptString) throws IOException {
		writeFile(conceptString, getConceptPath(conceptId, branch));
	}

	private void persistConceptChildren(String branch, String conceptId, String conceptChildrenString) throws IOException {
		writeFile(conceptChildrenString, getConceptChildrenPath(conceptId, branch));
	}

	private void writeFile(String contents, String path) throws IOException {
		File file = new File(conceptStore, path);
		file.createNewFile();
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(contents);
		}
	}

	private boolean conceptExists(String conceptId, String branch) {
		return new File(conceptStore, getConceptPath(conceptId, branch)).isFile();
	}

	private String getConceptString(String conceptId, String branch) throws IOException {
		return readFileOrNull(new File(conceptStore, getConceptPath(conceptId, branch)));
	}

	private String getConceptChildrenString(String conceptId, String branch) throws IOException {
		return readFileOrNull(new File(conceptStore, getConceptChildrenPath(conceptId, branch)));
	}

	private String getConceptPath(String conceptId, String branch) {
		return getConceptPath(conceptId, branch, "");
	}

	private String getConceptChildrenPath(String conceptId, String branch) {
		return getConceptPath(conceptId, branch, "-children");
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
