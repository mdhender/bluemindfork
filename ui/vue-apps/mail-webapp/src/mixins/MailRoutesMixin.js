import { mapGetters, mapState } from "vuex";

import { MY_MAILBOX } from "~getters";

export default {
    computed: {
        ...mapState("mail", { $_MailRoutesMixin_folders: "folders", $_MailRoutesMixin_mailboxes: "mailboxes" }),
        ...mapGetters("mail", { $_MailRoutesMixin_MY_MAILBOX: MY_MAILBOX })
    },
    methods: {
        folderRoute({ key }) {
            const folder = this.$_MailRoutesMixin_folders[key];
            if (folder && this.$_MailRoutesMixin_mailboxes[folder.mailboxRef.key]) {
                const mailbox = this.$_MailRoutesMixin_mailboxes[folder.mailboxRef.key];
                if (mailbox.key === this.$_MailRoutesMixin_MY_MAILBOX.key) {
                    return { name: "v:mail:home", params: { folder: folder.path, mailbox: null } };
                } else {
                    return { name: "v:mail:home", params: { folder: folder.path, mailbox: mailbox.name } };
                }
            }
            return { name: "v:mail:home", params: { folder: null, mailbox: null } };
        }
    }
};
