package org.edu_sharing.service.stream;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.commons.lang3.NotImplementedException;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.stream.model.ContentEntry;
import org.edu_sharing.service.stream.model.ContentEntry.Audience.STATUS;
import org.edu_sharing.service.stream.model.ScoreResult;
import org.edu_sharing.service.stream.model.StreamSearchRequest;
import org.edu_sharing.service.stream.model.StreamSearchResult;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;


public class StreamServiceElasticsearchImpl implements StreamService {

	public StreamServiceElasticsearchImpl() {
		if(client!=null) {
			return;
		}

		RestClient restClient = RestClient.builder(SearchServiceElastic.getConfiguredHosts()).build();
		ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
		client= new ElasticsearchClient(transport);

		try {
			String mapping = "{\n"+
				"\"properties\": {\n"+
					"\"created\": { \"type\": \"date\" },\n"+
					"\"modified\": { \"type\": \"date\" },\n"+
					"\"title\": { \"type\": \"keyword\" },\n"+
					"\"description\": { \"type\": \"text\" },\n"+
					"\"priority\": { \"type\": \"integer\" },\n"+
					"\"audience\": { \"type\": \"nested\" },\n"+
					"\"properties\": {\n"+
						"\"authority\": { \"type\": \"keyword\" },\n"+
						"\"status\": { \"type\": \"keyword\" }\n"+
					"}\n"+
				"}\n"+
			"}";
			client.indices().create(req->req
					.index(INDEX_NAME)
					.mappings(m->m.withJson(new StringReader(mapping))));

		}catch(Exception e) {
			// index already exists
			// throw new RuntimeException("Elastic search init failed",e);
		}
	}
	private static String INDEX_NAME="entry_index22";
	private static Time SCROLL_TIME=Time.of(t->t.time("1m"));
	private static ElasticsearchClient client;

	@Override
	public String addEntry(ContentEntry entry) throws Exception {
		IndexResponse result = client.index(req -> req
				.index(INDEX_NAME)
				.document(entry));
		return result.id();
	}

	@Override
	public void updateEntry(ContentEntry entry) throws Exception {
		client.update(req->req
				.index(INDEX_NAME)
				.doc(entry)
				, Map.class);
	}

	public StreamSearchResult searchScroll(String scrollId) throws Exception {
		ScrollResponse<ContentEntry> searchResult = client.scroll(req -> req.scrollId(scrollId).scroll(SCROLL_TIME), ContentEntry.class);
		return responseToStreamResult(searchResult);
	}
	@Override
	public boolean canAccessNode(final List<String> authorities,String nodeId) throws Exception {
		SearchResponse<Map> searchResult = client.search(req -> req
						.index(INDEX_NAME)
						.size(0)
						.query(q -> q.bool(b -> {
							getAuthorityQuery(b, authorities, null);
							b.must(must -> must.match(match -> match.field("nodeId.keyword").query(nodeId)));
							return b;
						}))
				, Map.class);

		return searchResult.hits().total() != null && searchResult.hits().total().value() > 0;
	}

	private BoolQuery.Builder getAuthorityQuery(BoolQuery.Builder main, List<String> authorities, ContentEntry.Audience.STATUS status) {
		final BoolQuery.Builder query = new BoolQuery.Builder().minimumShouldMatch("1");
		for(String a : authorities) {
			query.should(should->should.bool(bool -> {
				bool.must(must -> must.match(match -> match.field("audience.authority").query(a)));
				if(status!=null) {
					bool.must(must->must.match(match->match.field("audience.status").query(status.toString())));
				}
				return bool;
			}));
		}

		BoolQuery.Builder exclude = new BoolQuery.Builder().minimumShouldMatch("1");
		for(STATUS type : ContentEntry.Audience.STATUS.values()) {
			if(type.equals(status)) {
				continue;
			}
			exclude.should(should -> should.bool(bool -> bool
					.must(must->must.match(match->match.field("audience.authority").query(authorities.get(0))))
					.must(must->must.match(match->match.field("audience.status").query(type.toString())))));
		}

		main.must(must->must.nested(nested->nested.path("audience").query(q->q.bool(query.build())).scoreMode(ChildScoreMode.None)));
		if(status!=null) {
			main.mustNot(mustNot->mustNot.nested(nested->nested.path("audience").query(q->q.bool(exclude.build())).scoreMode(ChildScoreMode.None)));
		}

		return main;
	}

	@Override
	public ContentEntry.Audience.STATUS getStatus(String entryId,List<String> authorities) throws Exception {
		ContentEntry source = getEntryRequest(entryId).source();
		if(source ==  null){
			return null;
		}

		List<ContentEntry.Audience> audience = source.audience;
		return audience.stream()
				.filter(x->authorities.contains(x.authority))
				.map(x->x.status)
				.findFirst()
				.orElse(null);
	}

