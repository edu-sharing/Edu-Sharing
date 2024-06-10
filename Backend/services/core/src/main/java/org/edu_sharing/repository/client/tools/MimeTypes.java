/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.client.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MimeTypes {
	public static final String MIME_DIRECTORY="application/x-directory";
	public static final String[] mime_doc = new String[]{"application/pdf", "application/msword", 
														"application/mspowerpoint", "text/xml", 
														"text/rtf", "application/x-zip", 
														"text/plain", "application/vnd.oasis.opendocument.text-template",
														"application/vnd.oasis.opendocument.text-web","application/vnd.oasis.opendocument.text-master", 
														"application/vnd.oasis.opendocument.graphics", "application/vnd.oasis.opendocument.graphics-template", 
														"application/vnd.oasis.opendocument.presentation", "application/vnd.oasis.opendocument.presentation-template", 
														"application/vnd.oasis.opendocument.spreadsheet", "application/vnd.oasis.opendocument.spreadsheet-template", 
														"application/vnd.oasis.opendocument.chart", "application/vnd.oasis.opendocument.formula", 
														"application/vnd.oasis.opendocument.database", "application/vnd.oasis.opendocument.image", 
														"application/vnd.ms-powerpoint", "application/vnd.ms-excel"}; 
	
	public static final String[] mime_pic = new String[]{"image/gif", "image/jpeg", 
														 "image/png", "image/tiff", 
														 "image/bmp", "image/tif", 
														 "image/photoshop","image/xcf",
														 "image/pcx"};
	
	public static final String[] mime_audio = new String[]{ "audio/mid", "audio/x-midi", "audio/x-wav", "audio/x-pn-realaudio",
															"audio/ogg", "audio/x-mpeg", "audio/aiff", "audio/basic", "audio/voxware",
															"audio/x-ms-wma", "audio/x-pn-realaudio", "audio/mpeg"};
	
	public static final String[] mime_video = new String[]{"video/x-msvideo", "video/quicktime", "video/mpeg", "video/x-flash", 
															"video/x-ms-wmv", "video/mp4v-es", "video/3gpp"};

	//private static final Object[] allMime = {mime_doc, mime_pic, mime_audio, mime_video};
	
	static Map<String,String> extensionMimeMap = null;
	
	String basePath;
	
	public MimeTypes(String basePath){
		this.basePath = basePath;
		
		if(this.basePath.endsWith("/")){
			this.basePath = this.basePath.substring(0, this.basePath.length() - 1);
		}
		
		if(!this.basePath.startsWith("http") && !this.basePath.startsWith("/")){
			this.basePath = "/" +this.basePath;
		}
	}
	
	/**
	 * @param wishlist arraylist with String fields that consist mimetype
	 * @return 
	 */
	public static String[] get(ArrayList wishlist){
		Object[] wishListArr = wishlist.toArray();
		return get(wishListArr);
	}
	
	public static String[] get(Object[] wishlist){
		ArrayList tmpArrayList = new ArrayList();
		for(int i = 0; i < wishlist.length; i++){
			String[] tmp = (String[])wishlist[i];
			for(int j = 0; j < tmp.length; j++){
				tmpArrayList.add(tmp[j]);
			}
		}
		String[] returnvalue = new String[tmpArrayList.size()]; 
		for(int i = 0; i < tmpArrayList.size(); i++){
			returnvalue[i] = (String)tmpArrayList.get(i);
		}
		return returnvalue;
	}
	
	public static String getAsString(Object[] wishlist){
		String result ="";
		for(int i = 0; i < wishlist.length; i++){
			String tmp = (String)wishlist[i];
			result += tmp +", ";
		}
		result = (!result.trim().equals(""))? result.substring(0, result.length()-3) : result;
		return result;
	}

	public static Map<String, String> getExtensionMimeMap() {
		
		if(extensionMimeMap == null){
			extensionMimeMap = new HashMap<>();
			extensionMimeMap.put("jpg", "image/jpeg");
			extensionMimeMap.put("jpeg", "image/jpeg");
			extensionMimeMap.put("png", "image/png");
			extensionMimeMap.put("gif", "image/gif");
			extensionMimeMap.put("bmp", "image/bmp");
			extensionMimeMap.put("tiff", "image/tiff");
			extensionMimeMap.put("tif", "image/tif");
			extensionMimeMap.put("psd", "image/photoshop");
			extensionMimeMap.put("xcf", "image/xcf");
			extensionMimeMap.put("pcx", "image/pcx");
			
			extensionMimeMap.put("avi", "video/x-msvideo");
			extensionMimeMap.put("mov", "video/mpeg");
			extensionMimeMap.put("flv", "video/x-flash");
			extensionMimeMap.put("wmv", "video/x-ms-wmv");
			extensionMimeMap.put("mpg", "video/mpeg");
			extensionMimeMap.put("mpeg", "video/mpeg");
			//extensionMimeMap.put("mp4", "video/mp4v-es");
			extensionMimeMap.put("mp4", "video/mp4");
			extensionMimeMap.put("3gp", "video/3gpp");
			
			extensionMimeMap.put("wav", "audio/wav");
			extensionMimeMap.put("mp3", "audio/mpeg");
			extensionMimeMap.put("mid", "audio/mid");
			extensionMimeMap.put("ogg", "audio/ogg");
			extensionMimeMap.put("aif", "audio/aiff");
			extensionMimeMap.put("au", "audio/basic");
			extensionMimeMap.put("vox", "audio/voxware");
			extensionMimeMap.put("wma", "audio/x-ms-wma");
			extensionMimeMap.put("ram", "audio/x-pn-realaudio");
			
			extensionMimeMap.put("odt", "application/vnd.oasis.opendocument.text");
			extensionMimeMap.put("ott", "application/vnd.oasis.opendocument.text-template");
			extensionMimeMap.put("oth", "application/vnd.oasis.opendocument.text-web");
			extensionMimeMap.put("odm", "application/vnd.oasis.opendocument.text-master");
			extensionMimeMap.put("odg", "application/vnd.oasis.opendocument.graphics");
			extensionMimeMap.put("otg", "application/vnd.oasis.opendocument.graphics-template");
			extensionMimeMap.put("odp", "application/vnd.oasis.opendocument.presentation");
			extensionMimeMap.put("otp", "application/vnd.oasis.opendocument.presentation-template");
			extensionMimeMap.put("ods", "application/vnd.oasis.opendocument.spreadsheet");
			extensionMimeMap.put("ots", "application/vnd.oasis.opendocument.spreadsheet-template");
			extensionMimeMap.put("odc", "application/vnd.oasis.opendocument.chart");
			extensionMimeMap.put("odf", "application/vnd.oasis.opendocument.formula");
			extensionMimeMap.put("odb", "application/vnd.oasis.opendocument.database");
			extensionMimeMap.put("odi", "application/vnd.oasis.opendocument.image");
			
			
			extensionMimeMap.put("ppt","application/vnd.ms-powerpoint");
			extensionMimeMap.put("ppz","application/vnd.ms-powerpoint");
			extensionMimeMap.put("pps","application/vnd.ms-powerpoint");
			extensionMimeMap.put("pot","application/vnd.ms-powerpoint");
			
			extensionMimeMap.put("doc","application/msword");
			
			//docx
			extensionMimeMap.put("docm","application/vnd.ms-word.document.macroEnabled.12");
			extensionMimeMap.put("docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document");
			extensionMimeMap.put("dotm","application/vnd.ms-word.template.macroEnabled.12");
			extensionMimeMap.put("dotx","application/vnd.openxmlformats-officedocument.wordprocessingml.template");
			extensionMimeMap.put("ppsm","application/vnd.ms-powerpoint.slideshow.macroEnabled.12");
			extensionMimeMap.put("ppsx","application/vnd.openxmlformats-officedocument.presentationml.slideshow");
			extensionMimeMap.put("pptm","application/vnd.ms-powerpoint.presentation.macroEnabled.12");
			extensionMimeMap.put("pptx","application/vnd.openxmlformats-officedocument.presentationml.presentation");
			extensionMimeMap.put("xlsb","application/vnd.ms-excel.sheet.binary.macroEnabled.12");
			extensionMimeMap.put("xlsm","application/vnd.ms-excel.sheet.macroEnabled.12");
			extensionMimeMap.put("xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
			extensionMimeMap.put("xps ","application/vnd.ms-xpsdocument");
			
		
			extensionMimeMap.put("dot","application/msword");
			
			extensionMimeMap.put("xls", "application/vnd.ms-excel");
										 
			extensionMimeMap.put("txt", "text/plain");		
			
			extensionMimeMap.put("pdf","application/pdf");
			
			extensionMimeMap.put("zip","application/zip");
			
			extensionMimeMap.put("mbz","application/zip");
			
			extensionMimeMap.put("epub","application/epub+zip");
			
			
			extensionMimeMap.put("xml","text/xml");
			
			//apple iworks
			extensionMimeMap.put("pages","application/vnd.apple.pages");
			extensionMimeMap.put("keynote","application/vnd.apple.keynote");
			extensionMimeMap.put("numbers","vnd.apple.numbers");
					   
		}
		return extensionMimeMap;
	}
	
	public static String guessMimetype(String filename){
		return getExtensionMimeMap().get(getExtension(filename));
	}
	
	public static String getExtension(String filename){
		String[] splittedFilename = filename.split("\\.");
		
		String fileExtension = null;
		if(splittedFilename != null && splittedFilename.length > 1){
			fileExtension = splittedFilename[splittedFilename.length - 1];
			if(fileExtension != null) fileExtension = fileExtension.toLowerCase();
		}
		return fileExtension;
	}
	
	
	/**
	 * The mime type - icon mapping stuff
	 */
	
	public static String DEFAULT_MIME_ICON_PATH = "themes/default/images/common/mimetypes/16/file.svg";	
	
	protected static String[][] mimeTypeIconMap = {
		{"application/pdf","file-pdf.svg"},
		{"application/msword","file-word.svg"},
		{"application/vnd.openxmlformats-officedocument.wordprocessingml.document","file-word.svg"},
		{"application/mspowerpoint","file-powerpoint.svg"},
		{"application/vnd.ms-powerpoint","file-powerpoint.svg"},
		{"application/vnd.openxmlformats-officedocument.presentationml.presentation","file-powerpoint.svg"},
		{"application/vnd.ms-excel","file-excel.svg"},
		{"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","file-excel.svg"},
		{"application/zip","file-zip.svg"},
		{"application/rar","file-zip.svg"},
		
		{"application/vnd.oasis.opendocument.text","file-word.svg"},
		{"application/vnd.oasis.opendocument.presentation","file-powerpoint.svg"},
		{"application/vnd.oasis.opendocument.spreadsheet","file-excel.svg"},
	

		{"audio/mpeg","file-audio.svg"},
		{"audio/mid","file-audio.svg"},
		{"audio/mp4a-latm","file-audio.svg"},
		{"audio/x-mpegurl","file-audio.svg"},
		{"audio/x-aiff","file-audio.svg"},
		{"audio/basic","file-audio.svg"},
		{"audio/x-pn-realaudio","file-audio.svg"},
		{"audio/x-wav","file-audio.svg"},
		{"audio/x-ms-wma","file-audio.svg"},
		{"image/gif","file-image.svg"},
		{"image/jpeg","file-image.svg"},
		{"image/png","file-image.svg"},
		{"image/x-png","file-image.svg"},
		{"image/tiff","file-image.svg"},
		{"video/3gpp","file-video.svg"},
		{"video/x-msvideo","file-video.svg"},
		{"video/quicktime","file-video.svg"},
		{"video/mpeg","file-video.svg"},
		{"video/mp4","file-video.svg"},
		{"video/mp4v-es","file-video.svg"},
		{"video/x-dv","file-video.svg"},
		{"video/vnd.mpegurl","file-video.svg"},
		{"video/x-m4v","file-video.svg"},
		{"video/x-flash","file-video.svg"},
		{"video/x-flv","file-video.svg"},
		{"video/x-ms-wmv","file-video.svg"},
		
		{MIME_DIRECTORY,"folder.svg"},
		
		//TODO ccmimetypes
		{CCConstants.CCM_TYPE_MAP,"collection.svg"},
		{CCConstants.CM_TYPE_FOLDER,"collection.svg"},
		{"text/xml","file-txt.svg"},
		{"text/plain","file-txt.svg"},
		{"text/html","file-link.svg"},
		
		//the folloing are no real mimetyps, they specify the type of some zip packages
		{"imsqti","file-qti.svg"},
		{"moodle","file-moodle.svg"},
		{"scorm","file-scorm.svg"},
		{"ADL SCORM","file-scorm.svg"}
	};

	public static String getDefaultPreview(HashMap properties, String moduleBaseUrl){
		String mimeType = (String) properties.get(CCConstants.CCM_PROP_CCRESSOURCETYPE);
		if ((mimeType==null) || (mimeType.length()==0)) mimeType = (String) properties.get(CCConstants.LOM_PROP_TECHNICAL_FORMAT);
		if ((mimeType==null) || (mimeType.length()==0)) mimeType = (String) properties.get(CCConstants.ALFRESCO_MIMETYPE);
		return getDefaultPreview(mimeType, moduleBaseUrl);
	}
	
	public static String getDefaultPreview(String mimeType, String moduleBaseUrl){
		String previewImageURL = null;
		
		if(mimeType != null){
			if(Arrays.asList(MimeTypes.mime_audio).contains(mimeType)){
				previewImageURL = moduleBaseUrl + "/images/preview_default/ksnapshot-audio.svg";
			}
			if(Arrays.asList(MimeTypes.mime_doc).contains(mimeType)){
				previewImageURL = moduleBaseUrl+ "/images/preview_default/ksnapshot-doc.svg";
			}
			if(Arrays.asList(MimeTypes.mime_pic).contains(mimeType)){
				previewImageURL = moduleBaseUrl + "/images/preview_default/ksnapshot-image.svg";
			}
			if(Arrays.asList(MimeTypes.mime_video).contains(mimeType)){
				previewImageURL = moduleBaseUrl + "/images/preview_default/ksnapshot-video.svg";
			}
		}
		if(previewImageURL == null){
			previewImageURL = moduleBaseUrl + "/images/preview_default/ksnapshot-file.svg";
		}
		
		return previewImageURL;
	}
}
