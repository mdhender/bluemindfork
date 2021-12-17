import { mapGetters, mapState } from "vuex";
import { MY_INBOX, MY_TRASH, FOLDERS } from "~/getters";

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
                const filtered = this.$store.getters[`mail/${FOLDERS}`].filter(folder => {
                    return (
                        folder.key !== this.$_FilterFolderMixin_activeFolder &&
                        folder.writable &&
                        (folder.path.toLowerCase().includes(this.pattern.toLowerCase()) ||
                            folder.name.toLowerCase().includes(this.pattern.toLowerCase()))
                    );
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
