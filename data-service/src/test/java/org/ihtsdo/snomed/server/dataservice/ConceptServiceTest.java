package org.ihtsdo.snomed.server.dataservice;

import org.ihtsdo.snomed.server.dataservice.util.StreamUtils;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class ConceptServiceTest {

	private ConceptService conceptService;

	@Before
	public void setUp() throws Exception {
		conceptService = new ConceptService();
	}

	@Test
	public void testLoadConceptOnMaster() throws Exception {
		String expectedMasterConcept = StreamUtils.readStream(getClass().getResourceAsStream("431371001-master.json"));

		String masterConcept = conceptService.loadConcept("431371001", "master");

		JSONAssert.assertEquals(expectedMasterConcept, masterConcept, true);
	}

	@Test
	public void testLoadConceptOnBranch() throws Exception {
		String expectedMasterConcept = StreamUtils.readStream(getClass().getResourceAsStream("431371001-authoring-team-branch.json"));

		String masterConcept = conceptService.loadConcept("431371001", "authoring-team");

		JSONAssert.assertEquals(expectedMasterConcept, masterConcept, true);
	}
}