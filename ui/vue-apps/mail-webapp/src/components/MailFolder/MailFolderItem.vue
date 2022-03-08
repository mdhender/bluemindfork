<template>
    <bm-dropzone
        v-if="!editingFolder"
        :states="{ active: false }"
        :accept="['conversation']"
        :value="folder"
        class="mail-folder-item flex-fill d-flex align-items-center"
    >
        <mail-folder-icon
            :shared="shared"
            :folder="folder"
            class="flex-fill"
            :class="folder.unread > 0 ? 'font-weight-bold' : ''"
        />
        <div
            v-if="!folder.writable"
            :title="$t('mail.folder.access.limited')"
            :class="folder.unread > 0 ? 'pr-1' : 'pr-2'"
        >
            <bm-icon icon="info-circle" />
        </div>
        <mail-folder-item-menu
            v-if="folder.writable"
            :folder="folder"
            class="mx-1"
            :class="folder.unread > 0 ? 'd-none' : ''"
            @edit="toggleEditFolder(folder.key)"
            @create="createSubFolder()"
        />
        <bm-counter-badge
            v-if="folder.unread > 0"
            :value="folder.unread"
            :variant="folder.key != activeFolder ? 'secondary' : 'primary'"
            class="mx-1 d-block"
            :class="{ 'read-only': !folder.writable }"
            :aria-label="$t('mail.folder.unread', { count: folder.unread })"
        />
    </bm-dropzone>
    <mail-folder-input
        v-else
        ref="folder-input"
        :mailboxes="[mailboxes[folder.mailboxRef.key]]"
        :folder="folder"
        :shared="shared"
        @close="closeInput"
        @submit="submit"
        @keydown.left.native.stop
        @keydown.right.native.stop
        @keydown.enter.native.stop
        @mousedown.native.stop
    />
</template>

<script>
import { mapActions, mapMutations, mapState } from "vuex";
import { BmCounterBadge, BmDropzone, BmIcon } from "@bluemind/styleguide";
import UUIDGenerator from "@bluemind/uuid";
import MailFolderIcon from "../MailFolderIcon";
import MailFolderInput from "../MailFolderInput";
import MailFolderItemMenu from "./MailFolderItemMenu";
import { ADD_FOLDER, REMOVE_FOLDER, SET_FOLDER_EXPANDED, TOGGLE_EDIT_FOLDER } from "~/mutations";
import { RENAME_FOLDER, CREATE_FOLDER } from "~/actions";
import { MailboxType } from "~/model/mailbox";
import { create } from "~/model/folder";

export default {
    name: "MailFolderItem",
    components: {
        BmCounterBadge,
        BmDropzone,
        BmIcon,
        MailFolderIcon,
        MailFolderInput,
        MailFolderItemMenu
    },
    props: {
        folderKey: {
            type: String,
            required: true
        }
    },
    computed: {
        ...mapState("mail", ["folderList", "folders", "activeFolder", "mailboxes"]),
        folder() {
            return this.folders[this.folderKey];
        },
        shared() {
            return this.mailboxes[this.folder.mailboxRef.key].type === MailboxType.MAILSHARE;
        },
        editingFolder() {
            return this.folderList.editing === this.folder.key;
        }
    },
    watch: {
        editingFolder: {
            handler: function (value) {
                if (value) {
                    this.$nextTick(() => {
                        this.$refs["folder-input"].select();
                    });
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapActions("mail", { RENAME_FOLDER, CREATE_FOLDER }),
        ...mapMutations("mail", { ADD_FOLDER, REMOVE_FOLDER, SET_FOLDER_EXPANDED, TOGGLE_EDIT_FOLDER }),
        toggleEditFolder(folderUid) {
            this.TOGGLE_EDIT_FOLDER(folderUid);
        },
        submit(name) {
            const mailbox = this.mailboxes[this.folder.mailboxRef.key];
            if (this.folder && this.folder.remoteRef.uid) {
                this.RENAME_FOLDER({ folder: this.folder, name, mailbox });
                if (this.activeFolder === this.folder.key) {
                    // FIXME if (conversation.length > 1)
                    this.$router.navigate({ name: "v:mail:conversation", params: { folder: this.folder.path } });
                }
            } else if (this.folder) {
                const parent = this.folders[this.folder.parent];
                this.CREATE_FOLDER({ name, parent, mailbox });
                this.REMOVE_FOLDER(this.folder);
            }
        },
        closeInput() {
            if (this.folder && this.folder.remoteRef.uid) {
                this.toggleEditFolder(this.folder.key);
            } else if (this.folder) {
                this.REMOVE_FOLDER(this.folder);
            }
        },
        async createSubFolder() {
            const key = UUIDGenerator.generate();
            const mailbox = this.mailboxes[this.folder.mailboxRef.key];
            this.ADD_FOLDER(create(key, "", this.folder, mailbox));
            await this.$nextTick();
            // FIXME: FEATWEBML-1386
            this.TOGGLE_EDIT_FOLDER(key);
            this.SET_FOLDER_EXPANDED({ ...this.folder, expanded: true });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-folder-item {
    min-height: 26px !important;

    .mail-folder-item-menu,
    .bm-counter-badge {
        min-width: 1.4rem;
    }

    .mail-folder-icon > div {
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        text-overflow: ellipsis;
        overflow: hidden;
        white-space: break-spaces;
        line-height: 1.2;
        word-break: break-word;
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
    .mail-folder-item:hover {
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
