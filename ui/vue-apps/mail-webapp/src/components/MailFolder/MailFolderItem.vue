<template>
    <bm-dropzone
        v-if="!editingFolder"
        :states="{ active: false }"
        :accept="['message']"
        :value="folder"
        class="mail-folder-item flex-fill d-inline-flex align-items-center"
    >
        <mail-folder-icon
            :shared="shared"
            :folder="folder"
            class="flex-fill"
            :class="folder.unread > 0 ? 'font-weight-bold' : ''"
        />
        <div v-if="!folder.writable" v-bm-tooltip.top.viewport :title="$t('mail.folder.access.limited')" class="pr-1">
            <bm-icon icon="info-circle" />
        </div>
        <mail-folder-item-menu
            :folder="folder"
            class="mx-1"
            :class="folder.unread > 0 ? 'd-none' : ''"
            @edit="toggleEditFolder(folder.key)"
        />
        <bm-counter-badge
            v-if="folder.unread > 0"
            :value="folder.unread"
            :variant="folder.key != activeFolder ? 'secondary' : 'primary'"
            class="mx-1 d-block"
        />
    </bm-dropzone>
    <mail-folder-input
        v-else
        ref="folder-input"
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
import { BmCounterBadge, BmDropzone, BmIcon, BmTooltip } from "@bluemind/styleguide";
import MailFolderIcon from "../MailFolderIcon";
import MailFolderInput from "../MailFolderInput";
import MailFolderItemMenu from "./MailFolderItemMenu";

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
    directives: { BmTooltip },
    props: {
        folderKey: {
            type: String,
            required: true
        }
    },
    computed: {
        ...mapState("mail", ["folderList", "folders", "activeFolder"]),
        folder() {
            return this.folders[this.folderKey];
        },
        shared() {
            return !this.folder.mailbox.startsWith("user.");
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
        ...mapActions("mail-webapp", ["renameFolder", "createFolder"]),
        ...mapMutations("mail", ["REMOVE_FOLDER", "TOGGLE_EDIT_FOLDER"]),
        toggleEditFolder(folderUid) {
            this.TOGGLE_EDIT_FOLDER(folderUid);
        },
        submit(newFolderName) {
            if (this.folder && this.folder.name !== "") {
                this.renameFolder({ folderKey: this.folder.key, newFolderName }).then(() => {
                    if (this.activeFolder === this.folder.key) {
                        this.$router.navigate({ name: "v:mail:message", params: { folder: this.folder.key } });
                    }
                });
            } else {
                const folder = {
                    name: newFolderName,
                    path: this.folder.path + "/" + newFolderName,
                    parent: this.folder.parent
                };
                this.createFolder({ folder, mailboxUid: this.folder.mailbox });
            }
        },
        closeInput() {
            if (this.folder && this.folder.name !== "") {
                this.toggleEditFolder(this.folder.key);
            } else {
                this.REMOVE_FOLDER(this.folder.key);
            }
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
        .bm-counter-badge {
            display: none !important;
        }
    }
}
</style>
