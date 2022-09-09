<template>
    <bm-dropzone
        v-if="!editingFolder"
        ref="dropzone"
        :states="{ active: false }"
        :accept="['conversation', 'folder']"
        :value="folder"
        class="mail-folder-item"
        :class="{ 'read-only': !folder.writable, 'show-menu-btn': showMenuBtn }"
        @holdover="onFolderHoldOver"
    >
        <mail-folder-icon
            :mailbox="mailboxes[folder.mailboxRef.key]"
            :folder="folder"
            :class="folder.unread > 0 ? 'font-weight-bold' : ''"
        />
        <div v-if="!folder.writable" :title="$t('mail.folder.access.limited')" class="instead-of-menu">
            <bm-icon icon="info-circle" />
        </div>
        <div v-if="folder.unread > 0 && !showMenuBtn" class="instead-of-menu">
            <bm-counter-badge
                :count="folder.unread"
                :active="folder.key == activeFolder"
                :aria-label="$t('mail.folder.unread', { count: folder.unread })"
            />
        </div>
        <mail-folder-item-menu
            v-if="folder.writable"
            :folder="folder"
            @edit="toggleEditFolder(folder.key)"
            @create="createSubFolder()"
            @shown="showMenuBtn = true"
            @hidden="showMenuBtn = false"
        />
    </bm-dropzone>
    <mail-folder-input
        v-else
        ref="folder-input"
        size="sm"
        :mailboxes="[mailboxes[folder.mailboxRef.key]]"
        :folder="folder"
        @close="closeInput"
        @submit="submit"
        @keydown.left.native.stop
        @keydown.right.native.stop
        @keydown.enter.native.stop
        @mousedown.native.stop
    />
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { BmCounterBadge, BmDropzone, BmIcon } from "@bluemind/styleguide";
import UUIDGenerator from "@bluemind/uuid";
import { folderUtils } from "@bluemind/mail";
import MailFolderIcon from "../MailFolderIcon";
import MailFolderInput from "../MailFolderInput";
import MailFolderItemMenu from "./MailFolderItemMenu";
import { RENAME_FOLDER, CREATE_FOLDER } from "~/actions";
import { FOLDER_HAS_CHILDREN } from "~/getters";
import { ADD_FOLDER, REMOVE_FOLDER, SET_FOLDER_EXPANDED, TOGGLE_EDIT_FOLDER } from "~/mutations";
import { FolderMixin } from "~/mixins";

const { create } = folderUtils;

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
    mixins: [FolderMixin],
    props: {
        folderKey: {
            type: String,
            required: true
        }
    },
    data() {
        return { showMenuBtn: false };
    },
    computed: {
        ...mapGetters("mail", { FOLDER_HAS_CHILDREN }),
        ...mapState("mail", ["folderList", "folders", "activeFolder", "mailboxes"]),
        folder() {
            const res = this.folders[this.folderKey];
            res.writable = this.folderKey[0] < "c"; // FIXME restore
            return res;
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
            this.SET_FOLDER_EXPANDED({ key, expanded: true });
        },
        onFolderHoldOver() {
            if (this.folder.writable && this.FOLDER_HAS_CHILDREN(this.folder)) {
                this.expand(this.folder.key);
                this.$refs.dropzone.refresh();
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/mixins/_responsiveness";
@import "~@bluemind/styleguide/css/_variables";

.mail-folder-item {
    display: flex;
    min-width: 0;
    align-items: center;
    height: 100%;

    .mail-folder-icon {
        min-width: 0;
        flex: 1;
    }

    .instead-of-menu,
    .mail-folder-item-menu {
        width: base-px-to-rem(28);
    }

    .instead-of-menu {
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .mail-folder-item-menu {
        display: none !important;

        .dropdown-toggle {
            padding: 0;
        }
    }
}

@include from-lg {
    .bm-tree-node-content:focus .mail-folder-item:not(.read-only),
    .bm-tree-node-content:focus-within .mail-folder-item:not(.read-only),
    .mail-folder-item:not(.read-only):hover,
    .mail-folder-item:not(.read-only).show-menu-btn {
        .mail-folder-item-menu {
            display: flex !important;
        }
        .instead-of-menu {
            display: none !important;
        }
    }
}
</style>
