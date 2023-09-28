import { Verb } from "@bluemind/core.container.api";
import { inject } from "@bluemind/inject";

const MailboxRight = {
    HAS_NO_RIGHTS: 1,
    CAN_READ_MY_MAILBOX: 2,
    CAN_EDIT_MY_MAILBOX: 3,
    CAN_MANAGE_SHARES: 4
};

const HANDLED_VERBS = [Verb.All, Verb.Manage, Verb.Write, Verb.Read];

// do not lose Access Controls with other verbs than HANDLED_VERBS
let otherAcl = [];

export default {
    matchingIcon: () => "user-enveloppe",
    defaultUserRight: MailboxRight.CAN_READ_MY_MAILBOX,
    defaultDomainRight: MailboxRight.HAS_NO_RIGHTS,
    maxRight: MailboxRight.CAN_MANAGE_SHARES,
    readRight: MailboxRight.CAN_READ_MY_MAILBOX,
    getOptions: i18n => [
        {
            text: i18n.t("preferences.has_no_rights"),
            value: MailboxRight.HAS_NO_RIGHTS
        },
        {
            text: i18n.t("preferences.mail.my_mailbox.can_read_my_mailbox"),
            value: MailboxRight.CAN_READ_MY_MAILBOX
        },
        {
            text: i18n.t("preferences.mail.my_mailbox.can_edit_my_mailbox"),
            value: MailboxRight.CAN_EDIT_MY_MAILBOX
        },
        {
            text: i18n.t("preferences.mail.my_mailbox.can_edit_my_mailbox_and_manage_shares"),
            value: MailboxRight.CAN_MANAGE_SHARES
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
        const userUids = new Set(
            acl.flatMap(({ subject }) =>
                subject !== domainUid && subject !== userId && subject !== container.owner ? subject : []
            )
        );
        const users = {};
        userUids.forEach(userUid => {
            users[userUid] = aclToRight(userUid, acl, this.defaultUserRight);
        });

        return { users };
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
    if (verbs.includes(Verb.All) || (verbs.includes(Verb.Write) && verbs.includes(Verb.Manage))) {
        return MailboxRight.CAN_MANAGE_SHARES;
    }
    if (verbs.includes(Verb.Write)) {
        return MailboxRight.CAN_EDIT_MY_MAILBOX;
    }
    if (verbs.includes(Verb.Read)) {
        return MailboxRight.CAN_READ_MY_MAILBOX;
    }
    return defaultRight;
}

function rightsToAcl(rightBySubject) {
    const acl = [];

    Object.entries(rightBySubject).forEach(([subject, right]) => {
        switch (right) {
            case MailboxRight.CAN_READ_MY_MAILBOX:
                acl.push({ verb: Verb.Read, subject });
                break;
            case MailboxRight.CAN_EDIT_MY_MAILBOX:
                acl.push({ verb: Verb.Write, subject });
                break;
            case MailboxRight.CAN_MANAGE_SHARES:
                acl.push({ verb: Verb.Write, subject });
                acl.push({ verb: Verb.Manage, subject });
                break;
            case MailboxRight.HAS_NO_RIGHTS:
            default:
                break;
        }
    });

    return acl.concat(otherAcl);
}
