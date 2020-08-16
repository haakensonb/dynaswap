package org.openmrs.module.dynaswap.atallah;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Comparator;

import org.openmrs.api.context.Context;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.openmrs.Privilege;
import org.openmrs.Role;

/**
 * CryptDAG
 */
public class CryptDAG {
	
	// private HashMap<String, Set<Privilege>> privList;
	
	/**
	 * constructor
	 */
	public CryptDAG() {
		// this.privList = new HashMap<String, Set<Privilege>>();
		// List<Role> roles = Context.getUserService().getAllRoles();
		// for (Role role : roles) {
		// 	boolean hasKey = this.privList.containsKey(role.getName());
		// 	if (hasKey == false) {
		// 		this.privList.put(role.getName(), Collections.<Privilege> emptySet());
		// 	} else {
		// 		Set<Privilege> privs = role.getPrivileges();
		// 		this.privList.get(role.getName()).addAll(privs);
		// 	}
		// }
		
		// for (String key : this.privList.keySet()) {
		// 	System.out.println("\nROLE: " + key);
		// 	System.out.println(this.privList.get(key).toString());
		// }
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
			ImmutablePair<String, Set<Privilege>> mapping = new ImmutablePair<String,Set<Privilege>>(roleName, privs);
			rolePrivMappings.add(mapping);
		}
		// find all intersections between privileges and generate dummy nodes
		int d = 0;
		for (int i = 0; i < nodePrivs.size(); i++) {
			for (int j = i + 1; j < nodePrivs.size(); j++) {
				Set<Privilege> privs1 = nodePrivs.get(i);
				Set<Privilege> privs2 = nodePrivs.get(j);
				Set<Privilege> intersect = this.getIntersect(privs1, privs2);
				if ((intersect.size() < Math.min(privs1.size(), privs2.size())) && (intersect.size() > 0)
				        && (nodePrivs.contains(intersect) == false)) {
					String dummyName = "Placeholder" + Integer.toString(d);
					ImmutablePair<String, Set<Privilege>> mapping = new ImmutablePair<String, Set<Privilege>>(dummyName, intersect);
					rolePrivMappings.add(mapping);
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

	}
	
	private Set<Privilege> getIntersect(Set<Privilege> setA, Set<Privilege> setB) {
		// retainAll modifies the set so we must use a copy to preserve it
		Set<Privilege> copySetA = new HashSet<Privilege>(setA);
		// retain elements that intersect
		copySetA.retainAll(setB);
		return copySetA;
	}
}
