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
        mailbox.name = item.name;
        mailbox.writable = item.writable;
        if (mailbox.type === "user") {
            mailbox.mailboxUid = "user." + mailbox.name;
            mailbox.root = "";
        } else {
            mailbox.mailboxUid = mailbox.uid;
            mailbox.root = item.name;
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
    isMe(mailbox, name) {
        return this.isUser(mailbox) && mailbox.name === name;
    }
};
