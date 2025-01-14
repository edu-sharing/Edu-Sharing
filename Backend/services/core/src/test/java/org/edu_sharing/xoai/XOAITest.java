//package org.edu_sharing.xoai;
//
//import com.lyncode.xml.exceptions.XmlWriteException;
//import org.apache.log4j.BasicConfigurator;
//import io.gdcc.xoai.dataprovider.exceptions.OAIException;
//import io.gdcc.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
//import io.gdcc.xoai.dataprovider.model.ItemIdentifier;
//import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
//import io.gdcc.xoai.model.oaipmh.DeletedRecord;
//import io.gdcc.xoai.model.oaipmh.Granularity;
//import io.gdcc.xoai.model.oaipmh.OAIPMH;
//import org.hamcrest.MatcherAssert;
//import org.hamcrest.core.StringContains;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import javax.xml.stream.XMLStreamException;
//import javax.xml.transform.TransformerConfigurationException;
//import java.util.*;
//
//public class XOAITest {
//
//
//    private EduOai underTest;
//
//    @BeforeEach
//    public void init() throws TransformerConfigurationException {
//        BasicConfigurator.configure();
//        RepositoryConfiguration configuration = new RepositoryConfiguration()
//                .withMaxListIdentifiers(100)
//                .withMaxListSets(100)
//                .withMaxListRecords(100)
//                .withMaxListSets(100)
//                .withAdminEmail("foo@bar.de")
//                .withBaseUrl("http://test.de")
//                .withDeleteMethod(DeletedRecord.NO)
//                .withGranularity(Granularity.Second)
//                .withEarliestDate(new Date(0))
//                .withRepositoryName("test");
//
//        underTest = new EduOai(configuration, "lom", new EduDataHandler() {
//            public ListItemIdentifiersResult getIdentifiers(int offset, int length, String set) {
//                ArrayList<ItemIdentifier> list = new ArrayList<>();
//
//                for (int i = offset; i < offset + length; ++i) {
//                    list.add(new EduItemIdentifier(set + ";node" + i, new Date()));
//                }
//
//                return new ListItemIdentifiersResult(true, list);
//            }
//
//            @Override
//            public List<String> getSets() throws OAIException {
//                return Arrays.asList("set1", "set2");
//            }
//
//            public EduItem getItem(String identifier) {
//                return new EduItem(identifier, "<lom></lom>");
//            }
//
//            @Override
//            public ListItemIdentifiersResult getIdentifiersFrom(int var1, int var2, Date var3, String set) {
//                return null;
//            }
//
//            @Override
//            public ListItemIdentifiersResult getIdentifiersUntil(int var1, int var2, Date var3, String set) {
//                return null;
//            }
//
//            @Override
//            public ListItemIdentifiersResult getIdentifiersFromUntil(int var1, int var2, Date var3, Date var4, String set) {
//                return null;
//            }
//
//        });
//    }
//
//    @Test
//    public void testResumptionToken() throws XMLStreamException, XmlWriteException, OAIException {
//        OAIPMH result = underTest.handleRequest(listRequest("set1"));
//        String xml = EduOai.responseToXML(result);
//        MatcherAssert.assertThat(xml, StringContains.containsString("<identifier>set1;node1</identifier>"));
//        result = underTest.handleRequest(listResumptionRequest("MToxMDB8MjpzZXQxfDM6fDQ6fDU6bG9t"));
//        xml = EduOai.responseToXML(result);
//        MatcherAssert.assertThat(xml, StringContains.containsString("<identifier>set1;node101</identifier>"));
//    }
//
//    @Test
//    public void testIdentity() throws OAIException, XMLStreamException, XmlWriteException {
//        OAIPMH identify = underTest.handleRequest(identifyRequest());
//        String xml = EduOai.responseToXML(identify);
//        MatcherAssert.assertThat(xml, StringContains.containsString("<baseURL>http://test.de</baseURL>"));
//    }
//
//    @Test
//    public void testSets() throws XMLStreamException, XmlWriteException, OAIException {
//        OAIPMH result = underTest.handleRequest(listRequest("set1"));
//        String xml = EduOai.responseToXML(result);
//        MatcherAssert.assertThat(xml, StringContains.containsString("<identifier>set1;node1</identifier>"));
//
//        result = underTest.handleRequest(listRequest("set2"));
//        xml = EduOai.responseToXML(result);
//        MatcherAssert.assertThat(xml, StringContains.containsString("<identifier>set2;node1</identifier>"));
//    }
//
//    private static Map<String, List<String>> identifyRequest() {
//        Map<String, List<String>> requests = new HashMap<>();
//        requests.put("verb", Collections.singletonList("Identify"));
//        return requests;
//    }
//
//    private static Map<String, List<String>> listRequest(String set) {
//        Map<String, List<String>> requests = new HashMap<>();
//        requests.put("verb", Collections.singletonList("ListIdentifiers"));
//        requests.put("metadataPrefix", Collections.singletonList("lom"));
//        requests.put("set", Collections.singletonList(set));
//        return requests;
//    }
//
//    private static Map<String, List<String>> listResumptionRequest(String resumptionToken) {
//        Map<String, List<String>> requests = new HashMap<>();
//        requests.put("verb", Collections.singletonList("ListIdentifiers"));
//        requests.put("resumptionToken", Collections.singletonList(resumptionToken));
//        return requests;
//    }
//
//    private static Map<String, List<String>> listRecordRequest() {
//        Map<String, List<String>> requests = new HashMap<>();
//        requests.put("verb", Collections.singletonList("ListRecords"));
//        requests.put("metadataPrefix", Collections.singletonList("lom"));
//        return requests;
//    }
//
//    private static Map<String, List<String>> getRecordRequest() {
//        Map<String, List<String>> requests = new HashMap<>();
//        requests.put("verb", Collections.singletonList("GetRecord"));
//        requests.put("metadataPrefix", Collections.singletonList("lom"));
//        requests.put("identifier", Collections.singletonList("my-node-id"));
//        return requests;
//    }
//}
