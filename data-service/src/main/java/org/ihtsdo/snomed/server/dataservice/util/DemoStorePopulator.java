package org.ihtsdo.snomed.server.dataservice.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DemoStorePopulator {

	private static final String DEMO_STORE = "demo-store";
	private final File conceptStore;

	public DemoStorePopulator(File conceptStore) {
		this.conceptStore = conceptStore;
	}

	public void populateStoreWithDemoProduct() throws IOException, URISyntaxException {
		Enumeration<URL> en = getClass().getClassLoader().getResources(DEMO_STORE);
		if (en.hasMoreElements()) {
			URL metaInf = en.nextElement();
			URI uri = metaInf.toURI();
			if (uri.toString().startsWith("jar:")) {
				System.out.println("Populating demo store from jar.");
				CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
				URL jar = codeSource.getLocation();
				ZipInputStream zip = new ZipInputStream(jar.openStream());
				ZipEntry zipEntry;
				while((zipEntry = zip.getNextEntry()) != null) {
					if (!zipEntry.isDirectory()) {
						String name = zipEntry.getName();
						if (name.startsWith(DEMO_STORE)) {
							File newFile = new File(conceptStore, name.substring(DEMO_STORE.length() + 1));
							newFile.mkdirs();
							Files.copy(zip, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
					}
				}
			} else {
				File fileMetaInf = new File(uri);
				populateStoreWithDemoProductRecurse(fileMetaInf, fileMetaInf.getAbsolutePath());
			}
		}
	}

	private void populateStoreWithDemoProductRecurse(File file, String absolutePath) throws IOException {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (File file1 : files) {
				populateStoreWithDemoProductRecurse(file1, absolutePath);
			}
		} else {
			String relativePath = file.getAbsolutePath().substring(absolutePath.length());
			File newFile = new File(conceptStore, relativePath);
			newFile.mkdirs();
			Files.copy(getClass().getResourceAsStream("/" + DEMO_STORE + relativePath), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

}
