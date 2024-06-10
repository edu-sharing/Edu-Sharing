package org.edu_sharing.alfresco.action;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentStreamListener;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ZipContentIndexer extends ActionExecuterAbstractBase {

	private NodeService nodeService;

	private ContentService contentService;

	private TransactionService transactionService = null;

	private static Log logger = LogFactory.getLog(ZipContentIndexer.class);

	public static final String CCM_ASPECT_RESSOURCEINFO = "{http://www.campuscontent.de/model/1.0}ressourceinfo";
	public static final String CCM_PROP_IO_RESOURCECONTENT = "{http://www.campuscontent.de/model/1.0}ccresourcecontent";

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void executeImpl(Action action,final NodeRef actionedUponNodeRef) {
		Object obj = nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_CONTENT);
		ContentData content = (ContentData) obj;
		logger.info(content.getContentUrl());
		logger.info(content.getMimetype());
		logger.info(content.getSize());
		
		
		//the old method:
		//indexContentFile(actionedUponNodeRef);
		
		//the new one
		indexResourceBody(actionedUponNodeRef);
	}
	
	/**
	 * whatch out for file idx_content.txt and index it
	 * @param zip
	 * @param actionedUponNodeRef
	 */
	private void indexContentFile( NodeRef actionedUponNodeRef){
		
		ZipInputStream zip = null;
		ContentReader contentreader = this.contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
		logger.info(contentreader.getMimetype());
		InputStream is = contentreader.getContentInputStream();

		zip = new ZipInputStream(is);
		try {

			ZipEntry current = null;
			while ((current = zip.getNextEntry()) != null) {

				if (current.getName().equals("idx_content.txt")) {
					logger.info("current.getName():" + current.getName());
					indexQtiContent(zip, actionedUponNodeRef);
					zip.close();
					return;

				}

			}

			zip.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				zip.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param zip
	 * @return
	 */
	private void indexResourceBody(NodeRef actionedUponNodeRef) {

		
		String contentToIdx = "";
		
		ArrayList<InputStream> theInputStreams = new ArrayList<>();
		ArrayList<String> fileNamesToIndex = new ArrayList<>();
		
		try {

			ContentReader contentreader = this.contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
			logger.info("contentreader.getMimetype:"+contentreader.getMimetype());
			logger.info("contentreader.getEncoding:"+contentreader.getEncoding());
			
			InputStream is = contentreader.getContentInputStream();

			theInputStreams.add(is);
			
			//make a copy of the input stream
			
			/*int read = 0;
			int chunk = 0;
			byte[] data = new byte[256];
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			while(-1 != (chunk = is.read(data)))
			{
				read += data.length;
				baos.write(data, 0, chunk);
			}
			
			ByteArrayInputStream copy = new ByteArrayInputStream(baos.toByteArray());*/
			//we work with the copy so we close is
			//ZipInputStream zip = new ZipInputStream(copy);
			
			ZipInputStream zip = new ZipInputStream(is);
			theInputStreams.add(zip);
			// get to index filelist

			
			ZipEntry current = null;

			
			while ((current = zip.getNextEntry()) != null) {

				if (current.getName().equals("imsmanifest.xml")) {
					logger.info("parsing manifest...");

					Document doc = new RessourceInfoTool().loadFromStream(zip);
					XPathFactory pfactory = XPathFactory.newInstance();
					XPath xpath = pfactory.newXPath();
					String resourcePath = "/manifest/resources/resource[contains(@type,'imsqti_assessment_xml') or contains(@type,'imsqti_item_xml')]";
					NodeList resourceNodeList = (NodeList) xpath.evaluate(resourcePath, doc, XPathConstants.NODESET);
					for (int i = 0; i < resourceNodeList.getLength(); i++) {

						String href = (String) xpath.evaluate("@href", resourceNodeList.item(i), XPathConstants.STRING);
						if (href != null) {
							logger.info("adding file to index list:" + href);
							fileNamesToIndex.add(href);
						}
					}
					
					break;

				}

			}
			
			
			
			
			// get to index content
			
			//cause of third party access (xpath stuff) to an stream is closing it we need to get a new one for every file that needs to indexed
			for(String fileNameToIndes:fileNamesToIndex){
				
				//create a New copy
				//ZipInputStream resourceInputStream = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()));
				//theInputStreams.add(resourceInputStream);
				
				//get a new reader
				ContentReader contentreader2 = contentreader.getReader();
				logger.info("contentreader.getMimetype:"+contentreader2.getMimetype());
				logger.info("contentreader.getEncoding:"+contentreader2.getEncoding());
				
				InputStream is2 = contentreader2.getContentInputStream();
			
				ZipInputStream resourceInputStream = new ZipInputStream(is2);
				theInputStreams.add(resourceInputStream);
				

				while ((current = resourceInputStream.getNextEntry()) != null) {
					if (fileNameToIndes.equals(current.getName())) {
						
						
						
						Document doc = new RessourceInfoTool().loadFromStream(resourceInputStream);
						XPathFactory pfactory = XPathFactory.newInstance();
						XPath xpath = pfactory.newXPath();
						
						
						NodeList nodeList =  (NodeList) xpath.evaluate("/assessmentItem/itemBody | /assessmentItem/modalFeedback | /assessmentTest/testPart  | /assessmentTest/testFeedback", doc, XPathConstants.NODESET);
						
						for(int i = 0; i < nodeList.getLength(); i++){
							Node bodyNode = nodeList.item(i);
							Transformer t = TransformerFactory.newInstance().newTransformer();
							t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
							t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
							t.setOutputProperty(OutputKeys.METHOD, "text");
							t.setOutputProperty(OutputKeys.INDENT,"no");
							StringWriter sw = new StringWriter();
							t.transform(new DOMSource(bodyNode), new StreamResult(sw));
							String toIndex = sw.toString();
							logger.info("will index content:" + toIndex);
							
							contentToIdx = contentToIdx + toIndex;
						}
						
						//Node bodyNode = (Node) xpath.evaluate("/assessmentItem/itemBody", doc, XPathConstants.NODE);

						
						
						//we have to do a "break" cause stream is now closed
						break;

					}
				}
				
			
			
			}

			

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			
			logger.info("finally tidy up the Streams");
			try {
				for(InputStream is : theInputStreams){
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Throwable e){
				logger.error("ERROR while tidy up InputStreams");
				e.printStackTrace();
			}
		}
		
		
		
		if(!contentToIdx.trim().equals("")){
			try{
				ByteArrayInputStream in = new ByteArrayInputStream(contentToIdx.getBytes("UTF-8"));
				indexQtiContent(in,actionedUponNodeRef);
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
	
		
	}

	private String convertStreamToString(InputStream is) throws IOException {
		/*
		 * To convert the InputStream to String we use the Reader.read(char[]
		 * buffer) method. We iterate until the Reader return -1 which means
		 * there's no more data to read. We use the StringWriter class to
		 * produce the string.
		 */
		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];

			Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}

			return writer.toString();
		} else {
			return "";
		}
	}

	private void indexQtiContent(final InputStream qTIContent, final NodeRef nodeRef) throws IOException {
		logger.info("started");

		RetryingTransactionCallback callback = new RetryingTransactionCallback() {
			@Override
			public Object execute() throws Throwable {
				logger.info("started");

				final ContentWriter contentWriter = contentService.getWriter(nodeRef, QName.createQName(CCM_PROP_IO_RESOURCECONTENT), true);
				contentWriter.addListener(new ContentStreamListener() {
					@Override
					public void contentStreamClosed() throws ContentIOException {
						logger.info("Content Stream was closed");
						logger.info(" ContentData size:" + contentWriter.getContentData().getSize());
						logger.info(" ContentData URL:" + contentWriter.getContentData().getContentUrl());
						logger.info(" ContentData MimeTyp:" + contentWriter.getContentData().getMimetype());

					}
				});
				contentWriter.setMimetype("text/plain");
				contentWriter.setEncoding("UTF-8");

				// ByteArrayInputStream is = new ByteArrayInputStream(content);

				contentWriter.putContent(qTIContent);
				
			
				
				logger.info("returns");
				return null;
			}
		};

		RetryingTransactionHelper retryingTransactionHelper = transactionService.getRetryingTransactionHelper();
		
		//retryingTransactionHelper.setMaxRetries(10);
		//retryingTransactionHelper.doInTransaction(callback, false,true);
		retryingTransactionHelper.doInTransaction(callback, false);
		logger.info("returns");

	}
	


	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setTransactionService(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

}
