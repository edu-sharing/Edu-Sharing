package org.edu_sharing.service.foldertemplates;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

public class LoggingErrorHandler implements ErrorHandler {

    private boolean isValid = true;
    
    List<String> message = new ArrayList<>(); 

    public boolean isValid() {
        return this.isValid;
    }

    @Override
    public void warning(SAXParseException exc) {
    	message.add("Error"+exc.getMessage()+" Line:"+exc.getLineNumber());
//    	System.out.println("Error"+exc.getMessage()+" Line:"+exc.getLineNumber());
    	// log info
        // valid or not?
    }

    @Override
    public void error(SAXParseException exc) {
        // log info
    	message.add("Error"+exc.getMessage()+" Line:"+exc.getLineNumber());
  //      System.out.println("Error"+exc.getMessage()+" Line:"+exc.getLineNumber());
        this.isValid = false;
    }

    @Override
    public void fatalError(SAXParseException exc) throws SAXParseException {
        // log info
    	message.add("Error"+exc.getMessage()+" Line:"+exc.getLineNumber());
    //    System.out.println("Error"+exc.getMessage()+" Line:"+exc.getLineNumber());
        this.isValid = false;
        throw exc;
    }
    
    
    public List<String> getMessage() {
		return message;
	}
}
