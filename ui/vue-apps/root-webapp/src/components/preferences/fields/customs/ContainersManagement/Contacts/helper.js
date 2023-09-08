import { Verb } from "@bluemind/core.container.api";
import { MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";

const AddressBookAcl = {
    HAS_NO_RIGHTS: 0,
    CAN_READ_MY_ADDRESSBOOK: 1,
    CAN_EDIT_MY_ADDRESSBOOK: 2,
    CAN_MANAGE_SHARES: 3
};

const HANDLED_VERBS = [Verb.All, Verb.Manage, Verb.Write, Verb.Read];

export default {
    matchingIcon: () => "addressbook",
    matchingFileTypeIcon: () => "file-type-vcard",
    allowedFileTypes: () => MimeType.VCARD,
    importFileRequest: async (containerUid, file, uploadCanceller) => {
        const encoded = await file.text().then(res => JSON.stringify(res));
        return inject("VCardServicePersistence", containerUid).importCards(encoded, uploadCanceller);
    },
    buildDefaultDirEntryAcl: dirEntry => [{ subject: dirEntry.uid, verb: Verb.Read }],
    defaultDomainAcl: [],
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
    aclToOption: acl => {
        const verbs = acl.map(({ verb }) => verb);

        if (verbs.includes(Verb.All) || verbs.includes(Verb.Manage)) {
            return AddressBookAcl.CAN_MANAGE_SHARES;
        }
        if (verbs.includes(Verb.Write)) {
            return AddressBookAcl.CAN_EDIT_MY_ADDRESSBOOK;
        }
        if (verbs.includes(Verb.Read)) {
            return AddressBookAcl.CAN_READ_MY_ADDRESSBOOK;
        }
        return AddressBookAcl.HAS_NO_RIGHTS;
    },
    updateAcl(acl, subject, option) {
        if (this.aclToOption(acl) !== option) {
            const newAcl = acl.flatMap(ac => (!HANDLED_VERBS.includes(ac.verb) ? ac : []));
            switch (option) {
                case AddressBookAcl.CAN_READ_MY_ADDRESSBOOK:
                    newAcl.push({ verb: Verb.Read, subject });
                    break;
                case AddressBookAcl.CAN_EDIT_MY_ADDRESSBOOK:
                    newAcl.push({ verb: Verb.Write, subject });
                    break;
                case AddressBookAcl.CAN_MANAGE_SHARES:
                    newAcl.push({ verb: Verb.Write, subject });
                    newAcl.push({ verb: Verb.Manage, subject });
                    break;
                default:
                    break;
            }
            return newAcl;
        }
    }
};
