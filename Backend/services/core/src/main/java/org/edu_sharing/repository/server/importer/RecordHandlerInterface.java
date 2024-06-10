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
package org.edu_sharing.repository.server.importer;

import org.w3c.dom.Node;

public interface RecordHandlerInterface extends RecordHandlerInterfaceBase {

	void handleRecord(Node nodeRecord, String cursor, String set) throws Throwable;

    /**
     * override this in any Record Handler to control if the peristent handler shall create subojects later
     * @return
     */
    default boolean createSubobjects(){return true;}
}
