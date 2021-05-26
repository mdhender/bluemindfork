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
package net.bluemind.system.importation.commons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;

import com.google.common.base.Strings;

import net.bluemind.lib.ldap.LdapProtocol;
import net.bluemind.system.importation.commons.exceptions.InvalidDnServerFault;

public class Parameters {
	public static abstract class Server {
		public static class Host implements Comparable<Host> {
			public final String hostname;
			public final int port;
			public final int priority;
			public final int weight;

			public static Host build(String srvRecord) {
				if (srvRecord == null || (srvRecord = srvRecord.trim()).isEmpty()) {
					throw new IllegalArgumentException("srvRecord must be defined");
				}
				String[] parts = srvRecord.split(" ");

				if (parts.length != 4) {
					throw new IllegalArgumentException("Invalid srvRecord: " + srvRecord);
				}

				return build(parts[3], Integer.parseInt(parts[2]), Integer.parseInt(parts[0]),
						Integer.parseInt(parts[1]));
			}

			public static Host build(String hostname, int port, int priority, int weight) {
				if (hostname == null || hostname.trim().isEmpty()) {
					throw new IllegalArgumentException("Ldap hostname must be defined");
				}

				return new Host(hostname, port, priority, weight);
			}

			private Host(String hostname, int port, int priority, int weight) {
				this.hostname = hostname;
				this.port = port;
				this.priority = priority;
				this.weight = weight;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((hostname == null) ? 0 : hostname.hashCode());
				result = prime * result + port;
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				Host other = (Host) obj;
				if (hostname == null) {
					if (other.hostname != null)
						return false;
				} else if (!hostname.equals(other.hostname))
					return false;
				if (port != other.port)
					return false;
				return true;
			}

			@Override
			public int compareTo(Host o) {
				int ret = Integer.compare(priority, o.priority);

				if (ret == 0) {
					ret = Integer.compare(weight, o.weight) * -1;
				}

				return ret;
			}
		}

		private final Optional<Host> host;

		public final String login;
		public final String password;

		public final LdapProtocol protocol;
		public final boolean acceptAllCertificates;

		public Server(Optional<Host> host, String login, String password, LdapProtocol protocol,
				boolean acceptAllCertificates) {
			this.host = host;
			this.login = Strings.isNullOrEmpty(login) ? null : login;
			this.password = Strings.isNullOrEmpty(password) ? null : password;

			if (protocol == null) {
				this.protocol = LdapProtocol.PLAIN;
			} else {
				this.protocol = protocol;
			}

			this.acceptAllCertificates = acceptAllCertificates;
		}

		public List<Host> getLdapHost() {
			return host.map(h -> Arrays.asList(h)).orElseGet(() -> sortLdapHosts(getAlternativeHosts()));
		}

		protected abstract List<Host> getAlternativeHosts();

		private static List<Host> sortLdapHosts(List<Host> ldapHosts) {
			List<Host> orderedLdapHosts = new ArrayList<>(ldapHosts);

			// Sort hosts using RFC 2782
			// http://www.ietf.org/rfc/rfc2782.txt
			Collections.sort(orderedLdapHosts);

			return orderedLdapHosts;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (acceptAllCertificates ? 1231 : 1237);
			result = prime * result + ((host == null) ? 0 : host.hashCode());
			result = prime * result + ((login == null) ? 0 : login.hashCode());
			result = prime * result + ((password == null) ? 0 : password.hashCode());
			result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Server other = (Server) obj;
			if (acceptAllCertificates != other.acceptAllCertificates)
				return false;
			if (host == null) {
				if (other.host != null)
					return false;
			} else if (!host.equals(other.host))
				return false;
			if (login == null) {
				if (other.login != null)
					return false;
			} else if (!login.equals(other.login))
				return false;
			if (password == null) {
				if (other.password != null)
					return false;
			} else if (!password.equals(other.password))
				return false;
			if (protocol != other.protocol)
				return false;
			return true;
		}
	}

	public static class Directory {
		public final Dn baseDn;
		public final String userFilter;
		public final String groupFilter;
		public final String extIdAttribute;

		public static Directory build(String baseDn, String userFilter, String groupFilter, String extIdAttribute)
				throws InvalidDnServerFault {
			try {
				return new Directory(new Dn(baseDn), userFilter == null ? "" : userFilter,
						groupFilter == null ? "" : groupFilter, extIdAttribute);
			} catch (LdapInvalidDnException e) {
				throw new InvalidDnServerFault(e);
			}
		}

		private Directory(Dn baseDn, String userFilter, String groupFilter, String extIdAttribute) {
			this.baseDn = baseDn;
			this.userFilter = userFilter;
			this.groupFilter = groupFilter;
			this.extIdAttribute = extIdAttribute;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((baseDn == null) ? 0 : baseDn.hashCode());
			result = prime * result + ((extIdAttribute == null) ? 0 : extIdAttribute.hashCode());
			result = prime * result + ((groupFilter == null) ? 0 : groupFilter.hashCode());
			result = prime * result + ((userFilter == null) ? 0 : userFilter.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Directory other = (Directory) obj;
			if (baseDn == null) {
				if (other.baseDn != null)
					return false;
			} else if (!baseDn.equals(other.baseDn))
				return false;
			if (extIdAttribute == null) {
				if (other.extIdAttribute != null)
					return false;
			} else if (!extIdAttribute.equals(other.extIdAttribute))
				return false;
			if (groupFilter == null) {
				if (other.groupFilter != null)
					return false;
			} else if (!groupFilter.equals(other.groupFilter))
				return false;
			if (userFilter == null) {
				if (other.userFilter != null)
					return false;
			} else if (!userFilter.equals(other.userFilter))
				return false;
			return true;
		}
	}

