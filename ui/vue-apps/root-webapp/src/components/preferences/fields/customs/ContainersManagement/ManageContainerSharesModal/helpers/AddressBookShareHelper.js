import { Verb } from "@bluemind/core.container.api";

export const AddressBookAcl = {
    HAS_NO_RIGHTS: 0,
    CAN_READ_MY_ADDRESSBOOK: 1,
    CAN_EDIT_MY_ADDRESSBOOK: 2,
    CAN_MANAGE_SHARES: 3
};

export function addressBookAclToVerb(acl) {
    switch (acl) {
        case AddressBookAcl.CAN_READ_MY_ADDRESSBOOK:
            return Verb.Read;
        case AddressBookAcl.CAN_EDIT_MY_ADDRESSBOOK:
            return Verb.Write;
        case AddressBookAcl.CAN_MANAGE_SHARES:
            return Verb.All;
        default:
            throw "impossible case : no acl equivalent";
    }
}

export function verbToAddressBookAcl(verb) {
    switch (verb) {
        case Verb.Read:
            return AddressBookAcl.CAN_READ_MY_ADDRESSBOOK;
        case Verb.Write:
            return AddressBookAcl.CAN_EDIT_MY_ADDRESSBOOK;
        case Verb.Manage:
        case Verb.All:
            return AddressBookAcl.CAN_MANAGE_SHARES;
        default:
            throw "impossible case : no acl equivalent";
    }
}

export function getAddressBookOptions(i18n, count) {
    return [
        {
            text: i18n.tc("preferences.manage_shares.has_no_rights", count),
            value: AddressBookAcl.HAS_NO_RIGHTS
        },
        {
            text: i18n.tc("preferences.contacts.can_read_my_addressbook", count),
            value: AddressBookAcl.CAN_READ_MY_ADDRESSBOOK
        },
        {
            text: i18n.tc("preferences.contacts.can_edit_my_addressbook", count),
            value: AddressBookAcl.CAN_EDIT_MY_ADDRESSBOOK
        },
        {
            text: i18n.tc("preferences.contacts.can_edit_my_addressbook_and_manage_shares", count),
            value: AddressBookAcl.CAN_MANAGE_SHARES
        }
    ];
}

export function defaultAddressBookDirEntryAcl() {
    return AddressBookAcl.CAN_READ_MY_ADDRESSBOOK;
}

export function defaultAddressBookDomainAcl() {
    return AddressBookAcl.HAS_NO_RIGHTS;
}
