import { Verb } from "@bluemind/core.container.api";
import { MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";

const AddressBookRight = {
    HAS_NO_RIGHTS: 1,
    CAN_READ_MY_ADDRESSBOOK: 2,
    CAN_EDIT_MY_ADDRESSBOOK: 3,
    CAN_MANAGE_SHARES: 4
};

const HANDLED_VERBS = [Verb.All, Verb.Manage, Verb.Write, Verb.Read];

// do not lose Access Controls with other verbs than HANDLED_VERBS
let otherAcl = [];

export default {
    matchingIcon: () => "addressbook",
    matchingFileTypeIcon: () => "file-type-vcard",
    allowedFileTypes: () => MimeType.VCARD,
    importFileRequest: async (containerUid, file, uploadCanceller) => {
        const vcard = await file.text();
        return inject("VCardServicePersistence", containerUid).importCards(vcard, uploadCanceller);
    },
    defaultUserRight: AddressBookRight.CAN_READ_MY_ADDRESSBOOK,
    maxRight: AddressBookRight.CAN_MANAGE_SHARES,
    readRight: AddressBookRight.CAN_READ_MY_ADDRESSBOOK,
    getOptions: i18n => [
        {
            text: i18n.t("preferences.has_no_rights"),
            value: AddressBookRight.HAS_NO_RIGHTS
        },
        {
            text: i18n.t("preferences.contacts.can_read_my_addressbook"),
            value: AddressBookRight.CAN_READ_MY_ADDRESSBOOK
        },
        {
            text: i18n.t("preferences.contacts.can_edit_my_addressbook"),
            value: AddressBookRight.CAN_EDIT_MY_ADDRESSBOOK
        },
        {
            text: i18n.t("preferences.contacts.can_edit_my_addressbook_and_manage_shares"),
            value: AddressBookRight.CAN_MANAGE_SHARES
        }
    ],
    async loadRights(container) {
        const allAcl = await inject("ContainerManagementPersistence", container.uid).getAccessControlList();
        const aclReducer = (res, ac) => {
            HANDLED_VERBS.includes(ac.verb) ? res[0].push(ac) : res[1].push(ac);
            return res;
        };
        const [acl, other] = allAcl.reduce(aclReducer, [[], []]);
        otherAcl = other;

        const { domain: domainUid, userId } = inject("UserSession");
        const domain = aclToRight(domainUid, acl, AddressBookRight.HAS_NO_RIGHTS);

        const userUids = new Set(
            acl.flatMap(({ subject }) =>
                subject !== domainUid && subject !== userId && subject !== container.owner ? subject : []
            )
        );
        const users = {};
        userUids.forEach(userUid => {
            users[userUid] = aclToRight(userUid, acl, this.defaultUserRight);
        });

        return { users, domain };
    },
    saveRights(rightBySubject, container) {
        return inject("ContainerManagementPersistence", container.uid).setAccessControlList(
            rightsToAcl(rightBySubject)
        );
    }
};

function aclToRight(subjectUid, acl, defaultRight) {
    const extractVerbs = acl => acl.flatMap(({ subject, verb }) => (subject === subjectUid ? verb : []));
    const verbs = extractVerbs(acl);
    return verbsToRight(verbs, defaultRight);
}

function verbsToRight(verbs, defaultRight) {
    if (verbs.includes(Verb.All) || verbs.includes(Verb.Manage)) {
        return AddressBookRight.CAN_MANAGE_SHARES;
    }
    if (verbs.includes(Verb.Write)) {
        return AddressBookRight.CAN_EDIT_MY_ADDRESSBOOK;
    }
    if (verbs.includes(Verb.Read)) {
        return AddressBookRight.CAN_READ_MY_ADDRESSBOOK;
    }
    return defaultRight;
}

function rightsToAcl(rightBySubject) {
    const acl = [];

    Object.entries(rightBySubject).forEach(([subject, right]) => {
        switch (right) {
            case AddressBookRight.CAN_READ_MY_ADDRESSBOOK:
                acl.push({ verb: Verb.Read, subject });
                break;
            case AddressBookRight.CAN_EDIT_MY_ADDRESSBOOK:
                acl.push({ verb: Verb.Write, subject });
                break;
            case AddressBookRight.CAN_MANAGE_SHARES:
                acl.push({ verb: Verb.Write, subject });
                acl.push({ verb: Verb.Manage, subject });
                break;
            default:
                break;
        }
    });

    return acl.concat(otherAcl);
}
