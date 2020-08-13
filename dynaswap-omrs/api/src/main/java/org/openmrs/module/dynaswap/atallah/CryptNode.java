package org.openmrs.module.dynaswap;

import org.openmrs.util.Security;

public class CryptNode {
	
	public String name;
	
	// need label as well, or maybe just label instead of name
	private String secret;
	
	/**
	 * constructor
	 */
	public CryptNode(String name) {
		this.name = name;
		this.updateSecret();
	}
	
	public void updateSecret() {
		this.secret = Security.getRandomToken();
	}
	
	public String getSecret() {
		return this.secret;
	}
}
