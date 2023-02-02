<script>
import { mapActions, mapGetters, mapState } from "vuex";
import { FOLDER_HAS_CHILDREN, MAILBOX_TRASH } from "~/getters";
import { EMPTY_FOLDER } from "~/actions";
import { MailRoutesMixin } from "~/mixins";

export default {
    name: "EmptyFolderAction",
    mixins: [MailRoutesMixin],
    props: {
        folder: {
            type: Object,
            default: null // if null, uses active folder
        }
    },
    computed: {
        ...mapGetters("mail", { FOLDER_HAS_CHILDREN, MAILBOX_TRASH }),
        ...mapState("mail", ["folders", "activeFolder", "mailboxes"]),
        targetFolder() {
            return this.folder || this.folders[this.activeFolder];
        },
        hasChildren() {
            return this.FOLDER_HAS_CHILDREN(this.targetFolder);
        },
        mailbox() {
            return this.mailboxes[this.targetFolder.mailboxRef.key];
        },
        isTrash() {
            return this.targetFolder.key === this.MAILBOX_TRASH(this.mailbox).key;
        }
    },
    methods: {
        ...mapActions("mail", { EMPTY_FOLDER }),
        async emptyFolder() {
            const confirm = await this.confirmEmptyFolder();
            if (confirm) {
                this.EMPTY_FOLDER({
                    folder: this.targetFolder,
                    mailbox: this.mailbox,
                    deep: this.isTrash
                });
                this.$router.navigate(this.folderRoute(this.targetFolder));
            }
        },
        confirmEmptyFolder() {
            const title = this.isTrash
                ? this.$t("mail.actions.empty_trash.modal.title")
                : this.$t("mail.actions.empty_folder.modal.title");
            const content = this.isTrash
                ? this.hasChildren
                    ? this.$t("mail.actions.empty_trash.modal.content.with_subfolder")
                    : this.$t("mail.actions.empty_trash.modal.content.without_subfolder")
                : this.$t("mail.actions.empty_folder.modal.content", { name: this.targetFolder.name });
            return this.confirm(title, content);
        },
        confirm(title, content) {
            // We call $bvModal on $root to prevent "MsgBox destroyed before resolve" error which typically occurs
            // when EmptyFolderAction is used around a menu entry, causing its destruction as the menu closes.
            return this.$root.$bvModal.msgBoxConfirm(content, {
                title,
                okTitle: this.$t("common.delete"),
                cancelTitle: this.$t("common.cancel"),
                okVariant: "fill-accent",
                cancelVariant: "text",
                centered: true,
                hideHeaderClose: false,
                autoFocusButton: "cancel"
            });
        }
    },
    render() {
        return this.$scopedSlots.default({
            execute: this.emptyFolder
        });
    }
};
</script>
