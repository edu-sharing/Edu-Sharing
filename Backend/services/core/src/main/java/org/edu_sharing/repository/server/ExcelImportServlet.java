package org.edu_sharing.repository.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileItemFactory;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.server.importer.ExcelLOMImporter;

public class ExcelImportServlet extends HttpServlet{

	private static Log logger = LogFactory.getLog(ExcelImportServlet.class);
	
	protected void doPost(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
		
		ServletOutputStream out = resp.getOutputStream();
		if (!JakartaServletFileUpload.isMultipartContent(request)){
			out.print("No MultipartContent");
			return;
		}



		DiskFileItemFactory factory = DiskFileItemFactory.builder().get();
		JakartaServletFileUpload upload = new JakartaServletFileUpload(factory);
		
		
		Map<String, String> authInfo = new AuthenticationToolAPI().validateAuthentication(request.getSession());
		
		if(authInfo == null){
			logger.error("not authenticated");
			return;
		}
		try{
			if(!new MCAlfrescoAPIClient().isAdmin()){
				logger.error("not an admin");
				return;
			}
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			return;
		}
		
		try {
			List items = upload.parseRequest(request);
			
			
			
			String parentId = null;
			
			DiskFileItem dfi = null;
			
			
			for (Object item : items) {
				FileItem fileItem = (FileItem) item;
				//logger.info(fileItem.getFieldName()+" "+fileItem.getName()+" "+fileItem.getContentType()+" "+fileItem.getString()+" ");
				if(!fileItem.isFormField()){
					dfi = (DiskFileItem)fileItem;
				}
				
				if(fileItem.getFieldName().equals("parentId")){
					parentId = fileItem.getString();
				}
				
			}
			
			
			if(dfi == null){
				logger.error("no file provided");
				return;
			}
			
			if(parentId == null){
				logger.error("no file provided");
				return;
			}
			
			try{
				new ExcelLOMImporter(parentId, dfi.getInputStream(), false);
			}catch(Exception e){
				e.printStackTrace();
			}

		} catch (FileUploadException e) {
			logger.error(e.getMessage(), e);
			out.print(e.getMessage());
			return;
		}
	}
	
}
