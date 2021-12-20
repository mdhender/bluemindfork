import { mapGetters, mapState } from "vuex";
import { MAILBOXES, MAILBOX_FOLDERS, MY_INBOX, MY_TRASH } from "~/getters";

export default {
    data() {
        return {
            maxFolders: 10,
            pattern: ""
        };
    },
    computed: {
        ...mapState("mail", {
            $_FilterFolderMixin_activeFolder: "activeFolder"
        }),
        ...mapGetters("mail", { $_FilterFolderMixin_trash: MY_TRASH, $_FilterFolderMixin_inbox: MY_INBOX }),
        matchingFolders() {
            if (this.pattern !== "") {
                const filtered = [];
                this.$store.getters[`mail/${MAILBOXES}`].forEach(mailbox => {
                    if (mailbox.writable) {
                        this.$store.getters[`mail/${MAILBOX_FOLDERS}`](mailbox).forEach(folder => {
                            if (
                                folder.key !== this.$_FilterFolderMixin_activeFolder &&
                                (folder.path.toLowerCase().includes(this.pattern.toLowerCase()) ||
                                    folder.name.toLowerCase().includes(this.pattern.toLowerCase()))
                            ) {
                                filtered.push(folder);
                            }
                        });
                    }
                });
                if (filtered) {
                    return filtered.slice(0, this.maxFolders);
                }
            }
            return [this.$_FilterFolderMixin_inbox, this.$_FilterFolderMixin_trash].filter(
                folder => folder && folder.key !== this.$_FilterFolderMixin_activeFolder
            );
        }
    }
};
