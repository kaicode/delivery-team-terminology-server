package org.ihtsdo.snomed.server.dataservice.json;

import org.ihtsdo.snomed.server.dataservice.util.StreamUtils;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.Date;

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

		long start = new Date().getTime();
		String actualMerged = jsonComponentMerge.mergeComponent(releaseConcept, deltaConcept);
		long millisTaken = new Date().getTime() - start;
		System.out.println("Merge took " + millisTaken + " milliseconds.");

		JSONAssert.assertEquals(expectedMergedConcept, actualMerged, true);
	}

}
