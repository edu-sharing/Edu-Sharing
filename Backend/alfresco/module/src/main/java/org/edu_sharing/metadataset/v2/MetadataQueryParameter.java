package org.edu_sharing.metadataset.v2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class MetadataQueryParameter implements Serializable {
	// the used syntax, inherited by the group of queries
	private final String syntax;
	private String name;
	private Map<String,String> statements;
	private boolean multiple;
	private boolean exactMatching = true;
	private String multiplejoin;
	private int ignorable;
	private List<MetadataQueryFacet> facets;
	private String preprocessor;
	private boolean mandatory = true;
	//only DSL
	private boolean asFilter = true;

	public MetadataQueryParameter(String syntax){
		this.syntax = syntax;
	}

	public String getSyntax() {
		return syntax;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getStatements() {
		return statements;
	}
	public String getStatement(String value) {
		String statement=null;
		if(statements!=null) {
			if(statements.containsKey(value))
				statement=statements.get(value);
			else if(statements.get(null)!=null)
				statement=statements.get(null);
		}
		if(statement==null) {
			statement = getDefaultStatement();
		}
		return QueryUtils.replaceCommonQueryParams(statement, QueryUtils.replacerFromSyntax(syntax, true));
	}
	private String getDefaultStatement() {
		if(syntax.equals(MetadataReader.QUERY_SYNTAX_DSL)){
			//return "{\"wildcard\":{\"properties." + name  +"\":{\"value\":\"${value}\"}}}";
			try {

				JSONObject jsonObject = new JSONObject();
				JSONObject termObject = new JSONObject();
				JSONObject detailObject = new JSONObject();
				jsonObject.put("wildcard", termObject);
				termObject.put("properties." + name + ".keyword", detailObject);
				detailObject.put("case_insensitive", true);
				detailObject.put("value", "*${value}*");
				return jsonObject.toString();
			}catch(JSONException e){
				throw new RuntimeException(e);
			}
		} else if(syntax.equals(MetadataReader.QUERY_SYNTAX_LUCENE)) {
			return "@" + name.replace(":", "\\:") + ":\"*${value}*\"";
		}
		throw new RuntimeException("Unsupported syntax for query language: " + syntax);
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MetadataQueryParameter){
			MetadataQueryParameter other=(MetadataQueryParameter)obj;
			return (other.name.equals(name));
		}
		return super.equals(obj);
	}

	public void setStatements(Map<String, String> statements) {
		this.statements = statements;
	}

	public boolean isMultiple() {
		return multiple;
	}

	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
	}

	public String getMultiplejoin() {
		return multiplejoin;
	}

	public void setMultiplejoin(String multiplejoin) {
		this.multiplejoin = multiplejoin;
	}

	public int getIgnorable() {
		return ignorable;
	}

	public void setIgnorable(int ignorable) {
		this.ignorable = ignorable;
	}

	public boolean isExactMatching() {
		return exactMatching;
	}

	public void setExactMatching(boolean exactMatching) {
		this.exactMatching = exactMatching;
	}


    public void setFacets(List<MetadataQueryFacet> facets) {
        this.facets = facets;
    }

    public List<MetadataQueryFacet> getFacets() {
        return facets;
    }

    public void setPreprocessor(String preprocessor) {
        this.preprocessor = preprocessor;
    }

    public String getPreprocessor() {
        return preprocessor;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isMandatory() {
        return mandatory;
    }

	public void setAsFilter(boolean asFilter) { this.asFilter = asFilter; }

	public boolean isAsFilter() { return asFilter; }

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class MetadataQueryFacet implements Serializable {
		String value;
		String nested;
	}
}
