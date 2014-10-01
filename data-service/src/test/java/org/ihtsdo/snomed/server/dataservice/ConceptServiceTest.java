package org.ihtsdo.snomed.server.dataservice;

import org.ihtsdo.snomed.server.dataservice.json.JsonComponentMergeException;
import org.ihtsdo.snomed.server.dataservice.util.StreamUtils;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;

public class ConceptServiceTest {

	private ConceptService conceptService;

	@Before
	public void setUp() throws Exception {
		conceptService = new ConceptService();
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

		String branchConcept = conceptService.loadConcept("431371001", "authoring-team");

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

		String conceptChildren = conceptService.loadConceptChildren("410607006", "authoring-team");

		JSONAssert.assertEquals(expectedConceptChildren, conceptChildren, true);
	}

}