	public static class SplitDomain {
		public final boolean splitRelayEnabled;
		public final String relayMailboxGroup;
		public final String relayMailboxGroupDn;

		public static SplitDomain addRelayMailboxGroupDn(SplitDomain splitDomain, String relayMailboxGroupDn) {
			return new SplitDomain(splitDomain.splitRelayEnabled, splitDomain.relayMailboxGroup, relayMailboxGroupDn);
		}

		public SplitDomain(boolean splitRelayEnabled, String relayMailboxGroup) {
			this.splitRelayEnabled = splitRelayEnabled;
			this.relayMailboxGroup = relayMailboxGroup;
			this.relayMailboxGroupDn = null;
		}

		private SplitDomain(boolean splitRelayEnabled, String relayMailboxGroup, String relayMailboxGroupDn) {
			this.splitRelayEnabled = splitRelayEnabled;
			this.relayMailboxGroup = relayMailboxGroup;
			this.relayMailboxGroupDn = relayMailboxGroupDn;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((relayMailboxGroup == null) ? 0 : relayMailboxGroup.hashCode());
			result = prime * result + (splitRelayEnabled ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SplitDomain other = (SplitDomain) obj;
			if (relayMailboxGroup == null) {
				if (other.relayMailboxGroup != null)
					return false;
			} else if (!relayMailboxGroup.equals(other.relayMailboxGroup))
				return false;
			if (splitRelayEnabled != other.splitRelayEnabled)
				return false;
			return true;
		}
	}

	public final boolean enabled;

	public final Server ldapServer;
	public final Directory ldapDirectory;
	public final SplitDomain splitDomain;

	public final Optional<String> lastUpdate;

	public static Parameters build(boolean enabled, Server ldapServer, Directory ldapDirectory, SplitDomain splitDomain,
			Optional<String> lastUpdate) {
		if (enabled) {
			if (ldapServer == null) {
				throw new IllegalArgumentException("ldapServer must not be null");
			}

			if (ldapDirectory == null) {
				throw new IllegalArgumentException("ldapDirectory must not be null");
			}

			if (splitDomain == null) {
				throw new IllegalArgumentException("splitDomain must not be null");
			}

			if (lastUpdate.isPresent() && lastUpdate.get().trim().isEmpty()) {
				throw new IllegalArgumentException("lastUpdate must not be empty");
			}
		}

		return new Parameters(enabled, ldapServer, ldapDirectory, splitDomain,
				lastUpdate == null ? Optional.empty() : lastUpdate);
	}

	public static Parameters disabled() {
		return new Parameters(false, null, null, null, Optional.empty());
	}

	protected Parameters(boolean enabled, Server ldapServer, Directory ldapDirectory, SplitDomain splitDomain,
			Optional<String> lastUpdate) {
		this.enabled = enabled;
		this.ldapServer = ldapServer;
		this.ldapDirectory = ldapDirectory;
		this.splitDomain = splitDomain;
		this.lastUpdate = lastUpdate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (enabled ? 1231 : 1237);
		result = prime * result + ((ldapDirectory == null) ? 0 : ldapDirectory.hashCode());
		result = prime * result + ((ldapServer == null) ? 0 : ldapServer.hashCode());
		result = prime * result + ((splitDomain == null) ? 0 : splitDomain.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Parameters other = (Parameters) obj;
		if (enabled != other.enabled)
			return false;
		if (ldapDirectory == null) {
			if (other.ldapDirectory != null)
				return false;
		} else if (!ldapDirectory.equals(other.ldapDirectory))
			return false;
		if (ldapServer == null) {
			if (other.ldapServer != null)
				return false;
		} else if (!ldapServer.equals(other.ldapServer))
			return false;
		if (splitDomain == null) {
			if (other.splitDomain != null)
				return false;
		} else if (!splitDomain.equals(other.splitDomain))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format(
				"LdapParameters [hostname=%s, protocol=%s, allCertificate=%b, loginDn=%s, lastupdate=%s, relayMailboxGroup=%s, baseDn=%s, userFilter=%s, groupFilter=%s, extId=%s, splitRelayEnabled=%s]",
				ldapServer.host.isPresent() ? ldapServer.host.get().hostname : "undef", ldapServer.protocol,
				ldapServer.acceptAllCertificates, ldapServer.login, lastUpdate, splitDomain.relayMailboxGroup,
				ldapDirectory.baseDn, ldapDirectory.userFilter, ldapDirectory.groupFilter, ldapDirectory.extIdAttribute,
				splitDomain.splitRelayEnabled);
	}
}