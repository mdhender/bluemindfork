import { Verb } from "@bluemind/core.container.api";

const MailboxSharingOptions = {
    HAS_NO_RIGHTS: 0,
    CAN_READ_MY_MAILBOX: 1,
    CAN_EDIT_MY_MAILBOX: 2,
    CAN_MANAGE_SHARES: 3
};

const HANDLED_VERBS = [Verb.All, Verb.Manage, Verb.Write, Verb.Read];

export default {
    matchingIcon: () => "user-enveloppe",
    buildDefaultDirEntryAcl: dirEntry => [{ subject: dirEntry.uid, verb: Verb.Read }],
    getOptions: i18n => [
        {
            text: i18n.t("preferences.has_no_rights"),
            value: MailboxSharingOptions.HAS_NO_RIGHTS
        },
        {
            text: i18n.t("preferences.mail.my_mailbox.can_read_my_mailbox"),
            value: MailboxSharingOptions.CAN_READ_MY_MAILBOX
        },
        {
            text: i18n.t("preferences.mail.my_mailbox.can_edit_my_mailbox"),
            value: MailboxSharingOptions.CAN_EDIT_MY_MAILBOX
        },
        {
            text: i18n.t("preferences.mail.my_mailbox.can_edit_my_mailbox_and_manage_shares"),
            value: MailboxSharingOptions.CAN_MANAGE_SHARES
        }
    ],
    aclToOption(acl) {
        const verbs = acl.map(({ verb }) => verb);
        if (verbs.includes(Verb.All) || (verbs.includes(Verb.Write) && verbs.includes(Verb.Manage))) {
            return MailboxSharingOptions.CAN_MANAGE_SHARES;
        }
        if (verbs.includes(Verb.Write)) {
            return MailboxSharingOptions.CAN_EDIT_MY_MAILBOX;
        }
        if (verbs.includes(Verb.Read)) {
            return MailboxSharingOptions.CAN_READ_MY_MAILBOX;
        }
        return MailboxSharingOptions.HAS_NO_RIGHTS;
    },
    updateAcl(acl, subject, option) {
        if (this.aclToOption(acl) !== option) {
            const newAcl = acl.flatMap(ac => (!HANDLED_VERBS.includes(ac.verb) ? ac : []));
            switch (option) {
                case MailboxSharingOptions.CAN_READ_MY_MAILBOX:
                    newAcl.push({ verb: Verb.Read, subject });
                    break;
                case MailboxSharingOptions.CAN_EDIT_MY_MAILBOX:
                    newAcl.push({ verb: Verb.Write, subject });
                    break;
                case MailboxSharingOptions.CAN_MANAGE_SHARES:
                    newAcl.push({ verb: Verb.Write, subject });
                    newAcl.push({ verb: Verb.Manage, subject });
                    break;
                case MailboxSharingOptions.HAS_NO_RIGHTS:
                default:
                    break;
            }
            return newAcl;
        }
    }
};
