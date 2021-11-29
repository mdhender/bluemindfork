import { Verb } from "@bluemind/core.container.api";

const MailboxAcl = {
    HAS_NO_RIGHTS: 0,
    CAN_SEND_ON_MY_BEHALF: 1,
    CAN_READ_MY_MAILBOX: 2,
    CAN_EDIT_MY_MAILBOX: 3,
    CAN_MANAGE_SHARES: 4
};

export default {
    matchingIcon: () => "user-enveloppe",
    defaultDirEntryAcl: MailboxAcl.CAN_READ_MY_MAILBOX,
    noRightAcl: MailboxAcl.HAS_NO_RIGHTS,
    getOptions: i18n => [
        {
            text: i18n.tc("preferences.manage_shares.has_no_rights", 1),
            value: MailboxAcl.HAS_NO_RIGHTS
        },
        {
            text: i18n.t("preferences.mail.my_mailbox.can_send_on_my_behalf"),
            value: MailboxAcl.CAN_SEND_ON_MY_BEHALF
        },
        {
            text: i18n.t("preferences.mail.my_mailbox.can_read_my_mailbox"),
            value: MailboxAcl.CAN_READ_MY_MAILBOX
        },
        {
            text: i18n.t("preferences.mail.my_mailbox.can_edit_my_mailbox"),
            value: MailboxAcl.CAN_EDIT_MY_MAILBOX
        },
        {
            text: i18n.t("preferences.mail.my_mailbox.can_edit_my_mailbox_and_manage_shares"),
            value: MailboxAcl.CAN_MANAGE_SHARES
        }
    ],
    aclToVerb: acl => {
        switch (acl) {
            case MailboxAcl.CAN_SEND_ON_MY_BEHALF:
                return Verb.SendOnBehalf;
            case MailboxAcl.CAN_READ_MY_MAILBOX:
                return Verb.Read;
            case MailboxAcl.CAN_EDIT_MY_MAILBOX:
                return Verb.Write;
            case MailboxAcl.CAN_MANAGE_SHARES:
                return Verb.All;
            default:
                throw "impossible case : no acl equivalent";
        }
    },
    verbToAcl: verb => {
        switch (verb) {
            case Verb.SendOnBehalf:
                return MailboxAcl.CAN_SEND_ON_MY_BEHALF;
            case Verb.Read:
                return MailboxAcl.CAN_READ_MY_MAILBOX;
            case Verb.Write:
                return MailboxAcl.CAN_EDIT_MY_MAILBOX;
            case Verb.All:
                return MailboxAcl.CAN_MANAGE_SHARES;
            default:
                throw "impossible case : no acl equivalent";
        }
    }
};
