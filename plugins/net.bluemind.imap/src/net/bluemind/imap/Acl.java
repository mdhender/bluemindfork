/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap;

/**
 * Cyrus ACL sets.
 * 
 * The ACL may be one of the special strings "none", "read" ("lrs"), "post"
 * ("lrsp"), "append" ("lrsip"), "write" ("lrswipkxte"), "delete" ("lrxte"), or
 * "all" ("lrswipkxte"), or any combinations of the ACL codes:
 * 
 * l Lookup (mailbox is visible to LIST/LSUB, SUBSCRIBE mailbox)
 * 
 * r Read (SELECT/EXAMINE the mailbox, perform STATUS)
 * 
 * s Seen (set/clear \SEEN flag via STORE, also set \SEEN flag during
 * APPEND/COPY/FETCH BODY[...])
 * 
 * w Write flags other than \SEEN and \DELETED
 * 
 * i Insert (APPEND, COPY destination)
 * 
 * p Post (send mail to mailbox)
 * 
 * k Create mailbox (CREATE new sub-mailboxes, parent for new mailbox in RENAME)
 * 
 * x Delete mailbox (DELETE mailbox, old mailbox name in RENAME)
 * 
 * t Delete messages (set/clear \DELETED flag via STORE, also set \DELETED flag
 * during APPEND/COPY)
 * 
 * e Perform EXPUNGE and expunge as part of CLOSE
 * 
 * a Administer (SETACL/DELETEACL/GETACL/LISTRIGHTS)
 * 
 * 
 */
public class Acl implements Cloneable {

	/**
	 * 'all' in cyradm
	 */
	public static final Acl ALL = new Acl("lrswipkxtean");

	/**
	 * Needed for delivery to mailshares
	 */
	public static final Acl POST = new Acl("p");

	/**
	 * Read/Write
	 */
	public static final Acl RW = new Acl("lrswipkxten");

	/**
	 * Read only
	 */
	public static final Acl RO = new Acl("lrsp");

	/**
	 * No rights at all
	 */
	public static final Acl NOTHING = new Acl("");

	private boolean l;
	private boolean r;
	private boolean s;
	private boolean w;
	private boolean i;
	private boolean p;
	private boolean k;
	private boolean x;
	private boolean t;
	private boolean e;
	private boolean a;
	private boolean n;

	public Acl(String cyrusAclString) {
		parse(cyrusAclString);
	}

	public String toString() {
		return getCyrusString();
	}

	private String getCyrusString() {
		StringBuilder ret = new StringBuilder(12);
		ret.append(l ? "l" : "");
		ret.append(r ? "r" : "");
		ret.append(s ? "s" : "");
		ret.append(w ? "w" : "");
		ret.append(i ? "i" : "");
		ret.append(p ? "p" : "");
		ret.append(k ? "k" : "");
		ret.append(x ? "x" : "");
		ret.append(t ? "t" : "");
		ret.append(e ? "e" : "");
		ret.append(a ? "a" : "");
		ret.append(n ? "n" : "");
		return ret.toString();
	}

	private void parse(String aclString) {
		char[] chars = aclString.toCharArray();
		for (char flag : chars) {
			switch (flag) {
			case 'l':
				l = true;
				break;
			case 'r':
				r = true;
				break;
			case 's':
				s = true;
				break;
			case 'w':
				w = true;
				break;
			case 'i':
				i = true;
				break;
			case 'p':
				p = true;
				break;
			case 'k':
				k = true;
				break;
			case 'x':
				x = true;
				break;
			case 't':
				t = true;
				break;
			case 'e':
				e = true;
				break;
			case 'a':
				a = true;
				break;
			case 'n':
				n = true;
				break;
			}
		}
	}

	public boolean isL() {
		return l;
	}

	public boolean isR() {
		return r;
	}

	public boolean isS() {
		return s;
	}

	public boolean isW() {
		return w;
	}

	public boolean isI() {
		return i;
	}

	public boolean isP() {
		return p;
	}

	public boolean isK() {
		return k;
	}

	public boolean isX() {
		return x;
	}

	public boolean isT() {
		return t;
	}

	public boolean isE() {
		return e;
	}

	public boolean isA() {
		return a;
	}

	public boolean isN() {
		return n;
	}

	public void setL(boolean l) {
		this.l = l;
	}

	public void setR(boolean r) {
		this.r = r;
	}

	public void setS(boolean s) {
		this.s = s;
	}

	public void setW(boolean w) {
		this.w = w;
	}

	public void setI(boolean i) {
		this.i = i;
	}

	public void setP(boolean p) {
		this.p = p;
	}

	public void setK(boolean k) {
		this.k = k;
	}

	public void setX(boolean x) {
		this.x = x;
	}

	public void setT(boolean t) {
		this.t = t;
	}

	public void setE(boolean e) {
		this.e = e;
	}

	public void setA(boolean a) {
		this.a = a;
	}

	public void setN(boolean n) {
		this.n = n;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Acl)) {
			return false;
		}
		return getCyrusString().equals(((Acl) obj).getCyrusString());
	}

	@Override
	public int hashCode() {
		return getCyrusString().hashCode();
	}

	public void add(Acl acl) {
		parse(acl.getCyrusString());
	}

	public boolean isEmpty() {
		return "".equals(getCyrusString());
	}

	@Override
	public Object clone() {
		return new Acl(getCyrusString());
	}

}
