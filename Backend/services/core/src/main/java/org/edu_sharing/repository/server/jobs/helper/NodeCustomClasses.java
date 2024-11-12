package org.edu_sharing.repository.server.jobs.helper;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * to be used with @BulkEditNodesJob
 */
public class NodeCustomClasses {
    public static class ImportExternalThumbnail implements Consumer<NodeRef> {
        @Override
        public void accept(NodeRef nodeRef) {
            String thumbUrl = NodeServiceHelper.getProperty(nodeRef, CCConstants.CCM_PROP_IO_THUMBNAILURL);
            if (thumbUrl == null) {
                Logger.getLogger(ImportExternalThumbnail.class).info(nodeRef + " has no " + CCConstants.CCM_PROP_IO_THUMBNAILURL);
            }
            try {
                HttpQueryTool.Callback<Throwable> callback = new HttpQueryTool.Callback<Throwable>() {
                    @Override
                    public void handle(InputStream is) {
                        try {
                            NodeServiceFactory.getLocalService().writeContent(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeRef.getId(), is,
                                    "image/jpeg", null, CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW);
                        } catch (Exception e) {
                            Logger.getLogger(ImportExternalThumbnail.class).warn("Thumb fetching failed for " + thumbUrl);
                        }
                    }
                };
                new HttpQueryTool().queryStream(thumbUrl, callback);
                if (callback.getResult() != null) {
                    Logger.getLogger(ImportExternalThumbnail.class).warn("Thumb fetching failed for " + thumbUrl + ": " + callback.getResult());
                } else {
                    NodeServiceHelper.removeProperty(nodeRef, CCConstants.CCM_PROP_IO_THUMBNAILURL);
                    Logger.getLogger(ImportExternalThumbnail.class).info(nodeRef + " thumb imported: " + CCConstants.CCM_PROP_IO_THUMBNAILURL);
                }
            } catch (Throwable t) {
                Logger.getLogger(ImportExternalThumbnail.class).warn("Thumb fetching failed for " + thumbUrl);
            }
        }
    }
}
