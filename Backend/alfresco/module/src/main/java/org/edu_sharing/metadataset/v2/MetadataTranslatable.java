package org.edu_sharing.metadataset.v2;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
@Getter
@Setter
public abstract class MetadataTranslatable implements Serializable{
	private String i18n;
	private String i18nPrefix;

	/**
	 * deep copy the current object instance
	 * @return
	 * @throws Throwable 
	 */
		public <T extends MetadataTranslatable> T copyInstance() throws Throwable {
			T obj = null;
	        try {
	            // Write the object out to a byte array
	            ByteArrayOutputStream bos = new ByteArrayOutputStream();
	            ObjectOutputStream out = new ObjectOutputStream(bos);
	            out.writeObject(this);
	            out.flush();
	            out.close();

	            // Make an input stream from the byte array and read
	            // a copy of the object back in.
	            ObjectInputStream in = new ObjectInputStream(
	                new ByteArrayInputStream(bos.toByteArray()));
	            obj = (T) in.readObject();
	        }
	        catch(Throwable t) {
	        	throw t;
	        }
	        return obj;
		}
}
