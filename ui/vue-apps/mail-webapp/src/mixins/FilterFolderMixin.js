import { mapGetters, mapState } from "vuex";
import { MY_INBOX, MY_TRASH } from "~/getters";

export default {
    data() {
        return {
            maxFolders: 10,
            pattern: ""
        };
    },
    computed: {
        ...mapState("mail", {
            $_FilterFolderMixin_activeFolder: "activeFolder",
            $_FilterFolderMixin_folders: "folders"
        }),
        ...mapGetters("mail", { $_FilterFolderMixin_trash: MY_TRASH, $_FilterFolderMixin_inbox: MY_INBOX }),
        matchingFolders() {
            if (this.pattern !== "") {
                const filtered = Object.values(this.$_FilterFolderMixin_folders).filter(
                    folder =>
                        folder.key !== this.$_FilterFolderMixin_activeFolder &&
                        folder.writable &&
                        (folder.path.toLowerCase().includes(this.pattern.toLowerCase()) ||
                            folder.name.toLowerCase().includes(this.pattern.toLowerCase()))
                );
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
