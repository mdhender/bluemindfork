import { Verb } from "@bluemind/core.container.api";
import { MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";

const AddressBookAcl = {
    HAS_NO_RIGHTS: 0,
    CAN_READ_MY_ADDRESSBOOK: 1,
    CAN_EDIT_MY_ADDRESSBOOK: 2,
    CAN_MANAGE_SHARES: 3
};

export default {
    matchingIcon: () => "addressbook",
    matchingFileTypeIcon: () => "file-type-vcard",
    allowedFileTypes: () => MimeType.VCARD,
    importFileRequest: async (containerUid, file, uploadCanceller) => {
        const encoded = await file.text().then(res => JSON.stringify(res));
        return inject("VCardServicePersistence", containerUid).importCards(encoded, uploadCanceller);
    },
    defaultDirEntryAcl: AddressBookAcl.CAN_READ_MY_ADDRESSBOOK,
    defaultDomainAcl: AddressBookAcl.HAS_NO_RIGHTS,
    noRightAcl: AddressBookAcl.HAS_NO_RIGHTS,
    getOptions: (i18n, count) => [
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
    ],
    aclToVerb: acl => {
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
    },
    verbToAcl: verb => {
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
};
