package org.edu_sharing.service.relations;

import java.util.HashMap;
import java.util.Map;

public final class RelationTypeUtil {
    private final static Map<OutputRelationType, OutputRelationType> invertRelationTypeSet = new HashMap<>(){{
        put(OutputRelationType.isBasisFor, OutputRelationType.isBasedOn);
        put(OutputRelationType.hasPart, OutputRelationType.isPartOf);
        put(OutputRelationType.references, OutputRelationType.references);
        put(OutputRelationType.isBasedOn, OutputRelationType.isBasisFor);
        put(OutputRelationType.isPartOf, OutputRelationType.hasPart);
    }};

    private final static Map<InputRelationType, OutputRelationType> convertRelationTypeSet = new HashMap<>(){{
        put(InputRelationType.references, OutputRelationType.references);
        put(InputRelationType.isBasedOn, OutputRelationType.isBasedOn);
        put(InputRelationType.isPartOf, OutputRelationType.isPartOf);
    }};

    public  static OutputRelationType invert(OutputRelationType type){
        return  invertRelationTypeSet.get(type);
    }
    public  static OutputRelationType toOutputType(InputRelationType type){
        return  convertRelationTypeSet.get(type);
    }
}
