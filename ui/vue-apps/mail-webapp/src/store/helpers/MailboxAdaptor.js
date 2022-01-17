import { Verb } from "@bluemind/core.container.api";
import { create } from "~/model/mailbox";

export const MailboxAdaptor = {
    fromMailboxContainer(item, dirEntry) {
        const type = item.ownerDirEntryPath.split("/")[1];
        const mailbox = create({ owner: item.owner, dn: dirEntry.displayName, address: dirEntry.email, type });
        if (mailbox) {
            mailbox.writable = item.verbs.includes(Verb.Write) || item.verbs.includes(Verb.All);
            mailbox.offlineSync = item.offlineSync;
            mailbox.remoteRef.id = item.internalId;
        }
        return mailbox;
    }
};
