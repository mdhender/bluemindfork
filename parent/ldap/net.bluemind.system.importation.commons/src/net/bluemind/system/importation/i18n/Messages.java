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
package net.bluemind.system.importation.i18n;

import java.util.HashMap;
import java.util.Map;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.group.api.Group;
import net.bluemind.user.api.User;

public class Messages {
	public static Map<String, String> manageUserFailed(Entry entry, Exception e) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Fail to manage user DN: " + entry.getDn().getName() + ", error: " + e.getMessage());
		messages.put("fr",
				"Impossible de gérer l'utilisateur DN: " + entry.getDn().getName() + ", erreur: " + e.getMessage());
		return messages;
	}

	public static Map<String, String> suspendingBMUserFailed(String deletedUserExtId, ServerFault sf) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Fail to suspend BM user UID: " + deletedUserExtId + ", error: " + sf.getMessage() + " ("
				+ sf.getCode() + ")");
		messages.put("fr", "Impossible de suspendre l'utilisateur UID: " + deletedUserExtId + ", erreur: "
				+ sf.getMessage() + " (" + sf.getCode() + ")");
		return messages;
	}

	public static Map<String, String> failedToCheckIfDnExists(Entry entry, ServerFault sf) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Fail to check if entity DN: " + entry.getDn() + " already exist in BM database, error: "
				+ sf.getMessage() + " (" + sf.getCode() + ")");
		messages.put("fr", "Impossible de vérifier si le DN: " + entry.getDn().getName() + " existe dans BM: "
				+ sf.getMessage() + " (" + sf.getCode() + ")");
		return messages;
	}

	public static Map<String, String> failedToManageBMGroup(Entry entry, Exception e) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Fail to manage BM group DN: " + entry.getDn().getName() + ", error: " + e.getMessage());
		messages.put("fr",
				"Impossible de gérer le groupe DN: " + entry.getDn().getName() + ", erreur: " + e.getMessage());
		return messages;
	}

	public static Map<String, String> failedToAddGroup(ItemValue<Group> currentGroup, String currentGroupGroupMember) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Unable to add group: " + currentGroupGroupMember + " to Blue Mind group: "
				+ currentGroup.value.name + " - group doesn't exist in Blue Mind, check errors or LDAP filter used");
		messages.put("fr", "Impossible d'ajouter le groupe: " + currentGroupGroupMember + " au groupe: "
				+ currentGroup.value.name
				+ " - le groupe n'existe pas dans Blue Mind, vérifiez qu'il n'y ait pas d'erreur à l'import ou le filtre LDAP utilisé");
		return messages;
	}

	public static Map<String, String> failedToAddUser(ItemValue<Group> currentGroup, String currentGroupUserMember) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Unable to add user: " + currentGroupUserMember + " to Blue Mind group: "
				+ currentGroup.value.name + " - user doesn't exist in Blue Mind, check errors or LDAP filter used");
		messages.put("fr", "Impossible d'ajouter l'utilisateur: " + currentGroupUserMember + " au groupe: "
				+ currentGroup.value.name
				+ " - l'utilisateur n'existe pas dans Blue Mind, vérifiez qu'il n'y ait pas d'erreur à l'import ou le filtre LDAP utilisé");
		return messages;
	}

	public static Map<String, String> failedToDeleteGroup(String deletedGroupUID, ServerFault sf) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Fail to delete BM group UID: " + deletedGroupUID + ", error: " + sf.getMessage() + " ("
				+ sf.getCode() + ")");
		messages.put("fr", "Impossible de supprimer le group UID: " + deletedGroupUID + ", erreur: " + sf.getMessage()
				+ " (" + sf.getCode() + ")");
		return messages;
	}

	public static Map<String, String> groupMemberNotFound(String groupMember) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Group member not found: " + groupMember + " - Ignoring");
		messages.put("fr", "Membre non trouvé: " + groupMember + " - Ignoré");
		return messages;
	}

	public static Map<String, String> groupMemberCheckFail(String groupMember, LdapException le) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Unable to get group member: " + groupMember + ": " + le.getMessage());
		messages.put("fr", "Impossible de trouver le membre: " + groupMember + ": " + le.getMessage());
		return messages;
	}

	public static Map<String, String> errorManageGroupMember(String group, String member, ServerFault sf) {
		HashMap<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Error on managing group: " + group + " member: " + member + " - " + sf.getMessage());
		messages.put("fr",
				"Erreur lors de la gestion du membre: " + member + " du groupe: " + group + " - " + sf.getMessage());
		return messages;
	}

	public static Map<String, String> errorManageGroupMembers(String group, ServerFault sf) {
		HashMap<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Error on managing members of group: " + group + " - " + sf.getMessage());
		messages.put("fr", "Erreur lors de la gestion des membres du groupe: " + group + " - " + sf.getMessage());
		return messages;
	}

	public static Map<String, String> failedLookupEntryDn(Dn entryDn, LdapException le) {
		HashMap<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Error on getting entry " + entryDn.getName() + ": " + le.getMessage());
		messages.put("fr",
				"Erreur lors de la récupération de l'entrée LDAP " + entryDn.getName() + ": " + le.getMessage());
		return messages;
	}

	public static Map<String, String> failGetGroupExternalId(String groupDn) {
		HashMap<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Error while getting external attribut for group: " + groupDn);
		messages.put("fr", "Erreur lors de la récupération de l'attribut externe du groupe: " + groupDn);
		return messages;
	}

	public static Map<String, String> missingAttribute(Dn entryDn, String attributeName) {
		HashMap<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Unable to manage entry: " + entryDn.getName() + ", missing attribute: " + attributeName);
		messages.put("fr",
				"Impossible de gérer l'entrée: " + entryDn.getName() + ", attribut manquant: " + attributeName);
		return messages;
	}

	public static Map<String, String> attributeMustBeString(Dn entryDn, String attributeName) {
		HashMap<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Unable to manage entry: " + entryDn.getName() + ", attribute: " + attributeName
				+ " must be a string value");
		messages.put("fr", "Impossible de gérer l'entrée: " + entryDn.getName() + ", l'attribut: " + attributeName
				+ " doit-être une chaîne de caractères");
		return messages;
	}

	public static Map<String, String> attributeMustNotBeEmpty(Dn entryDn, String attributeName) {
		HashMap<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Unable to manage entry: " + entryDn.getName() + ", attribute: " + attributeName
				+ " must not be empty");
		messages.put("fr", "Impossible de gérer l'entrée: " + entryDn.getName() + ", l'attribut: " + attributeName
				+ " ne doit pas être vide");
		return messages;
	}

	public static Map<String, String> unableToManageEntry(String entryDn, ServerFault sf) {
		HashMap<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Unable to manage DN " + entryDn + ": " + sf.getMessage() + " (" + sf.getCode() + ")");
		messages.put("fr", "Impossible de gérer le DN " + entryDn + ": " + sf.getMessage() + " (" + sf.getCode() + ")");
		return messages;
	}

	public static Map<String, String> importWithScanner(String kind, String ldapScannerName) {
		HashMap<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", String.format("Import %s directory using scanner: %s", kind, ldapScannerName));
		messages.put("fr", String.format("Import de l'annuaire %s en utilisant le scanner: %s", kind, ldapScannerName));
		return messages;
	}

	public static Map<String, String> createUser(String login) {
		HashMap<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Creating user: " + login);
		messages.put("fr", "Création de l'utilisateur: " + login);
		return messages;
	}

	public static Map<String, String> updateUser(String login) {
		HashMap<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Updating user: " + login);
		messages.put("fr", "Mise à jour de l'utilisateur: " + login);
		return messages;
	}

	public static Map<String, String> createGroup(String name) {
		HashMap<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Creating group: " + name);
		messages.put("fr", "Création du groupe: " + name);
		return messages;
	}

	public static Map<String, String> updateGroup(String name) {
		HashMap<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Updating group: " + name);
		messages.put("fr", "Mise à jour du groupe: " + name);
		return messages;
	}

	public static Map<String, String> manageGroupFailed(Entry entry, ServerFault sf) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Fail to manage group DN: " + entry.getDn().getName() + ", error: " + sf.getMessage() + " ("
				+ sf.getCode() + ")");
		messages.put("fr", "Impossible de gérer le groupe DN: " + entry.getDn().getName() + ", erreur: "
				+ sf.getMessage() + " (" + sf.getCode() + ")");
		return messages;
	}

	public static Map<String, String> deleteGroup(ItemValue<Group> group) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", String.format("Delete group: %s, externalID: %s", group.value.name, group.externalId));
		messages.put("fr",
				String.format("Suppression du groupe: %s, ID externe: %s", group.value.name, group.externalId));
		return messages;
	}

	public static Map<String, String> deletedGroupNotFound(String extId) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", String.format("Group with externalID: %s not found", extId));
		messages.put("fr", String.format("Groupe d'ID externe: %s non trouvé", extId));
		return messages;
	}

	public static Map<String, String> suspendUser(ItemValue<User> user) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", String.format("Suspend user: %s, externalID: %s", user.value.login, user.externalId));
		messages.put("fr",
				String.format("Suspension de l'utilisateur: %s, ID externe: %s", user.value.login, user.externalId));
		return messages;
	}

	public static Map<String, String> suspendedUserNotFound(String extId) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", String.format("User with externalID: %s not found", extId));
		messages.put("fr", String.format("Utilisateur d'ID externe: %s non trouvé", extId));
		return messages;
	}

	public static Map<String, String> beforeImport(String kind) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", String.format("Executing preprocessing operations of %s import", kind));
		messages.put("fr", String.format("Exécution des opérations avant l'import %s", kind));
		return messages;
	}

	public static Map<String, String> beforeEndImport(String kind, long duration) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", String.format("Ending preprocessing operations of %s import in %dms", kind, duration));
		messages.put("fr", String.format("Opérations avant l'import %s terminées en %dms", kind, duration));
		return messages;
	}

	public static Map<String, String> afterImport(String kind) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", String.format("Executing postprocessing operations of %s import", kind));
		messages.put("fr", String.format("Exécution des opérations après l'import %s", kind));
		return messages;
	}

	public static Map<String, String> afterEndImport(String kind, long duration) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", String.format("Ending postprocessing operations of %s import in %dms", kind, duration));
		messages.put("fr", String.format("Opérations après l'import %s terminées en %dms", kind, duration));
		return messages;
	}

	public static Map<String, String> manageUserPhotoFailed(Entry entry, Exception e) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en", "Fail to manage user DN: " + entry.getDn().getName() + " photo, error: " + e.getMessage());
		messages.put("fr", "Impossible de gérer la photo de l'utilisateur DN: " + entry.getDn().getName() + ", erreur: "
				+ e.getMessage());
		return messages;
	}

	public static Map<String, String> manageUserGroupsMemberships(Entry entry, Exception e) {
		Map<String, String> messages = new HashMap<String, String>(2);
		messages.put("en",
				"Fail to manage user DN: " + entry.getDn().getName() + " groups membership, error: " + e.getMessage());
		messages.put("fr", "Impossible de gérer appartenance aux groupes de l'utilisateur DN: "
				+ entry.getDn().getName() + ", erreur: " + e.getMessage());
		return messages;
	}
}
