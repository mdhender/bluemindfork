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
package net.bluemind.lib.ldap;

import java.io.IOException;
import java.util.List;

import org.apache.directory.api.asn1.util.Oid;
import org.apache.directory.api.ldap.codec.api.BinaryAttributeDetector;
import org.apache.directory.api.ldap.codec.api.LdapApiService;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.AbandonRequest;
import org.apache.directory.api.ldap.model.message.AddRequest;
import org.apache.directory.api.ldap.model.message.AddResponse;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.CompareRequest;
import org.apache.directory.api.ldap.model.message.CompareResponse;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.message.DeleteRequest;
import org.apache.directory.api.ldap.model.message.DeleteResponse;
import org.apache.directory.api.ldap.model.message.ExtendedRequest;
import org.apache.directory.api.ldap.model.message.ExtendedResponse;
import org.apache.directory.api.ldap.model.message.ModifyDnRequest;
import org.apache.directory.api.ldap.model.message.ModifyDnResponse;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapConProxy implements LdapConnection, AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(LdapConProxy.class);

	private static final int MAX_RETRY = 3;

	private final LdapConnectionConfig config;
	private LdapNetworkConnection wrapped;
	private final Throwable alloc;

	private enum BindMethod {
		ANONYMOUS, LDAP_CON, BIND_REQUEST, DN, DN_CRED, NAME, NAME_CRED;
	}

	private BindMethod bindMethod = BindMethod.ANONYMOUS;
	private Dn bindDn = null;
	private String bindName = null;
	private String bindCredentials = null;
	private BindRequest bindRequest = null;

	public LdapConProxy(LdapConnectionConfig config) {
		this.config = config;
		this.wrapped = getNewWrapped();

		this.alloc = new Throwable("LDAP Connection leaked from");
		alloc.fillInStackTrace();
	}

	private LdapNetworkConnection getNewWrapped() {
		LdapNetworkConnection wrapped = new LdapNetworkConnection(config);
		return wrapped;
	}

	@Override
	protected void finalize() throws Throwable {
		if (wrapped.isConnected()) {
			logger.error("LDAP leak, alloc follows.", alloc);
			try {
				wrapped.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
		super.finalize();
	}

	public boolean isConnected() {
		return wrapped.isConnected();
	}

	public boolean isAuthenticated() {
		return wrapped.isAuthenticated();
	}

	public boolean connect() throws LdapException {
		return wrapped.connect();
	}

	@Override
	public void close() throws IOException {
		wrapped.close();
	}

	public void add(Entry entry) throws LdapException {
		getConnection().add(entry);
	}

	public AddResponse add(AddRequest addRequest) throws LdapException {
		return getConnection().add(addRequest);
	}

	public void abandon(int messageId) {
		wrapped.abandon(messageId);
	}

	public void abandon(AbandonRequest abandonRequest) {
		wrapped.abandon(abandonRequest);
	}

	public void bind() throws LdapException {
		bindMethod = BindMethod.LDAP_CON;
		wrapped.bind();
	}

	public void anonymousBind() throws LdapException {
		bindMethod = BindMethod.ANONYMOUS;

		wrapped.anonymousBind();
	}

	public void bind(String name) throws LdapException {
		bindMethod = BindMethod.NAME;
		bindName = name;

		wrapped.bind(name);
	}

	public void bind(String name, String credentials) throws LdapException {
		bindMethod = BindMethod.NAME_CRED;
		bindName = name;
		bindCredentials = credentials;

		wrapped.bind(name, credentials);
	}

	public void bind(Dn name) throws LdapException {
		bindMethod = BindMethod.DN;
		bindDn = name;

		wrapped.bind(name);
	}

	public void bind(Dn name, String credentials) throws LdapException {
		bindMethod = BindMethod.DN_CRED;
		bindDn = name;

		wrapped.bind(name, credentials);
	}

	public BindResponse bind(BindRequest bindRequest) throws LdapException {
		bindMethod = BindMethod.BIND_REQUEST;
		this.bindRequest = bindRequest;

		return wrapped.bind(bindRequest);
	}

	public EntryCursor search(Dn baseDn, String filter, SearchScope scope, String... attributes) throws LdapException {
		return getConnection().search(baseDn, filter, scope, attributes);
	}

	public EntryCursor search(String baseDn, String filter, SearchScope scope, String... attributes)
			throws LdapException {
		return getConnection().search(baseDn, filter, scope, attributes);
	}

	public SearchCursor search(SearchRequest searchRequest) throws LdapException {
		return getConnection().search(searchRequest);
	}

	public void unBind() throws LdapException {
		wrapped.unBind();
	}

	public void setTimeOut(long timeOut) {
		wrapped.setTimeOut(timeOut);
	}

	public void modify(Dn dn, Modification... modifications) throws LdapException {
		getConnection().modify(dn, modifications);
	}

	public void modify(String dn, Modification... modifications) throws LdapException {
		getConnection().modify(dn, modifications);
	}

	public void modify(Entry entry, ModificationOperation modOp) throws LdapException {
		getConnection().modify(entry, modOp);
	}

	public ModifyResponse modify(ModifyRequest modRequest) throws LdapException {
		return getConnection().modify(modRequest);
	}

	public void rename(String entryDn, String newRdn) throws LdapException {
		getConnection().rename(entryDn, newRdn);
	}

	public void rename(Dn entryDn, Rdn newRdn) throws LdapException {
		getConnection().rename(entryDn, newRdn);
	}

	public void rename(String entryDn, String newRdn, boolean deleteOldRdn) throws LdapException {
		getConnection().rename(entryDn, newRdn, deleteOldRdn);
	}

	public void rename(Dn entryDn, Rdn newRdn, boolean deleteOldRdn) throws LdapException {
		getConnection().rename(entryDn, newRdn, deleteOldRdn);
	}

	public void move(String entryDn, String newSuperiorDn) throws LdapException {
		getConnection().move(entryDn, newSuperiorDn);
	}

	public void move(Dn entryDn, Dn newSuperiorDn) throws LdapException {
		getConnection().move(entryDn, newSuperiorDn);
	}

	public void moveAndRename(Dn entryDn, Dn newDn) throws LdapException {
		getConnection().moveAndRename(entryDn, newDn);
	}

	public void moveAndRename(String entryDn, String newDn) throws LdapException {
		getConnection().moveAndRename(entryDn, newDn);
	}

	public void moveAndRename(Dn entryDn, Dn newDn, boolean deleteOldRdn) throws LdapException {
		getConnection().moveAndRename(entryDn, newDn, deleteOldRdn);
	}

	public void moveAndRename(String entryDn, String newDn, boolean deleteOldRdn) throws LdapException {
		getConnection().moveAndRename(entryDn, newDn, deleteOldRdn);
	}

	public ModifyDnResponse modifyDn(ModifyDnRequest modDnRequest) throws LdapException {
		return getConnection().modifyDn(modDnRequest);
	}

	public void delete(String dn) throws LdapException {
		getConnection().delete(dn);
	}

	public void delete(Dn dn) throws LdapException {
		getConnection().delete(dn);
	}

	public DeleteResponse delete(DeleteRequest deleteRequest) throws LdapException {
		return getConnection().delete(deleteRequest);
	}

	public boolean compare(String dn, String attributeName, String value) throws LdapException {
		return getConnection().compare(dn, attributeName, value);
	}

	public boolean compare(String dn, String attributeName, byte[] value) throws LdapException {
		return getConnection().compare(dn, attributeName, value);
	}

	public boolean compare(String dn, String attributeName, Value<?> value) throws LdapException {
		return getConnection().compare(dn, attributeName, value);
	}

	public boolean compare(Dn dn, String attributeName, String value) throws LdapException {
		return getConnection().compare(dn, attributeName, value);
	}

	public boolean compare(Dn dn, String attributeName, byte[] value) throws LdapException {
		return getConnection().compare(dn, attributeName, value);
	}

	public boolean compare(Dn dn, String attributeName, Value<?> value) throws LdapException {
		return getConnection().compare(dn, attributeName, value);
	}

	public CompareResponse compare(CompareRequest compareRequest) throws LdapException {
		return getConnection().compare(compareRequest);
	}

	public ExtendedResponse extended(String oid) throws LdapException {
		return getConnection().extended(oid);
	}

	public ExtendedResponse extended(String oid, byte[] value) throws LdapException {
		return getConnection().extended(oid, value);
	}

	public ExtendedResponse extended(Oid oid) throws LdapException {
		return getConnection().extended(oid);
	}

	public ExtendedResponse extended(Oid oid, byte[] value) throws LdapException {
		return getConnection().extended(oid, value);
	}

	public ExtendedResponse extended(ExtendedRequest extendedRequest) throws LdapException {
		return getConnection().extended(extendedRequest);
	}

	public boolean exists(String dn) throws LdapException {
		return getConnection().exists(dn);
	}

	public boolean exists(Dn dn) throws LdapException {
		return getConnection().exists(dn);
	}

	public Entry getRootDse() throws LdapException {
		return getConnection().getRootDse();
	}

	public Entry getRootDse(String... attributes) throws LdapException {
		return getConnection().getRootDse(attributes);
	}

	public Entry lookup(Dn dn) throws LdapException {
		return getConnection().lookup(dn);
	}

	public Entry lookup(String dn) throws LdapException {
		return getConnection().lookup(dn);
	}

	public Entry lookup(Dn dn, String... attributes) throws LdapException {
		return getConnection().lookup(dn, attributes);
	}

	public Entry lookup(Dn dn, Control[] controls, String... attributes) throws LdapException {
		return getConnection().lookup(dn, controls, attributes);
	}

	public Entry lookup(String dn, String... attributes) throws LdapException {
		return getConnection().lookup(dn, attributes);
	}

	public Entry lookup(String dn, Control[] controls, String... attributes) throws LdapException {
		return getConnection().lookup(dn, controls, attributes);
	}

	public boolean isControlSupported(String controlOID) throws LdapException {
		return getConnection().isControlSupported(controlOID);
	}

	public List<String> getSupportedControls() throws LdapException {
		return getConnection().getSupportedControls();
	}

	public void loadSchema() throws LdapException {
		wrapped.loadSchema();
	}

	public SchemaManager getSchemaManager() {
		return wrapped.getSchemaManager();
	}

	public LdapApiService getCodecService() {
		return wrapped.getCodecService();
	}

	public boolean doesFutureExistFor(int messageId) {
		return wrapped.doesFutureExistFor(messageId);
	}

	public BinaryAttributeDetector getBinaryAttributeDetector() {
		return wrapped.getBinaryAttributeDetector();
	}

	public void setBinaryAttributeDetector(BinaryAttributeDetector binaryAttributeDetecter) {
		wrapped.setBinaryAttributeDetector(binaryAttributeDetecter);
	}

	private LdapConnection getConnection() throws LdapException {
		int retry = 0;
		while (!wrapped.isConnected() && retry < MAX_RETRY) {
			retry++;
			logger.warn("Trying to rebind on LDAP server (" + retry + ")...");
			tryBind();
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				throw new LdapException(e);
			}
		}

		return wrapped;
	}

	private void tryBind() throws LdapException {
		try {
			wrapped.close();
		} catch (IOException e) {
		}

		wrapped = getNewWrapped();

		if (bindMethod == BindMethod.ANONYMOUS) {
			wrapped.anonymousBind();
		} else if (bindMethod == BindMethod.BIND_REQUEST) {
			wrapped.bind(bindRequest);
		} else if (bindMethod == BindMethod.DN) {
			wrapped.bind(bindDn);
		} else if (bindMethod == BindMethod.DN_CRED) {
			wrapped.bind(bindDn, bindCredentials);
		} else if (bindMethod == BindMethod.LDAP_CON) {
			wrapped.bind();
		} else if (bindMethod == BindMethod.NAME) {
			wrapped.bind(bindName);
		} else if (bindMethod == BindMethod.NAME_CRED) {
			wrapped.bind(bindName, bindCredentials);
		}
	}

	@Override
	public void setSchemaManager(SchemaManager arg0) {
		setSchemaManager(arg0);
	}

	public LdapConnectionConfig getConfig() {
		return wrapped.getConfig();
	}

	@Override
	public void loadSchemaRelaxed() throws LdapException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isRequestCompleted(int messageId) {
		return wrapped.isRequestCompleted(messageId);
	}
}