	@Override
	public ContentEntry getEntry(String entryId) throws Exception {
		return getEntryRequest(entryId).source();
	}

	private GetResponse<ContentEntry> getEntryRequest(String entryId) throws IOException {
		return client.get(req->req.index(INDEX_NAME).id(entryId), ContentEntry.class);
	}

	@Override
	public ScoreResult getScoreByAuthority(String authority,ContentEntry.Audience.STATUS status) throws Exception {
		SearchResponse<Map> searchResult = client.search(req->req
				.index(INDEX_NAME)
						.size(0)
						.aggregations("score", Aggregation.of(agg->agg.sum(sum->sum.field("score"))))
						.query(query->query.bool(bool->getAuthorityQuery(bool, Collections.singletonList(authority),status)))
		,Map.class);
		ScoreResult result=new ScoreResult();
		result.score=(long)searchResult.aggregations().get("score").sum().value();
		return result;
	}
	@Override
	public void delete(String id) throws Exception {
		client.delete(req->req.index(INDEX_NAME).id(id));
	}

	@Override
	public void deleteEntriesByAuthority(String username) {
		//@TODO this is not supported in the current api version
		// we need to update the api
		// https://www.elastic.co/guide/en/elasticsearch/client/java-rest/6.5/java-rest-high-document-delete-by-query.html
		//DeleteByQueryRequest request = new DeleteByQueryRequest();
		//client.delete(request);
		throw new NotImplementedException("deleteEntriesByAuthority");
	}

	@Override
	public void updateStatus(String id,String authority,ContentEntry.Audience.STATUS status) throws Exception {
		String script="boolean updated=false; if(ctx._source.audience == null) {ctx._source.audience=new ArrayList();}  for (item in ctx._source.audience) {if (item!=null && item.authority == params.audience.authority) {item.status = params.audience.status;updated=true;}} if(!updated){ctx._source.audience.add(params.audience);}";
		//script="boolean updated=false; if(ctx._source.audience == null) {ctx._source.audience=new ArrayList();}  if(!updated){ctx._source.audience.add(params.audience.authority+'='+params.audience.status);}";
		Map<String, Object> audience = Map.of(
				"authority", authority,
				"status", status
		);

		client.update(req->req
                        .index(INDEX_NAME)
                        .id(id)
						.script(scp->scp.source(script).params("audience", JsonData.of(audience)))
                , Map.class);
	}
	@Override
	public StreamSearchResult search(StreamSearchRequest request) throws Exception {
		if(request.authority==null) {
			throw new IllegalArgumentException("StreamSearchRequest is missing authority");
		}

		BoolQuery.Builder query = new BoolQuery.Builder()
				.must(must-> must.bool(bool->getAuthorityQuery(bool, request.authority, request.status)));

		if(request.properties!=null) {
			for(String key : request.properties.keySet()) {
				query.must(must->must.match(match -> match.field("properties."+key).query(request.properties.get(key))));
			}
		}

		if(request.search!=null) {
			query.must(must->must.match(match->match.field("description").field("title").query(request.search)));
		}

		SearchResponse<ContentEntry> searchResult = client.search(req-> {
			req.index(INDEX_NAME)
					.size(request.size)
					.from(request.offset)
					.query(q -> q.bool(query.build()));

					if(request.sortDefinition!=null && request.sortDefinition.hasContent()){
						for(SortDefinition.SortDefinitionEntry sort : request.sortDefinition.getSortDefinitionEntries()){
							req.sort(s->s.field(field->field.field(sort.getProperty()).order(sort.isAscending() ? SortOrder.Asc : SortOrder.Desc)));
						}
					}
					else {
						req.sort(SortOptions.of(opt->opt.field(field->field.field("priority").order(SortOrder.Desc))));
						req.sort(SortOptions.of(opt->opt.field(field->field.field("created").order(SortOrder.Desc))));
					}
			return req;
		}
				, ContentEntry.class);
		return responseToStreamResult(searchResult);
	}
	private static StreamSearchResult responseToStreamResult(ResponseBody<ContentEntry> searchResult) {
		StreamSearchResult result=new StreamSearchResult();
		result.scrollId=searchResult.scrollId();
		result.results=searchResult.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
		result.total=searchResult.hits().total() != null ? searchResult.hits().total().value() : 0;
		return result;
	}

	@Override
	public Map<String, Number> getTopValues(String property) throws Exception {
		List<StringTermsBucket> buckets = client.search(req -> req
						.index(INDEX_NAME)
						.size(0)
						.aggregations("agg", agg -> agg.terms(term -> term.field("properties." + property + ".keyword").valueType(ValueType.String.toString())))
				, Map.class)
				.aggregations()
				.get("agg")
				.sterms()
				.buckets()
				.array();


        return buckets.stream().collect(Collectors.toMap(x->x.key().stringValue(), MultiBucketBase::docCount));
	}
}
