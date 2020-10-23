package org.openmrs.module.dynaswap.atallah;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.Comparator;

import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.openmrs.api.db.hibernate.DbSession;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.module.dynaswap.api.DynaSWAPBaseModuleService;
import org.openmrs.module.dynaswap.api.dao.DynaSWAPBaseModuleDao;
import org.openmrs.module.dynaswap.api.impl.DynaSWAPBaseModuleServiceImpl;

/**
 * CryptDAG
 */

public class CryptDAG {
	
	private String formattedGraph;
	
	private DynaSWAPBaseModuleService dynaService;
	
	// Some core Roles can't be deleted but we don't always want to include them
	// in the hierarchy. Instead only consider Roles with this prefix.
	// HACK: Change this so it isn't hard coded. Or find some other way to specify roles.
	private static final String ROLE_PREFIX = "[CRYPT]";
	
	/**
	 * constructor
	 */
	public CryptDAG() {
		this.dynaService = Context.getService(DynaSWAPBaseModuleService.class);
		this.createGraph();
	}
	
	@Transactional
	public void createGraph() {
		List<Role> unfilteredRoles = Context.getUserService().getAllRoles();
		// Filter down to only Roles with the right name prefix.
		List<Role> roles = this.getPrefixFilteredRoles(unfilteredRoles);
		
		ArrayList<Set<String>> nodePrivs = new ArrayList<Set<String>>();
		HashMap<String, Privilege> privNameToPrivObjMapping = new HashMap<String, Privilege>();
		ArrayList<ImmutablePair<String, Set<String>>> roleNameToPrivNamesMappings = new ArrayList<ImmutablePair<String, Set<String>>>();
		
		for (Role role : roles) {
			// Get a list of all the possible privileges.
			Set<String> privNames = new HashSet<String>();
			for (Privilege priv : role.getPrivileges()) {
				// Get privilege names
				privNames.add(priv.getPrivilege());
				// Also populate mapping from privName to actual PrivObject
				privNameToPrivObjMapping.put(priv.getPrivilege(), priv);
			}
			nodePrivs.add(privNames);
			// Build role/privilege mapping.
			String roleName = role.getName();
			// Set<Privilege> privs = role.getPrivileges();
			ImmutablePair<String, Set<String>> mapping = new ImmutablePair<String, Set<String>>(roleName, privNames);
			roleNameToPrivNamesMappings.add(mapping);
		}
		
		// Find all intersections between privileges and generate dummy nodes.
		int d = 0;
		for (int i = 0; i < nodePrivs.size(); i++) {
			for (int j = i + 1; j < nodePrivs.size(); j++) {
				Set<String> privs1 = nodePrivs.get(i);
				Set<String> privs2 = nodePrivs.get(j);
				Set<String> intersect = SetUtils.getIntersect(privs1, privs2);
				if ((intersect.size() < Math.min(privs1.size(), privs2.size())) && (intersect.size() > 0)
				        && (nodePrivs.contains(intersect) == false)) {
					String dummyName = "Placeholder" + Integer.toString(d);
					ImmutablePair<String, Set<String>> mapping = new ImmutablePair<String, Set<String>>(dummyName, intersect);
					roleNameToPrivNamesMappings.add(mapping);
					nodePrivs.add(intersect);
					d += 1;
				}
			}
		}
		
		// Sort nodePrivs by number of privileges in descending order.
		Collections.sort(nodePrivs, new Comparator<Set<String>>() {
			
			public int compare(Set<String> p1, Set<String> p2) {
				return p2.size() - p1.size();
			}
		});
		
		ArrayList<String> nodeNames = new ArrayList<String>();
		for (int i = 0; i < nodePrivs.size(); i++) {
			nodeNames.add("");
		}
		// Fill nodeNames using correct ordering
		for (int i = 0; i < nodePrivs.size(); i++) {
			for (int j = 0; j < nodePrivs.size(); j++) {
				if (roleNameToPrivNamesMappings.get(i).getValue() == nodePrivs.get(j)) {
					nodeNames.set(j, roleNameToPrivNamesMappings.get(i).getKey());
				}
			}
		}
		
		// Init adjacency matrix.
		ArrayList<ArrayList<Integer>> adjMat = new ArrayList<ArrayList<Integer>>(nodePrivs.size());
		// Init ArrayList with empty ArrayLists for use later in dfs.
		for (int i = 0; i < nodePrivs.size(); i++) {
			adjMat.add(new ArrayList<Integer>());
		}
		
		// Init tot (total privileges) with empty sets for use later in dfs.
		ArrayList<Set<String>> tot = new ArrayList<Set<String>>(nodePrivs.size());
		for (int i = 0; i < nodePrivs.size(); i++) {
			tot.add(Collections.<String> emptySet());
		}
		
		// Use depth first search to build the adjacency matrix.
		this.dfs(adjMat, nodePrivs, tot, 0);
		
		// Try to store special json formatted version of the graph.
		try {
			this.formatGraphAsJson(adjMat, nodePrivs, nodeNames);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		// Delete any previous CryptNode/CryptEdge objects from an old hierarchy
		// NOTE: Once enc/dec functionality is added, the old hierarchy must decrypt all
		// 		 data before deleting hierarchy information.
		int result = this.dynaService.deleteAllCryptNodeEdgeData();
		System.out.println("Delete results: " + Integer.toString(result));
		
		// Create CryptNode objects and store in HashMap.
		HashMap<String, CryptNode> nodeMapping = new HashMap<String, CryptNode>();
		for (int i = 0; i < nodePrivs.size(); i++) {
			String name = nodeNames.get(i);
			CryptNode node = new CryptNode(name);
			nodeMapping.put(name, node);
		}
		// Create CryptEdge objects and assign to proper CryptNode objects.
		for (int i = 0; i < adjMat.size(); i++) {
			int row = i;
			for (int j = 0; j < adjMat.get(i).size(); j++) {
				int val = adjMat.get(i).get(j);
				String parentName = nodeNames.get(row);
				CryptNode parentNode = nodeMapping.get(parentName);
				String childName = nodeNames.get(val);
				CryptNode childNode = nodeMapping.get(childName);
				CryptEdge edge = new CryptEdge(parentNode.getName(), childNode.getName(), parentNode.getDeriveKey(),
				        childNode.getLabel(), childNode.getDeriveKey(), childNode.getDecryptKey());
				nodeMapping.get(parentName).edges.add(edge);
			}
		}
		for (HashMap.Entry<String, CryptNode> entry : nodeMapping.entrySet()) {
			this.dynaService.saveCryptNode(entry.getValue());
		}
		System.out.println("\nnodeMapping: " + nodeMapping.toString());
		
	}
	
	private void dfs(ArrayList<ArrayList<Integer>> adjMat, ArrayList<Set<String>> nodePrivs, ArrayList<Set<String>> tot,
	        int cur) {
		if (cur == nodePrivs.size()) {
			return;
		}
		if (tot.get(cur).size() > 0) {
			this.dfs(adjMat, nodePrivs, tot, cur + 1);
			return;
		}
		
		for (int i = cur + 1; i < nodePrivs.size(); i++) {
			this.dfs(adjMat, nodePrivs, tot, i);
			boolean isSubsetOfNodePriv = SetUtils.isProperSubset(tot.get(i), nodePrivs.get(cur));
			boolean isSubsetOfTotCur = SetUtils.isProperSubset(tot.get(i), tot.get(cur));
			if (isSubsetOfNodePriv && (!isSubsetOfTotCur)) {
				if (!adjMat.get(cur).contains(i)) {
					adjMat.get(cur).add(i);
				}
				// union of tot[cur] and tot[i]
				Set<String> newSet = SetUtils.getUnion(tot.get(cur), tot.get(i));
				tot.set(cur, newSet);
			}
		}
		
		Set<String> diffSet = SetUtils.getDifference(nodePrivs.get(cur), tot.get(cur));
		nodePrivs.set(cur, diffSet);
		
		Set<String> unionSet = SetUtils.getUnion(tot.get(cur), nodePrivs.get(cur));
		tot.set(cur, unionSet);
	}
	
	public void formatGraphAsJson(ArrayList<ArrayList<Integer>> adjMat, ArrayList<Set<String>> nodePrivs,
	        ArrayList<String> nodeNames) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode info = mapper.createObjectNode();
		ObjectNode nodeEdge = mapper.createObjectNode();
		ArrayNode nodes = mapper.createArrayNode();
		ArrayNode edges = mapper.createArrayNode();
		
		for (int i = 0; i < adjMat.size(); i++) {
			String nodeName = nodeNames.get(i);
			// Create json node object.
			ObjectNode node = mapper.createObjectNode();
			node.put("id", nodeName);
			node.put("label", nodeName);
			// Create json nodeData to hold node.
			ObjectNode nodeData = mapper.createObjectNode();
			nodeData.put("data", node);
			nodes.add(nodeData);
			for (int j = 0; j < adjMat.get(i).size(); j++) {
				// Create json edge object.
				ObjectNode edge = mapper.createObjectNode();
				int val = adjMat.get(i).get(j);
				String idName = String.format("e%s%s", i, val);
				edge.put("id", idName);
				String srcName = nodeNames.get(i);
				edge.put("source", srcName);
				String targetName = nodeNames.get(val);
				edge.put("target", targetName);
				// Create json edgeData to hold edge.
				ObjectNode edgeData = mapper.createObjectNode();
				edgeData.put("data", edge);
				edges.add(edgeData);
			}
		}
		
		nodeEdge.put("nodes", nodes);
		nodeEdge.put("edges", edges);
		info.put("elements", nodeEdge);
		this.formattedGraph = mapper.writeValueAsString(info);
	}
	
	public String getFormattedGraph() {
		return this.formattedGraph;
	}
	
	private void printAdjMat(ArrayList<ArrayList<Integer>> adjMat) {
		// Print adj mat for testing
		System.out.println("adjMat:");
		String adjMatStr = "";
		for (int i = 0; i < adjMat.size(); i++) {
			for (int j = 0; j < adjMat.get(i).size(); j++) {
				adjMatStr += adjMat.get(i).get(j);
				adjMatStr += " ";
			}
			adjMatStr += "\n";
		}
		System.out.println(adjMatStr);
	}
	
	private List<Role> getPrefixFilteredRoles(List<Role> unfilteredRoles) {
		List<Role> roles = new ArrayList<Role>();
		for (Role role : unfilteredRoles) {
			if (role.getRole().startsWith(CryptDAG.ROLE_PREFIX)) {
				roles.add(role);
			}
		}
		return roles;
	}
}
