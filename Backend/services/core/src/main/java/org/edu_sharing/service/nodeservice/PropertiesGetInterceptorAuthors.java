package org.edu_sharing.service.nodeservice;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.tools.VCardConverter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * interceptor that generates an combined field virtual:all_authors for the frontend including all authors and author_freetext
 */
@Slf4j
public class PropertiesGetInterceptorAuthors extends PropertiesGetInterceptorDefault {
    @Override
    public Map<String, Object> beforeCacheProperties(PropertiesContext context) {
        try {
            Object author = context.getProperties().get(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR);
            if(author == null) {
                author = Collections.emptyList();
            }
            Object freetext = context.getProperties().get(CCConstants.CCM_PROP_AUTHOR_FREETEXT);
            if(freetext == null) {
                freetext = Collections.emptyList();
            }
            List<String> result = Stream.concat(
                    ((freetext instanceof List) ? ((List<String>)freetext) : Collections.singletonList(freetext.toString())).stream(),
                    ((author instanceof List) ? ((List<String>)author) : Collections.singletonList(author.toString())).stream().map(VCardConverter::getNameForVCardString)
            ).filter(StringUtils::isNotBlank).collect(Collectors.toList());

            // fix after DESP-738
            context.getProperties().put("{virtualproperty}all_authors",
                    ValueTool.toMultivalue(result.toArray(String[]::new))
            );
        } catch(Throwable t) {
            log.warn(t.getMessage(), t);
        }
        return context.getProperties();
    }
}
