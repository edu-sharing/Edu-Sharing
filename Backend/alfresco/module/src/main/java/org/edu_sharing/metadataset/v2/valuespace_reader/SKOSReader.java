package org.edu_sharing.metadataset.v2.valuespace_reader;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.ValuespaceData;
import org.edu_sharing.metadataset.v2.ValuespaceInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class SKOSReader extends ValuespaceReader{
    private static final String ASSOCIATION_IS_CHILD_OF = "isChildOf";
    private String url;
    private static final Logger logger = Logger.getLogger(SKOSReader.class);

    public SKOSReader(ValuespaceInfo info) {
        super(info);
        // e.g. http://localhost:8000/api/v1/curricula/metadatasets
        Matcher matched = matches("(https?:\\/\\/.*\\/)w3id\\.org\\/.*\\.json");
        if(matched.matches() || ValuespaceInfo.ValuespaceType.SKOS.equals(info.getType())) {
            url = info.getValue();
            if(matched.groupCount() >= 2) {
                logger.info("matched SKOS at " + matched.group(1));
            }
        }
    }

    @Override
    public ValuespaceData getValuespace(String locale) throws Exception {
        JSONObject primary = fetch();
        MetadataKey title = convertEntry(primary, "title", null);
        return new ValuespaceData(title, getValuespace(primary.getJSONArray("hasTopConcept"), null, locale));
    }

    public List<MetadataKey> getValuespace(JSONArray list, MetadataKey parent, String locale) throws Exception {
        List<MetadataKey> result = new ArrayList<>();
        for (int i = 0; i < list.length(); i++) {
            JSONObject entry = list.getJSONObject(i);
            MetadataKey converted = convertEntry(entry, "prefLabel", locale);
            if(parent != null){
                converted.setParent(parent.getKey());
            }
            result.add(converted);
            if(entry.has("narrower")){
                List<MetadataKey> subList = getValuespace(entry.getJSONArray("narrower"), converted, locale);
                result.addAll(subList);
            }
        }
        return result;
    }

    private MetadataKey convertEntry(JSONObject entry, String labelId, String locale) throws JSONException {
        MetadataKey key = new MetadataKey();
        key.setKey(entry.getString("id"));
        String de = entry.getJSONObject(labelId).getString("de");
        key.setCaption(de);
        key.setLocale("de");
        if(locale != null && !"de_DE".equals(locale)) {
            try {
                String[] splitted=locale.split("_");
                key.setCaption(entry.getJSONObject(labelId).getString(splitted[0]));
                key.setLocale(splitted[0]);
            }catch(JSONException ignored) { }
        }
        if(entry.has("notation")) {
            JSONArray notations = entry.getJSONArray("notation");
            List<String> altKeys = new ArrayList<>();
            for(int i = 0; i < notations.length(); i++) {
                altKeys.add(notations.getString(i));
            }
            key.setAlternativeKeys(altKeys);
        }
        if(entry.has("url")) {
            try {
                // newer skos versions
                key.setUrl(entry.getJSONArray("url").getJSONObject(0).getString("id"));
            } catch(JSONException e) {
                key.setUrl(entry.getString("url"));
            }
        }
        for(MetadataKey.MetadataKeyRelated.Relation relation: MetadataKey.MetadataKeyRelated.Relation.values()) {
            if(entry.has(relation.name())) {
                JSONArray related = entry.getJSONArray(relation.name());
                for(int i = 0; i < related.length(); i++) {
                    JSONObject relatedJson = related.getJSONObject(i);
                    MetadataKey.MetadataKeyRelated relatedKey = new MetadataKey.MetadataKeyRelated(relation);
                    relatedKey.setKey(relatedJson.getString("id"));
                    key.addRelated(relatedKey);
                }
            }
        }

        return key;
    }

    JSONObject fetch() throws IOException, JSONException {
        return new JSONObject(ReaderUtils.query(url));
    }

    @Override
    protected boolean supportsUrl() {
        return url != null;
    }
}
