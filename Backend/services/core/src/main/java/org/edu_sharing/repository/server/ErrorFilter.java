package org.edu_sharing.repository.server;

import com.google.gson.Gson;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.rendering.RenderingException;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.DAOMissingException;
import org.edu_sharing.restservices.DAOSecurityException;
import org.edu_sharing.restservices.DAOValidationException;
import org.edu_sharing.restservices.shared.ErrorResponse;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Optional;


public class ErrorFilter implements Filter {
	public static class ErrorFilterException extends ServletException {
		private int statusCode;
		public ErrorFilterException(Throwable t) {
			super(t);
			this.statusCode = mapStatusCode(t);
		}
		public ErrorFilterException(int statusCode) {
			super();
			this.statusCode = statusCode;
		}

		private int mapStatusCode(Throwable t) {
			DAOException dao = DAOException.mapping(t);
			if(dao instanceof DAOValidationException) {
				return HttpServletResponse.SC_BAD_REQUEST;
			}
			if(dao instanceof DAOSecurityException) {
				return HttpServletResponse.SC_FORBIDDEN;
			}
			if(dao instanceof DAOMissingException) {
				return HttpServletResponse.SC_NOT_FOUND;
			}
			return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}

		public int getStatusCode() {
			return statusCode;
		}

		public void setStatusCode(int statusCode) {
			this.statusCode = statusCode;
		}
	}

	// stores the currently accessing tool type, e.g. CONNECTOR
	public static ThreadLocal<String> accessToolType = new ThreadLocal<>();

	Logger logger = Logger.getLogger(ErrorFilter.class);

	ServletContext context;

	@Override
	public void destroy() {
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		this.context=config.getServletContext();
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		try {
			chain.doFilter(req, res);
		} catch(Throwable t) {
			if (t instanceof RenderingException) {
				logger.error(((RenderingException)t).getTechnicalDetail(), t);
			} else {
				logger.error(t.getMessage(), t);
			}
			int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			if(t instanceof ErrorFilterException) {
				statusCode = ((ErrorFilterException) t).getStatusCode();
			}
			handleError((HttpServletRequest) req, (HttpServletResponse)res, t, statusCode);
		}
	}

	public static void handleError(HttpServletRequest req, HttpServletResponse resp, Throwable t, int statusCode) {
		try {
			if(t != null) {
				boolean isAboutStatusCall = Optional.ofNullable(req.getQueryString())
						.map(x->x.contains("timeoutSeconds"))
						.orElse(false);

				if (isAboutStatusCall) {
					Logger.getLogger(ErrorFilter.class).debug(t.getMessage(), t);
				} else {
					Logger.getLogger(ErrorFilter.class).error(t.getMessage(), t);
				}
			}
			String accept = req.getHeader("accept");
			if (! (accept!=null && accept.toLowerCase().contains("text/html"))) {
				resp.reset();
			}
			ErrorResponse response = new ErrorResponse();
			response.setError(statusCode + "");
			if (Logger.getLogger(ErrorFilter.class).getEffectiveLevel().toInt() <= Level.INFO_INT) {
				response.setMessage(t != null ? t.getMessage() : statusCode+"");
			} else {
				response.setMessage("LogLevel is > INFO");
			}
			resp.setStatus(statusCode);
			if (accept!=null && accept.toLowerCase().contains("text/html")) {
				resp.setContentType("text/html");
				resp.getWriter().print("<head><script nonce=\"" + SecurityHeadersFilter.ngCspNonce.get() + "\">window.location.href=\"" + URLTool.getNgErrorUrl(statusCode + "") + "\";</script></head>");
			} else if(accept!=null && accept.toLowerCase().contains("application/json")) {
				resp.setHeader("Content-Type", "application/json");
				resp.getWriter().print(new Gson().toJson(response));
			}
			else {
				resp.setHeader("Content-Type", "plain/text");
				resp.getWriter().print(response.toString());
			}
		}catch(IOException e){
			Logger.getLogger(ErrorFilter.class).error("Fatal error delivering error to client", e);
		}
	}


}
