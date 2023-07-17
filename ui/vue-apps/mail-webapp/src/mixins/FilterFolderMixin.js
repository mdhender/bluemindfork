import { mapGetters } from "vuex";
import { folderUtils } from "@bluemind/mail";
import { MAILBOXES, MAILBOX_FOLDERS, MY_INBOX, MY_TRASH } from "~/getters";
import { matchPattern } from "@bluemind/string";

const { createRoot } = folderUtils;

export default {
    data() {
        return {
            maxFolders: 10,
            pattern: ""
        };
    },
    computed: {
        ...mapGetters("mail", {
            $_FilterFolderMixin_trash: MY_TRASH,
            $_FilterFolderMixin_inbox: MY_INBOX
        })
    },
    methods: {
        matchingFolders(isExcluded, includedMailboxes = []) {
            return (includedMailboxes.length ? includedMailboxes : this.$store.getters[`mail/${MAILBOXES}`])
                .filter(mailbox => mailbox.writable)
                .flatMap(mailbox => [createRoot(mailbox), ...this.$store.getters[`mail/${MAILBOX_FOLDERS}`](mailbox)])
                .filter(folder => !isExcluded(folder) && matchPattern(this.pattern, [folder.path, folder.name]))
                .slice(0, this.maxFolders);
        }
    }
};
