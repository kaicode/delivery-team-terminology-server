package org.ihtsdo.snomed.server.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamUtils {
	static String readStream(InputStream releaseStream) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(releaseStream));
		String line;
		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line).append(System.lineSeparator());
		}
		return stringBuilder.toString();
	}
}
