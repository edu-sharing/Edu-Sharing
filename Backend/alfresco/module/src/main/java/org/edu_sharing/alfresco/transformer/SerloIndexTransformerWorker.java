package org.edu_sharing.alfresco.transformer;

public class SerloIndexTransformerWorker {};
/*public class SerloIndexTransformerWorker extends ContentTransformerHelper implements ContentTransformerWorker {

    Logger logger = Logger.getLogger(SerloIndexTransformerWorker.class);

    NodeService nodeService = null;

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getVersionString() {
        return "1.1";
    }

    @Override
    public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options) {
        logger.debug("called:" +sourceMimetype +" "+targetMimetype + " use:" + options.getUse());
        return options.getSourceNodeRef() != null && AuthenticationUtil.runAsSystem(
                () -> RessourceInfoExecuter.CCM_RESSOURCETYPE_SERLO.equals(nodeService.getProperty(options.getSourceNodeRef(), QName.createQName(RessourceInfoExecuter.CCM_PROP_IO_RESSOURCETYPE))) && targetMimetype.equals("text/plain")
        );
    }

    @Override
    public void transform(ContentReader reader, ContentWriter writer, TransformationOptions options) throws Exception {
        logger.debug("called");


        JSONObject jsonObject = (JSONObject) new JSONParser().parse(reader.getContentString());
        StringBuffer resultString = new StringBuffer();
        traverse(resultString,null, jsonObject);
        writer.putContent(resultString.toString());
    }

    private void traverse(StringBuffer resultString, String key, Object o){
        logger.debug("traversing:"+key);
        if(o instanceof JSONObject){
            for(Object e : ((JSONObject)o).entrySet()){
                Map.Entry entry = (Map.Entry)e;
                traverse(resultString, (String)entry.getKey(), entry.getValue());
            }
        }else if(o instanceof JSONArray){
            JSONArray jarr = (JSONArray)o;
            for(int i = 0; i < jarr.size(); i++){
                traverse(resultString, null, jarr.get(i));
            }
        }else if(o instanceof String){
            if("text".equals(key)){
                if(resultString.length() == 0){
                    resultString.append((String)o);
                }else{
                    resultString.append(" " + o);
                }
            }
        }else if(o instanceof Long){
            //can be the version
        }else {
            logger.warn("unknown class "+o.getClass() +" key:"+key);
        }
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
}*/
