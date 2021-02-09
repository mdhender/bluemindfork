import { Verb } from "@bluemind/core.container.api";
import { create } from "../../model/mailbox";

export const MailboxAdaptor = {
    fromMailboxContainer(item) {
        const type = item.ownerDirEntryPath.split("/")[1];
        const mailbox = create({ owner: item.owner, name: item.ownerDisplayname, type });
        if (mailbox) {
            mailbox.writable = item.verbs.includes(Verb.Write) || item.verbs.includes(Verb.All);
            mailbox.offlineSync = item.offlineSync;
        }
        return mailbox;
    },

    toMailboxContainer(mailbox) {
        return {
            ownerDirEntryPath: "/" + mailbox.type,
            ownerDisplayname: mailbox.name,
            owner: mailbox.owner,
            verbs: mailbox.writable ? [Verb.Write] : [Verb.Read],
            type: "mailboxacl"
        };
    }
};
