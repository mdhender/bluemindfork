<template>
    <div class="filtered-item" @click="select">
        <edit-folder-modal ref="edit-modal" />
        <folder-item :folder="folder" class="w-100" @edit="rename" @create="create">
            <div class="d-flex flex-column flex-fill">
                <mail-folder-icon
                    :shared="shared"
                    :folder="folder"
                    class="pl-3"
                    :class="{ 'font-weight-bold': isUnread }"
                />
                <div class="folder-path">
                    <span class="d-inline-block text-truncate">{{ path.start }}</span>
                    <span class="text-nowrap">{{ path.end }}</span>
                </div>
            </div>
        </folder-item>
    </div>
</template>

<script>
import UUIDGenerator from "@bluemind/uuid";
import { folderUtils, mailboxUtils } from "@bluemind/mail";
import MailFolderIcon from "../MailFolderIcon";
import { MailRoutesMixin } from "~/mixins";
import FolderItem from "./FolderItem";
import EditFolderModal from "./modals/EditFolderModal";

const { MailboxType } = mailboxUtils;
const { create } = folderUtils;

export default {
    name: "FilteredItem",
    components: {
        MailFolderIcon,
        FolderItem,
        EditFolderModal
    },
    mixins: [MailRoutesMixin],
    props: {
        folder: {
            type: Object,
            required: true
        }
    },
    computed: {
        isUnread() {
            return this.folder.unread > 0;
        },
        path() {
            let path = this.folder.path;
            path = path.substring(0, path.lastIndexOf("/"));
            return {
                start: path.length ? "/" + path.substring(0, path.lastIndexOf("/")) : this.$t("mail.rootFolder"),
                end: path.substring(path.lastIndexOf("/"))
            };
        },
        mailbox() {
            return this.$store.state.mail.mailboxes[this.folder.mailboxRef.key];
        },
        shared() {
            return this.mailbox.type === MailboxType.MAILSHARE;
        }
    },
    methods: {
        select() {
            this.$router.push(this.folderRoute(this.folder));
        },
        create() {
            const folder = create(UUIDGenerator.generate(), "", this.folder, this.mailbox);
            this.$refs["edit-modal"].show(folder);
        },
        rename() {
            this.$refs["edit-modal"].show(this.folder);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
@import "~@bluemind/styleguide/css/mixins";

.filtered-item {
    .folder-path {
        display: flex;
        font-size: $font-size-sm;
        font-weight: $font-weight-bold;
        color: $neutral-fg;
        *:first-child {
            @include text-overflow;
        }
    }
}
</style>
