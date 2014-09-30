package org.ihtsdo.snomed.server.json;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;

public class JsonComponentMergeTest {

	private JsonComponentMerge jsonComponentMerge;

	@Before
	public void setup() {
		jsonComponentMerge = new JsonComponentMerge();
	}

	@Test
	public void testMergeReleaseAndDeltaConcept() throws JSONException, IOException, JsonComponentMergeException {
		String releaseConcept = StreamUtils.readStream(getClass().getResourceAsStream("431371001-release.json"));
		String deltaConcept = StreamUtils.readStream(getClass().getResourceAsStream("431371001-delta.json"));
		String expectedMergedConcept = StreamUtils.readStream(getClass().getResourceAsStream("431371001-merged.json"));

		String actualMerged = jsonComponentMerge.mergeConcept(releaseConcept, deltaConcept);

		JSONAssert.assertEquals(expectedMergedConcept, actualMerged, true);
	}

}
