<template>
    <bm-dropzone
        v-if="!editingFolder"
        :states="{ active: false }"
        :accept="['message']"
        :value="folder"
        class="mail-folder-item w-100 d-flex align-items-center"
    >
        <mail-folder-icon
            :shared="shared"
            :folder="folder"
            class="flex-fill"
            :class="folder.unread > 0 ? 'font-weight-bold' : ''"
        />
        <div v-if="!folder.writable" v-bm-tooltip.top.viewport class="mr-2" :title="$t('mail.folder.access.limited')">
            <bm-icon icon="info-circle" />
        </div>
        <mail-folder-item-menu :folder="folder" @edit="toggleEditFolder(folder.uid)" />
        <bm-counter-badge
            v-if="folder.unread > 0"
            :value="folder.unread"
            :variant="folder.key != currentFolderKey ? 'secondary' : 'primary'"
            class="mr-1 d-block"
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
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { BmCounterBadge, BmDropzone, BmIcon, BmTooltip } from "@bluemind/styleguide";
import { ItemUri } from "@bluemind/item-uri";
import { REMOVE_FOLDER } from "@bluemind/webapp.mail.store";
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
        folder: {
            type: Object,
            required: true
        },
        shared: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    computed: {
        ...mapState("mail-webapp", ["currentFolderKey"]),
        ...mapGetters("mail-webapp", ["my", "mailshares"]),
        editingFolder() {
            return this.folder.editing;
        }
    },
    watch: {
        editingFolder: {
            handler: function(value) {
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
        ...mapActions("mail-webapp", ["expandFolder", "renameFolder", "createFolder"]),
        ...mapMutations([REMOVE_FOLDER]),
        ...mapMutations("mail-webapp", ["toggleEditFolder"]),
        submit(newFolderName) {
            if (this.folder.name !== "") {
                this.renameFolder({ folderKey: this.folder.key, newFolderName }).then(() => {
                    if (this.currentFolderKey === this.folder.key) {
                        this.$router.navigate({ name: "v:mail:message", params: { folder: this.folder.key } });
                    }
                });
            } else {
                const folder = {
                    value: {
                        name: newFolderName,
                        fullName: this.folder.fullName + newFolderName,
                        path: this.folder.fullName + newFolderName,
                        parentUid: this.folder.parent
                    },
                    displayName: newFolderName
                };
                let mailboxUid;
                let parentFolder = this.my.folders.find(f => f.uid === this.folder.parent);
                if (parentFolder) {
                    mailboxUid = ItemUri.container(parentFolder.key);
                } else {
                    for (let mailshare of this.mailshares) {
                        parentFolder = mailshare.folders.find(folder => folder.uid === this.folder.parent);
                        if (parentFolder) {
                            mailboxUid = ItemUri.container(parentFolder.key);
                            break;
                        }
                    }
                }
                this.createFolder({ folder, mailboxUid });
            }
        },
        closeInput() {
            if (this.folder.name !== "") {
                this.toggleEditFolder(this.folder.uid);
            } else {
                this[REMOVE_FOLDER](this.folder.uid);
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-folder-item {
    .mail-folder-item-menu,
    .bm-counter-badge {
        right: 0;
    }

    .mail-folder-icon {
        padding: {
            top: $sp-1;
            bottom: $sp-1;
        }
    }

    .mail-folder-item-menu.d-flex + .bm-counter-badge {
        display: none !important;
    }
}

@include media-breakpoint-up(lg) {
    .mail-folder-item:hover {
        .mail-folder-item-menu {
            display: flex !important;
        }
        .bm-counter-badge {
            display: none !important;
        }
    }
}
</style>
