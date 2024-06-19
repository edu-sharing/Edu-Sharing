package org.edu_sharing.repository.server;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.LRMITool;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.json.JSONObject;
import org.springframework.validation.Errors;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class NgErrorServlet extends HttpServlet {
	private static Logger logger = Logger.getLogger(NgErrorServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handleRequest(req, resp);
	}

	private static void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			Object errorMessage= req.getAttribute("jakarta.servlet.error.message");
			Object errorCode= req.getAttribute("jakarta.servlet.error.status_code");
			ErrorFilter.handleError(req, resp, new Throwable(
					errorMessage.toString()),
					Integer.parseInt(errorCode.toString())
			);
		}catch(NullPointerException e) {
			try {
				Throwable t = (Throwable) req.getAttribute("jakarta.servlet.error.exception");
				logger.error(t);
			} catch(Throwable t){
				resp.sendError(500, "Fatal error preparing error.html: "+t.getMessage());
			}
		}catch(Throwable t) {
			logger.error(t);
			resp.sendError(500, "Fatal error preparing error.html: "+t.getMessage());
		}
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String method = req.getMethod();
		//prevent webdav methods 404 is transformed to 501
		if ("PROPFIND".equals(method)
				|| "PROPPATCH".equals(method)
				|| "COPY".equals(method)
				|| "LOCK".equals(method)
				|| "MKCOL".equals(method)
				|| "MOVE".equals(method)
				|| "UNLOCK".equals(method)) {
			handleRequest(req, resp);
		}else {
			super.service(req, resp);
		}

	}
}
