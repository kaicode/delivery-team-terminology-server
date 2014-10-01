package org.ihtsdo.snomed.server.dataservice;

import org.ihtsdo.snomed.server.dataservice.exception.InvalidOperationException;
import org.ihtsdo.snomed.server.dataservice.json.JsonComponentMergeException;
import org.ihtsdo.snomed.server.dataservice.util.StreamUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.nio.file.Files;

public class ConceptServiceTest {

	public static final String AUTHORING_TEAM_BRANCH = "authoring-team";
	private ConceptService conceptService;

	@Before
	public void setUp() throws Exception {
		conceptService = new ConceptService(Files.createTempDirectory(getClass().getName()).toString());
		conceptService.init();
	}

	@Test
	public void testLoadConceptOnMaster() throws Exception {
		String expectedMasterConcept = StreamUtils.readStream(getClass().getResourceAsStream("431371001-master.json"));

		String masterConcept = conceptService.loadConcept("431371001", ConceptService.MASTER);

		JSONAssert.assertEquals(expectedMasterConcept, masterConcept, true);
	}

	@Test
	public void testLoadConceptOnBranch() throws Exception {
		String expectedBranchConcept = StreamUtils.readStream(getClass().getResourceAsStream("431371001-authoring-team-branch.json"));

		String branchConcept = conceptService.loadConcept("431371001", AUTHORING_TEAM_BRANCH);

		JSONAssert.assertEquals(expectedBranchConcept, branchConcept, true);
	}

	@Test
	public void testLoadConceptChildrenOnMaster() throws IOException, JsonComponentMergeException {
		String expectedConceptChildren = StreamUtils.readStream(getClass().getResourceAsStream("410607006-children-master.json"));

		String conceptChildren = conceptService.loadConceptChildren("410607006", ConceptService.MASTER);

		JSONAssert.assertEquals(expectedConceptChildren, conceptChildren, true);
	}

	@Test
	public void testLoadConceptChildrenOnBranch() throws IOException, JsonComponentMergeException {
		String expectedConceptChildren = StreamUtils.readStream(getClass().getResourceAsStream("410607006-children-branch.json"));

		String conceptChildren = conceptService.loadConceptChildren("410607006", AUTHORING_TEAM_BRANCH);

		JSONAssert.assertEquals(expectedConceptChildren, conceptChildren, true);
	}

	@Test
	public void testCreateConceptOnMasterThrows() throws IOException {
		try {
			conceptService.createConcept("", ConceptService.MASTER);
			Assert.fail("Should have thrown InvalidOperationException");
		} catch (InvalidOperationException e) {
			Assert.assertEquals("Can not modify the master branch directly.", e.getMessage());
		}
	}

	@Test
	public void testCreateConcept() throws InvalidOperationException, IOException, JsonComponentMergeException {
		String newConcept = StreamUtils.readStream(getClass().getResourceAsStream("new-concept.json"));
		Assert.assertEquals(342, newConcept.length());

		String concept = conceptService.createConcept(newConcept, AUTHORING_TEAM_BRANCH);
		String conceptId = new JSONObject(concept).getString("conceptId");

		Assert.assertEquals(360, concept.length());

		String loadedConcept = conceptService.loadConcept(conceptId, AUTHORING_TEAM_BRANCH);
		JSONAssert.assertEquals(concept, loadedConcept, true);
	}

	@Test
	public void testUpdateConceptOnBranch() throws IOException, JsonComponentMergeException, InvalidOperationException {
		String expectedBranchConcept = StreamUtils.readStream(getClass().getResourceAsStream("431371001-authoring-team-branch.json"));
		String conceptId = "431371001";
		JSONAssert.assertEquals(expectedBranchConcept, conceptService.loadConcept(conceptId, AUTHORING_TEAM_BRANCH), true);

		String expectedUpdatedBranchConcept = StreamUtils.readStream(getClass().getResourceAsStream("431371001-authoring-team-branch-updated.json"));

		String updatedConcept = conceptService.updateConcept(conceptId, AUTHORING_TEAM_BRANCH, "{ \"module\": \"123\" }");

		JSONAssert.assertEquals(expectedUpdatedBranchConcept, updatedConcept, true);
		JSONAssert.assertEquals(expectedUpdatedBranchConcept, conceptService.loadConcept(conceptId, AUTHORING_TEAM_BRANCH), true);
	}

	@Test
	public void testAddParentUpdatesParentsChildList() throws IOException, JsonComponentMergeException, InvalidOperationException {
		String originalParentChildren = conceptService.loadConceptChildren("105590001", AUTHORING_TEAM_BRANCH);

		conceptService.updateConcept("410980008", AUTHORING_TEAM_BRANCH, "{" +
				"'statedRelationships': [" +
				"	{" +
				"		'module': '900000000000207008'," +
				"		'target': {" +
				"			'conceptId': '105590001'" +
				"		}," +
				"		'groupId': 0," +
				"		'type': {" +
				"			'conceptId': '116680003'" +
				"		}," +
				"		'charType': {" +
				"			'conceptId': '900000000000010007'" +
				"		}" +
				"	}" +
				"]" +
				"}".replace("'", "\""));

		String updatedParentChildren = conceptService.loadConceptChildren("105590001", AUTHORING_TEAM_BRANCH);

		Assert.assertNotEquals(originalParentChildren, updatedParentChildren);
	}

}
