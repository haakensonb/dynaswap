package org.openmrs.module.dynaswap.atallah;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.Comparator;

import org.openmrs.api.context.Context;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Privilege;
import org.openmrs.Role;

/**
 * CryptDAG
 */
public class CryptDAG {
	
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
				return p1.getValue().size() - p2.getValue().size();
			}
		});
		ArrayList<String> nodeNames = new ArrayList<String>();
		for (ImmutablePair<String, Set<Privilege>> p : rolePrivMappings) {
			nodeNames.add(p.getKey());
		}
		// sort nodePrivs by number of privileges in descending order
		Collections.sort(nodePrivs, new Comparator<Set<Privilege>>() {
			
			public int compare(Set<Privilege> p1, Set<Privilege> p2) {
				return p1.size() - p2.size();
			}
		});
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
		this.dfs(adj_mat, nodePrivs, tot, 0);
		this.getFormattedGraph(adj_mat, nodePrivs, nodeNames);
		// System.out.println("adj_mat: " + adj_mat.toString());
		// System.out.println("\nnode: " + nodePrivs.toString());
		// System.out.println("\nmappings: " + rolePrivMappings.toString());
	}
	
	private void dfs(ArrayList<ArrayList<Integer>> adj_mat, ArrayList<Set<Privilege>> nodePrivs,
	        ArrayList<Set<Privilege>> tot, int cur) {
		System.out.println("Entering dfs with cur: " + Integer.toString(cur));
		if (cur == nodePrivs.size()) {
			return;
		}
		if (tot.get(cur).size() > 0) {
			this.dfs(adj_mat, nodePrivs, tot, cur + 1);
			return;
		}
		
		for (int i = cur + 1; i < nodePrivs.size(); i++) {
			this.dfs(adj_mat, nodePrivs, tot, i);
			boolean isSubsetOfNodePriv = nodePrivs.get(cur).containsAll(tot.get(i));
			boolean isSubsetOfTot = tot.get(cur).containsAll(tot.get(i));
			if (isSubsetOfNodePriv && !isSubsetOfTot) {
				if (!adj_mat.get(cur).contains(i)) {
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
	        ArrayList<String> nodeNames) {
		// There must be a better way to format nested HashMaps for json,
		// or some way to get python's dict functionality for this.
		// Maybe formatting as custom objects and then converting?
		HashMap<String, HashMap<String, ArrayList<HashMap<String, HashMap<String, String>>>>> formattedInfo = new HashMap<String, HashMap<String, ArrayList<HashMap<String, HashMap<String, String>>>>>();
		// use format as specified in cytoscape-dagre
		formattedInfo.put("elements", new HashMap<String, ArrayList<HashMap<String, HashMap<String, String>>>>());
		formattedInfo.get("elements").put("nodes", new ArrayList<HashMap<String, HashMap<String, String>>>());
		formattedInfo.get("elements").put("edges", new ArrayList<HashMap<String, HashMap<String, String>>>());
		
		for (int i = 0; i < adj_mat.size(); i++) {
			// create node
			HashMap<String, HashMap<String, String>> node = new HashMap<String, HashMap<String, String>>();
			HashMap<String, String> nodeData = new HashMap<String, String>();
			nodeData.put("id", nodeNames.get(i));
			nodeData.put("label", nodeNames.get(i));
			node.put("data", nodeData);
			formattedInfo.get("elements").get("nodes").add(node);
			for (int j = 0; j < adj_mat.get(i).size(); j++) {
				// create edge
				HashMap<String, HashMap<String, String>> edge = new HashMap<String, HashMap<String, String>>();
				HashMap<String, String> edgeData = new HashMap<String, String>();
				int val = adj_mat.get(i).get(j);
				edgeData.put("id", "e" + i + val);
				edgeData.put("source", nodeNames.get(i));
				edgeData.put("target", nodeNames.get(val));
				edge.put("data", edgeData);
				formattedInfo.get("elements").get("edges").add(edge);
			}
		}
		
		System.out.println(formattedInfo.toString());
	}
}
