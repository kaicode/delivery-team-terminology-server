package org.ihtsdo.snomed.server.api;

import org.ihtsdo.snomed.server.dataservice.ConceptService;
import org.ihtsdo.snomed.server.dataservice.exception.InvalidOperationException;
import org.ihtsdo.snomed.server.dataservice.json.JsonComponentMergeException;
import org.ihtsdo.snomed.server.dataservice.util.StreamUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;

@WebServlet("/api/concepts/*")
public class ConceptServlet extends HttpServlet {

	public static final String APPLICATION_JSON = "application/json";
	private ConceptService conceptService;

	@Override
	public void init() throws ServletException {
		conceptService = new ConceptService("/tmp/terminology-server-store");
		try {
			conceptService.init();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	@Override
	/**
	 * GET - View concept of concept child list
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		if (pathInfo != null && !pathInfo.isEmpty()) {
			String[] pathParts = pathInfo.split("/");
			if (pathParts.length == 3) {
				String branch = pathParts[1];
				String conceptId = pathParts[2];
				try {
					String concept = conceptService.loadConcept(conceptId, branch);
					response.setContentType(APPLICATION_JSON);
					response.getWriter().print(concept);
				} catch (JsonComponentMergeException e) {
					e.printStackTrace(); // todo: logging
					response.setStatus(500);
				}
			} else if (pathParts.length == 4 && pathParts[3].equals("children")) {
				String branch = pathParts[1];
				String conceptId = pathParts[2];
				try {
					String conceptChildren = conceptService.loadConceptChildren(conceptId, branch);
					response.setContentType(APPLICATION_JSON);
					response.getWriter().print(conceptChildren);
				} catch (JsonComponentMergeException e) {
					e.printStackTrace(); // todo: logging
					response.setStatus(500);
				}
			}
		}
	}

	@Override
	/**
	 * POST - Create concept
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		if (pathInfo != null && !pathInfo.isEmpty()) {
			String[] pathParts = pathInfo.split("/");
			if (pathParts.length == 2) {
				String branch = pathParts[1];

				ServletInputStream inputStream = request.getInputStream();
				String newConcept = StreamUtils.readStream(inputStream);
				try {
					String concept = conceptService.createConcept(newConcept, branch);
					response.setContentType(APPLICATION_JSON);
					response.getWriter().print(concept);
				} catch (InvalidOperationException e) {
					e.printStackTrace(); // todo: logging
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				}
			}
		}
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		if (pathInfo != null && !pathInfo.isEmpty()) {
			String[] pathParts = pathInfo.split("/");
			if (pathParts.length == 3) {
				String branch = pathParts[1];
				String conceptId = pathParts[2];

				ServletInputStream inputStream = request.getInputStream();
				String conceptUpdates = StreamUtils.readStream(inputStream);
				try {
					String concept = conceptService.updateConcept(conceptId, branch, conceptUpdates);
					response.setContentType(APPLICATION_JSON);
					response.getWriter().print(concept);
				} catch (InvalidOperationException e) {
					e.printStackTrace(); // todo: logging
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				} catch (JsonComponentMergeException e) {
					e.printStackTrace(); // todo: logging
					response.setStatus(500);
				}
			}
		}
	}
}
