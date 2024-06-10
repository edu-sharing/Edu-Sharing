package org.edu_sharing.service.foldertemplates;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TmplGroup {

	
	List<TGroup> GroupList = new ArrayList<>();

	public TmplGroup (){};

	public List<TGroup> getGroups (){
		return GroupList;
	};
	
	public TmplGroup (Node folder){
        Node g = null;
		NodeList groups = folder.getChildNodes();
		
	   	for (int i = 0, len = groups.getLength(); i < len; i++){
	   		Node currentNode = groups.item(i);
	   		if (currentNode.getNodeType() == Node.ELEMENT_NODE && 
	   				currentNode.getNodeName()=="groups"   ) 
	   		{
	   			g = currentNode;
	   			break;
     	   	}
	   	}
		
		if (g != null){
			NodeList nodeList = g.getChildNodes();
		   	for (int i = 0, len = nodeList.getLength(); i < len; i++){
		   		if (nodeList.item(i).getNodeName()=="group"){
			   		Node group = nodeList.item(i);
			    	String name = group.getAttributes().getNamedItem("name").getNodeValue();
			    	String right = group.getAttributes().getNamedItem("right").getNodeValue();
			    	if (!name.isEmpty() && !right.isEmpty()){
			    	 TGroup tg = new TGroup();
			    	 tg.setName(name);
			    	 tg.setRight(right);
			    	 GroupList.add(tg);
			    	}
		   		}
		    	
		   	}
		}
		
	};
	
	
}
