import { mapGetters, mapState } from "vuex";

import { MY_MAILBOX } from "~/getters";
import MessagePathParam from "~/router/MessagePathParam";
import { draft } from "@bluemind/mail";

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
        },
        draftPath(myDrafts) {
            return MessagePathParam.build(undefined, {
                remoteRef: { internalId: draft.TEMPORARY_MESSAGE_ID },
                folderRef: { key: myDrafts.key }
            });
        },

        /**
         * Navigate to the URL of the given conversation. Fall back to message-like URL if the conversation is a single
         * message. Fallback to the URL of the given folder if conversaiton is false.
         */
        navigateTo(conversation, folder) {
            if (!conversation) {
                this.$router.push(this.folderRoute(folder));
            } else {
                this.$router.navigate({ name: "v:mail:conversation", params: { conversation } });
            }
        }
    }
};
