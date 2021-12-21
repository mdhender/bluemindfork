<template>
    <bm-dropzone
        :states="{ active: false }"
        :accept="['conversation']"
        :value="folder"
        class="filtered-item align-items-center d-inline-flex border-bottom border-ligth pl-2"
    >
        <div class="d-flex flex-column flex-fill">
            <mail-folder-icon
                :shared="shared"
                :folder="folder"
                class="pl-3"
                :class="{ 'font-weight-bold': isUnread }"
            />
            <div class="folder-path">
                <span class="d-inline-block text-truncate">{{ path.start }}</span
                ><span>{{ path.end }}</span>
            </div>
        </div>
        <div v-if="!folder.writable" :title="$t('mail.folder.access.limited')" :class="isUnread ? 'pr-1' : 'pr-2'">
            <bm-icon icon="info-circle" />
        </div>
        <mail-folder-item-menu v-if="folder.writable" :folder="folder" class="mx-1" :class="{ 'd-none': isUnread }" />
        <bm-counter-badge
            v-if="isUnread"
            :value="folder.unread"
            :variant="isActive ? 'secondary' : 'primary'"
            class="mx-1 d-block"
            :class="{ 'read-only': !folder.writable }"
            :aria-label="$t('mail.folder.unread', { count: folder.unread })"
        />
    </bm-dropzone>
</template>

<script>
import { mapActions, mapMutations, mapState } from "vuex";
import { BmCounterBadge, BmDropzone, BmIcon } from "@bluemind/styleguide";
import MailFolderIcon from "../MailFolderIcon";
import MailFolderItemMenu from "./MailFolderItemMenu";
import { REMOVE_FOLDER, TOGGLE_EDIT_FOLDER } from "~/mutations";
import { RENAME_FOLDER, CREATE_FOLDER } from "~/actions";
import { MailboxType } from "~/model/mailbox";

export default {
    name: "FilteredItem",
    components: {
        BmCounterBadge,
        BmDropzone,
        BmIcon,
        MailFolderIcon,
        MailFolderItemMenu
    },
    props: {
        folder: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("mail", ["folderList", "folders", "activeFolder", "mailboxes"]),
        isActive() {
            return this.folder.key === this.activeFolder;
        },
        isUnread() {
            return this.folder.unread > 0;
        },
        path() {
            let path = this.folder.path;
            path = path.substring(0, path.lastIndexOf("/"));
            return {
                start: path.length ? "/" + path.substring(0, path.lastIndexOf("/")) : this.$t("mail.folder.root"),
                end: path.substring(path.lastIndexOf("/"))
            };
        },
        shared() {
            return this.mailboxes[this.folder.mailboxRef.key].type === MailboxType.MAILSHARE;
        }
    },

    methods: {
        ...mapActions("mail", { RENAME_FOLDER, CREATE_FOLDER }),
        ...mapMutations("mail", { REMOVE_FOLDER, TOGGLE_EDIT_FOLDER })
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
    .mail-folder-item-menu,
    .bm-counter-badge {
        min-width: 1.4rem;
    }

    .mail-folder-item-menu {
        visibility: hidden;

        & > button {
            padding: 0;
        }
        &.d-flex + .bm-counter-badge {
            display: none !important;
        }
    }
}

@include media-breakpoint-up(lg) {
    .filtered-item:hover {
        .mail-folder-item-menu {
            display: flex !important;
            visibility: visible;
        }
        .bm-counter-badge:not(.read-only) {
            display: none !important;
        }
    }
}
</style>
