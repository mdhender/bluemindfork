<template>
    <div class="filtered-item" @click="select">
        <folder-item :folder="folder" class="w-100">
            <div class="d-flex flex-column flex-fill">
                <mail-folder-icon
                    :shared="shared"
                    :folder="folder"
                    class="pl-3"
                    :class="{ 'font-weight-bold': isUnread }"
                />
                <div class="folder-path">
                    <span class="d-inline-block text-truncate">{{ path.start }}</span>
                    <span>{{ path.end }}</span>
                </div>
            </div>
        </folder-item>
    </div>
</template>

<script>
import { mapState } from "vuex";
import MailFolderIcon from "../MailFolderIcon";
import { MailboxType } from "~/model/mailbox";
import FolderItem from "./FolderItem";
import { MailRoutesMixin } from "~/mixins";

export default {
    name: "FilteredItem",
    components: {
        MailFolderIcon,
        FolderItem
    },
    mixins: [MailRoutesMixin],
    props: {
        folder: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("mail", ["mailboxes"]),
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
        shared() {
            return this.mailboxes[this.folder.mailboxRef.key].type === MailboxType.MAILSHARE;
        }
    },

    methods: {
        select() {
            this.$router.push(this.folderRoute(this.folder));
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
        color: $secondary;
        *:first-child {
            @include text-overflow;
        }
    }
}
</style>
