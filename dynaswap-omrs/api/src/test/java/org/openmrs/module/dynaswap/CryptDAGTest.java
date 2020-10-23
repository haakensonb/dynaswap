package org.openmrs.module.dynaswap;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import org.junit.Test;
import org.openmrs.module.dynaswap.atallah.CryptDAG;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.Role;
import org.openmrs.Privilege;
import org.openmrs.api.context.Context;
import org.openmrs.api.UserService;

public class CryptDAGTest extends BaseModuleContextSensitiveTest {
	
	@Test
	public void CryptDAG_constructor() {
		CryptDAG dag = new CryptDAG();
	}
	
	// @Test
	// public void CryptDAG_priv_equals() {
	// 	Privilege p1 = new Privilege("1");
	// 	Privilege p2 = new Privilege("2");
	// 	boolean eq = p1.equals(p2);
	// 	System.out.println("p1 == p2: " + eq);
	// 	Privilege p3 = new Privilege("1");
	// 	boolean eq2 = p1.equals(p3);
	// 	System.out.println("p1 == p3: " + eq2);
	// 	System.out.println("PrivSet test");
	// 	Set<Privilege> ps = new HashSet<Privilege>();
	// 	ps.add(p1);
	// 	System.out.println("PrivSet contains p1: " + ps.contains(p1));
	// 	System.out.println("PrivSet contains p1 str: " + ps.contains("1"));
	// }
	
	@Test
	public void CryptDAG_model_gen_simple_mapping() {
		UserService us = Context.getUserService();
		// // First clear out existing Roles/Privs
		// List<Role> roles = us.getAllRoles();
		// for (Role role : roles) {
		// 	us.purgeRole(r[CRYPT]ole);
		// }
		// List<Privilege> privs = us.getAllPrivileges();
		// for (Privilege priv : privs) {
		// 	us.purgePrivilege(priv);
		// }
		
		// Add a simple model generation test case consisting of
		// the following role-priv object mappings described in a JSON-like
		// format below. For now, values will be hard coded.
		// In the future it may be useful to create a method to parse JSON into a mapping.
		// {"admin": [0, 1, 2], "nurse": [1, 2], "clerk": [2, 3, 4], "doctor": [1, 2, 4]}
		List<Privilege> privs = new ArrayList<Privilege>();
		for (int i = 0; i < 5; i++) {
			Privilege priv = new Privilege(Integer.toString(i));
			privs.add(priv);
			us.savePrivilege(priv);
		}
		Role admin = new Role("[CRYPT]admin");
		admin.addPrivilege(privs.get(0));
		admin.addPrivilege(privs.get(1));
		admin.addPrivilege(privs.get(2));
		us.saveRole(admin);
		Role nurse = new Role("[CRYPT]nurse");
		nurse.addPrivilege(privs.get(1));
		nurse.addPrivilege(privs.get(2));
		us.saveRole(nurse);
		Role clerk = new Role("[CRYPT]clerk");
		clerk.addPrivilege(privs.get(2));
		clerk.addPrivilege(privs.get(3));
		clerk.addPrivilege(privs.get(4));
		us.saveRole(clerk);
		Role doctor = new Role("[CRYPT]doctor");
		doctor.addPrivilege(privs.get(1));
		doctor.addPrivilege(privs.get(2));
		doctor.addPrivilege(privs.get(4));
		us.saveRole(doctor);
		CryptDAG dag = new CryptDAG();
		System.out.println(dag.getFormattedGraph());
		// // Print new roles/privs
		// List<Role> newRoles = us.getAllRoles();
		// System.out.println("2nd test");
		// for (Role role : newRoles) {
		// 	System.out.println(role);
		// }
		// List<Privilege> newPrivs = us.getAllPrivileges();
		// for (Privilege priv : newPrivs) {
		// 	System.out.println(priv);
		// }
		
	}
}
