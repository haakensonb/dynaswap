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
	
	// private Set<String> getPrivNameSet(Set<Privilege> privSet){
	// 	Set<String> privNames = new HashSet<String>();
	// 	for(Privilege priv : privSet){
	// 		// get privilege names
	// 		privNames.add(priv.getPrivilege());
	// 	}
	// 	return privNames;
	// }
	
	@Transactional
	public void createGraph() {
		List<Role> unfilteredRoles = Context.getUserService().getAllRoles();
		// Filter down to only Roles with the right name prefix.
		List<Role> roles = this.getPrefixFilteredRoles(unfilteredRoles);
		// ArrayList<Set<Privilege>> nodePrivs = new ArrayList<Set<Privilege>>();
		ArrayList<Set<String>> nodePrivs = new ArrayList<Set<String>>();
		HashMap<String, Privilege> privNameToPrivObjMapping = new HashMap<String, Privilege>();
		ArrayList<ImmutablePair<String, Set<String>>> roleNameToPrivNamesMappings = new ArrayList<ImmutablePair<String, Set<String>>>();
		for (Role role : roles) {
			// Get a list of all the possible privileges.
			// nodePrivs.add(role.getPrivileges());
			// Set<String> privNames = this.getPrivNameSet(role.getPrivileges());
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
		
		// print for testing
		System.out.println("roleNameToPrivNamesMappings (node_name): ");
		for (ImmutablePair<String, Set<String>> pair : roleNameToPrivNamesMappings) {
			System.out.println("role: " + pair.getKey());
			for (String s : pair.getValue()) {
				System.out.print(s + ", ");
			}
			System.out.println();
		}
		
		// Sort rolePrivMappings by number of privileges in descending order.
		// Sort roleNameToPrivNamesMappings by number of privileges in descending order.
		// Collections.sort(roleNameToPrivNamesMappings, new Comparator<ImmutablePair<String, Set<String>>>() {
		
		// 	public int compare(ImmutablePair<String, Set<String>> p1, ImmutablePair<String, Set<String>> p2) {
		// 		return p2.getValue().size() - p1.getValue().size();
		// 	}
		// });
		
		// print for testing
		System.out.println("roleNameToPrivNamesMappings (node_name) AFTER SORT: ");
		for (ImmutablePair<String, Set<String>> pair : roleNameToPrivNamesMappings) {
			System.out.println("role: " + pair.getKey());
			for (String s : pair.getValue()) {
				System.out.print(s + ", ");
			}
			System.out.println();
		}
		
		// Print nodePrivs for testing
		System.out.println("nodePrivs before sort: ");
		for (Set<String> np : nodePrivs) {
			for (String p : np) {
				System.out.print(p + " ");
			}
			System.out.println();
		}
		
		// Sort nodePrivs by number of privileges in descending order.
		Collections.sort(nodePrivs, new Comparator<Set<String>>() {
			
			public int compare(Set<String> p1, Set<String> p2) {
				return p2.size() - p1.size();
			}
		});
		
		// Print nodePrivs for testing
		System.out.println("nodePrivs after sort: ");
		for (Set<String> np : nodePrivs) {
			for (String p : np) {
				System.out.print(p + " ");
			}
			System.out.println();
		}
		
		ArrayList<String> nodeNames = new ArrayList<String>();
		for (int i = 0; i < nodePrivs.size(); i++) {
			nodeNames.add("");
		}
		// for (ImmutablePair<String, Set<String>> p : roleNameToPrivNamesMappings) {
		// 	nodeNames.add(p.getKey());
		// }
		for (int i = 0; i < nodePrivs.size(); i++) {
			for (int j = 0; j < nodePrivs.size(); j++) {
				if (roleNameToPrivNamesMappings.get(i).getValue() == nodePrivs.get(j)) {
					nodeNames.set(j, roleNameToPrivNamesMappings.get(i).getKey());
				}
			}
		}
		
		// Print nodeNames for testing
		System.out.println("node names: ");
		for (String s : nodeNames) {
			System.out.print(s + ", ");
		}
		System.out.println();
		
		// Init adjacency matrix.
		ArrayList<ArrayList<Integer>> adj_mat = new ArrayList<ArrayList<Integer>>(nodePrivs.size());
		// Init ArrayList with empty ArrayLists for use later in dfs.
		for (int i = 0; i < nodePrivs.size(); i++) {
			adj_mat.add(new ArrayList<Integer>());
		}
		ArrayList<Set<String>> tot = new ArrayList<Set<String>>(nodePrivs.size());
		// Init ArrayList with empty sets for use later in dfs.
		for (int i = 0; i < nodePrivs.size(); i++) {
			tot.add(Collections.<String> emptySet());
		}
		// Print nodePrivs for testing
		System.out.println("nodePrivs before dfs: ");
		for (Set<String> np : nodePrivs) {
			for (String p : np) {
				System.out.print(p + " ");
			}
			System.out.println();
		}
		
		// Use depth first search to build the adjacency matrix.
		this.dfs(adj_mat, nodePrivs, tot, 0);
		
		// Print nodePrivs for testing
		System.out.println("nodePrivs after dfs: ");
		for (Set<String> np : nodePrivs) {
			for (String p : np) {
				System.out.print(p + " ");
			}
			System.out.println();
		}
		
		// // Print nodePrivs for testing
		// System.out.println("nodePrivs after dfs: ");
		// for (Set<Privilege> np : nodePrivs) {
		// 	for (Privilege p : np) {
		// 		System.out.print(p + " ");
		// 	}
		// 	System.out.println();
		// }
		
		// Print tot for testing
		// System.out.println("tot: ");
		// for (Set<Privilege> sp : tot) {
		// 	for (Privilege p : sp) {
		// 		System.out.print(p + " ");
		// 	}
		// 	System.out.println();
		// }
		// Print adj mat for testing
		System.out.println("adj_mat:");
		String adj_mat_str = "";
		for (int i = 0; i < adj_mat.size(); i++) {
			for (int j = 0; j < adj_mat.get(i).size(); j++) {
				adj_mat_str += adj_mat.get(i).get(j);
				adj_mat_str += " ";
			}
			adj_mat_str += "\n";
		}
		System.out.println(adj_mat_str);
		
		// Try to store special json formatted version of the graph.
		try {
			this.formatGraphAsJson(adj_mat, nodePrivs, nodeNames);
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
		for (int i = 0; i < adj_mat.size(); i++) {
			int row = i;
			for (int j = 0; j < adj_mat.get(i).size(); j++) {
				int val = adj_mat.get(i).get(j);
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
	
	private void dfs(ArrayList<ArrayList<Integer>> adj_mat, ArrayList<Set<String>> nodePrivs, ArrayList<Set<String>> tot,
	        int cur) {
		if (cur == nodePrivs.size()) {
			return;
		}
		if (tot.get(cur).size() > 0) {
			this.dfs(adj_mat, nodePrivs, tot, cur + 1);
			return;
		}
		
		for (int i = cur + 1; i < nodePrivs.size(); i++) {
			this.dfs(adj_mat, nodePrivs, tot, i);
			boolean isSubsetOfNodePriv = SetUtils.isProperSubset(tot.get(i), nodePrivs.get(cur));
			boolean isSubsetOfTotCur = SetUtils.isProperSubset(tot.get(i), tot.get(cur));
			if (isSubsetOfNodePriv && (!isSubsetOfTotCur)) {
				if (!adj_mat.get(cur).contains(i)) {
					adj_mat.get(cur).add(i);
				}
				// union of tot[cur] and tot[i]
				Set<String> newSet = SetUtils.getUnion(tot.get(cur), tot.get(i));
				tot.set(cur, newSet);
			}
		}
		
		System.out.println("cur: " + cur);
		System.out.println("tot before diff: ");
		for (Set<String> sp : tot) {
			for (String p : sp) {
				System.out.print(p + " ");
			}
			System.out.println();
		}
		
		// Testing set diff
		System.out.println("setDiff input setA:");
		for (String s : nodePrivs.get(cur)) {
			System.out.print(s + " ");
		}
		System.out.println();
		// Testing set diff
		System.out.println("setDiff input setB:");
		for (String s : tot.get(cur)) {
			System.out.print(s + " ");
		}
		System.out.println();
		
		Set<String> diffSet = SetUtils.getDifference(nodePrivs.get(cur), tot.get(cur));
		// Testing set diff
		System.out.println("nodePriv set before diff:");
		for (String s : nodePrivs.get(cur)) {
			System.out.print(s + " ");
		}
		System.out.println();
		
		System.out.println("diffSet after diff:");
		for (String s : diffSet) {
			System.out.print(s + " ");
		}
		System.out.println();
		
		nodePrivs.set(cur, diffSet);
		// System.out.println("nodePriv set after diff in arraylist:");
		// for (String s : nodePrivs.get(cur)) {
		// 	System.out.print(s + " ");
		// }
		// System.out.println();
		
		Set<String> unionSet = SetUtils.getUnion(tot.get(cur), nodePrivs.get(cur));
		tot.set(cur, unionSet);
	}
	
	public void formatGraphAsJson(ArrayList<ArrayList<Integer>> adj_mat, ArrayList<Set<String>> nodePrivs,
	        ArrayList<String> nodeNames) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode info = mapper.createObjectNode();
		ObjectNode nodeEdge = mapper.createObjectNode();
		ArrayNode nodes = mapper.createArrayNode();
		ArrayNode edges = mapper.createArrayNode();
		
		for (int i = 0; i < adj_mat.size(); i++) {
			String nodeName = nodeNames.get(i);
			// Create json node object.
			ObjectNode node = mapper.createObjectNode();
			node.put("id", nodeName);
			node.put("label", nodeName);
			// Create json nodeData to hold node.
			ObjectNode nodeData = mapper.createObjectNode();
			nodeData.put("data", node);
			nodes.add(nodeData);
			for (int j = 0; j < adj_mat.get(i).size(); j++) {
				// Create json edge object.
				ObjectNode edge = mapper.createObjectNode();
				int val = adj_mat.get(i).get(j);
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
