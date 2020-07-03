import { Verb } from "@bluemind/core.container.api";

export const MailBoxBuilder = {
    build(item, getters) {
        const mailbox = {};
        switch (item.ownerDirEntryPath.split("/")[1]) {
            case "mailshares":
                mailbox.type = "mailshare";
                break;
            case "users":
                mailbox.type = "user";
                break;
        }
        mailbox.uid = item.owner;
        mailbox.name = item.ownerDisplayName;
        mailbox.writable = item.verbs.includes(Verb.Write) || item.verbs.includes(Verb.All);
        if (mailbox.type === "user") {
            mailbox.mailboxUid = "user." + mailbox.uid;
            mailbox.root = "";
        } else {
            mailbox.mailboxUid = mailbox.uid;
            mailbox.root = item.ownerDisplayName;
        }
        mailbox.folders = getters["folders/getFoldersByMailbox"](mailbox.mailboxUid);
        return mailbox;
    },

    isMailshare(mailbox) {
        return mailbox.type === "mailboxacl" && mailbox.ownerDirEntryPath.split("/")[1] === "mailshares";
    },
    isUser(mailbox) {
        return mailbox.type === "mailboxacl" && mailbox.ownerDirEntryPath.split("/")[1] === "users";
    },
    isMe(mailbox, uid) {
        return this.isUser(mailbox) && mailbox.uid === uid;
    }
};
