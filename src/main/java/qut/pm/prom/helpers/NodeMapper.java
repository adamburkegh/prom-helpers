package qut.pm.prom.helpers;

import java.util.HashMap;
import java.util.Map;

import org.processmining.models.graphbased.NodeID;

public class NodeMapper {

	private Map<NodeID,String> node2id = new HashMap<>();
	private Map<String,NodeID> id2node = new HashMap<>();
	
	public void put(NodeID nodeId, String id) {
		node2id.put(nodeId, id);
		id2node.put(id, nodeId);
	}
	
	public NodeID getNode(String id) {
		return id2node.get(id);
	}

	public String getId(NodeID nodeId) {
		return node2id.get(nodeId);
	}

	
}
