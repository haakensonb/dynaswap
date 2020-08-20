package org.openmrs.module.dynaswap.atallah;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.Comparator;

import org.openmrs.api.context.Context;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.Privilege;
import org.openmrs.Role;

/**
 * CryptDAG
 */
public class CryptDAG {
	
	public String formattedInfo;
	
	/**
	 * constructor
	 */
	public CryptDAG() {
		this.createGraph();
	}
	
	public void createGraph() {
		List<Role> roles = Context.getUserService().getAllRoles();
		ArrayList<Set<Privilege>> nodePrivs = new ArrayList<Set<Privilege>>();
		// maybe change role key to use uuid (unique)
		ArrayList<ImmutablePair<String, Set<Privilege>>> rolePrivMappings = new ArrayList<ImmutablePair<String, Set<Privilege>>>();
		for (Role role : roles) {
			// get a list of all the possible privileges
			nodePrivs.add(role.getPrivileges());
			// build role/privilege mapping
			String roleName = role.getName();
			Set<Privilege> privs = role.getPrivileges();
			ImmutablePair<String, Set<Privilege>> mapping = new ImmutablePair<String, Set<Privilege>>(roleName, privs);
			rolePrivMappings.add(mapping);
		}
		// find all intersections between privileges and generate dummy nodes
		int d = 0;
		for (int i = 0; i < nodePrivs.size(); i++) {
			for (int j = i + 1; j < nodePrivs.size(); j++) {
				Set<Privilege> privs1 = nodePrivs.get(i);
				Set<Privilege> privs2 = nodePrivs.get(j);
				Set<Privilege> intersect = SetUtils.getIntersect(privs1, privs2);
				if ((intersect.size() < Math.min(privs1.size(), privs2.size())) && (intersect.size() > 0)
				        && (nodePrivs.contains(intersect) == false)) {
					String dummyName = "Placeholder" + Integer.toString(d);
					ImmutablePair<String, Set<Privilege>> mapping = new ImmutablePair<String, Set<Privilege>>(dummyName,
					        intersect);
					rolePrivMappings.add(mapping);
					nodePrivs.add(intersect);
					d += 1;
				}
			}
		}
		// sort rolePrivMappings by number of privileges in descending order
		Collections.sort(rolePrivMappings, new Comparator<ImmutablePair<String, Set<Privilege>>>() {
			
			public int compare(ImmutablePair<String, Set<Privilege>> p1, ImmutablePair<String, Set<Privilege>> p2) {
				// return p1.getValue().size() - p2.getValue().size();
				return p2.getValue().size() - p1.getValue().size();
			}
		});
		ArrayList<String> nodeNames = new ArrayList<String>();
		for (ImmutablePair<String, Set<Privilege>> p : rolePrivMappings) {
			nodeNames.add(p.getKey());
		}
		System.out.println("nodeNames: " + nodeNames.toString());
		System.out.println("nodePrivs before: " + nodePrivs.toString());
		// sort nodePrivs by number of privileges in descending order
		Collections.sort(nodePrivs, new Comparator<Set<Privilege>>() {
			
			public int compare(Set<Privilege> p1, Set<Privilege> p2) {
				// return p1.size() - p2.size();
				return p2.size() - p1.size();
			}
		});
		System.out.println("nodePrivs after: " + nodePrivs.toString());
		ArrayList<ArrayList<Integer>> adj_mat = new ArrayList<ArrayList<Integer>>(nodePrivs.size());
		// init ArrayList with empty ArrayLists for use later in dfs
		for (int i = 0; i < nodePrivs.size(); i++) {
			adj_mat.add(new ArrayList<Integer>());
		}
		ArrayList<Set<Privilege>> tot = new ArrayList<Set<Privilege>>(nodePrivs.size());
		// init ArrayList with empty sets for use later in dfs
		for (int i = 0; i < nodePrivs.size(); i++) {
			tot.add(Collections.<Privilege> emptySet());
		}
		// System.out.println("before");
		// System.out.println("adj_mat: " + adj_mat.toString());
		// System.out.println("tot: " + tot.toString());
		this.dfs(adj_mat, nodePrivs, tot, 0);
		// System.out.println("after");
		// System.out.println("adj_mat: " + adj_mat.toString());
		// System.out.println("tot: " + tot.toString());
		
		try {
			this.getFormattedGraph(adj_mat, nodePrivs, nodeNames);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		// System.out.println("\nnode: " + nodePrivs.toString());
		// System.out.println("\nmappings: " + rolePrivMappings.toString());
	}
	
	private void dfs(ArrayList<ArrayList<Integer>> adj_mat, ArrayList<Set<Privilege>> nodePrivs,
	        ArrayList<Set<Privilege>> tot, int cur) {
		// System.out.println("Entering dfs with cur: " + Integer.toString(cur));
		if (cur == nodePrivs.size()) {
			return;
		}
		if (tot.get(cur).size() > 0) {
			this.dfs(adj_mat, nodePrivs, tot, cur + 1);
			return;
		}
		
		for (int i = cur + 1; i < nodePrivs.size(); i++) {
			this.dfs(adj_mat, nodePrivs, tot, i);
			// boolean isSubsetOfNodePriv = nodePrivs.get(cur).containsAll(tot.get(i));
			// boolean isSubsetOfTot = tot.get(cur).containsAll(tot.get(i));
			// boolean isSubsetOfNodePriv = tot.get(i).containsAll(nodePrivs.get(cur));
			// boolean isSubsetOfTot = tot.get(i).containsAll(tot.get(cur));
			// boolean isSubsetOfNodePriv = SetUtils.isProperSubset(tot.get(i), nodePrivs.get(cur));
			// boolean isSubsetOfTot = SetUtils.isProperSubset(tot.get(i), tot.get(cur));
			boolean isSubsetOfNodePriv = SetUtils.isProperSubset(nodePrivs.get(cur), tot.get(i));
			boolean isSubsetOfTot = SetUtils.isProperSubset(tot.get(cur), tot.get(i));
			// System.out.println("isSubsetOfNodePriv: " + isSubsetOfNodePriv);
			// System.out.println("isSubsetOfTot: " + isSubsetOfTot);
			if ((isSubsetOfNodePriv == true) && (isSubsetOfTot == false)) {
				// if (isSubsetOfNodePriv == true) {
				// System.out.println("about to add!!!!!!!!!!!!!!!!!!");
				if (!adj_mat.get(cur).contains(i)) {
					System.out.println("adding" + i + "to adj_mat");
					adj_mat.get(cur).add(i);
				}
				// union of tot[cur] and tot[i]
				Set<Privilege> newSet = SetUtils.getIntersect(tot.get(cur), tot.get(i));
				tot.set(cur, newSet);
			}
		}
		Set<Privilege> diffSet = SetUtils.getDifference(nodePrivs.get(cur), tot.get(cur));
		nodePrivs.set(cur, diffSet);
		Set<Privilege> unionSet = SetUtils.getUnion(tot.get(cur), nodePrivs.get(cur));
		tot.set(cur, unionSet);
	}
	
	public void getFormattedGraph(ArrayList<ArrayList<Integer>> adj_mat, ArrayList<Set<Privilege>> nodePrivs,
	        ArrayList<String> nodeNames) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode info = mapper.createObjectNode();
		ObjectNode nodeEdge = mapper.createObjectNode();
		ArrayNode nodes = mapper.createArrayNode();
		ArrayNode edges = mapper.createArrayNode();
		
		for (int i = 0; i < adj_mat.size(); i++) {
			String nodeName = nodeNames.get(i);
			// create json node object
			ObjectNode node = mapper.createObjectNode();
			node.put("id", nodeName);
			node.put("label", nodeName);
			// create json nodeData to hold node
			ObjectNode nodeData = mapper.createObjectNode();
			nodeData.put("data", node);
			nodes.add(nodeData);
			for (int j = 0; j < adj_mat.get(i).size(); j++) {
				// create json edge object
				ObjectNode edge = mapper.createObjectNode();
				int val = adj_mat.get(i).get(j);
				String idName = String.format("e%s%s", i, val);
				edge.put("id", idName);
				String srcName = nodeNames.get(i);
				edge.put("source", srcName);
				String targetName = nodeNames.get(val);
				edge.put("target", targetName);
				// create json edgeData to hold edge
				ObjectNode edgeData = mapper.createObjectNode();
				edgeData.put("data", edge);
				edges.add(edgeData);
			}
		}
		
		nodeEdge.put("nodes", nodes);
		nodeEdge.put("edges", edges);
		info.put("elements", nodeEdge);
		this.formattedInfo = mapper.writeValueAsString(info);
	}
}
