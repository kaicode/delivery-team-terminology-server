package org.ihtsdo.snomed.server.api;

import org.ihtsdo.snomed.server.dataservice.ConceptService;
import org.ihtsdo.snomed.server.dataservice.json.JsonComponentMergeException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/concepts/*")
public class ConceptServlet extends HttpServlet {

	public static final String APPLICATION_JSON = "application/json";
	private ConceptService conceptService;

	@Override
	public void init() throws ServletException {
		conceptService = new ConceptService();
	}

	@Override
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
}
