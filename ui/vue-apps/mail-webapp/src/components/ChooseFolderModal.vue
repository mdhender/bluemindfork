<template>
    <bm-modal
        ref="choose-folder-modal"
        class="choose-folder-modal"
        body-class="choose-folder-modal-body"
        header-class="choose-folder-modal-header"
        v-bind="[$attrs, $props]"
        :ok-disabled="!selectedFolder || inputState === false"
        centered
        auto-focus-button="ok"
        :scrollable="false"
        @ok="$emit('ok', selectedFolder)"
        @cancel="doCancel"
        @hide="doCancel"
    >
        <template #default>
            <div class="d-flex">
                <div class="mr-4">
                    <bm-icon icon="folder" size="5x" class="mr-2 text-neutral" />
                </div>
                <div class="modal-form-autocomplete flex-fill position-relative">
                    <p>{{ $t("mail.actions.choose_folder.modal.combo.label") }}</p>
                    <bm-form-autocomplete-input
                        ref="autocomplete-input"
                        v-model.trim="pattern"
                        variant="outline-neutral"
                        :items="itemsOrDefaults()"
                        :state="inputState"
                        icon="search"
                        actionable-icon
                        :max-results="maxFolders"
                        autofocus
                        extra
                        @selected="onSelected"
                        @input="onInputUpdate"
                        @focusExtra="$refs['extra'].focus()"
                    >
                        <template #default="{ item }">
                            <div class="d-flex align-items-center">
                                <mail-folder-icon
                                    no-text
                                    :shared="allMailboxes[item.mailboxRef.key].type === MailboxType.MAILSHARE"
                                    :folder="item"
                                />
                                <span class="pl-2 flex-fill"> {{ translatePath(item) }}</span>
                                <mail-mailbox-icon no-text :mailbox="allMailboxes[item.mailboxRef.key]" />
                            </div>
                        </template>
                        <template v-if="!folderNameExists && !selectedExcluded" #extra="{close, focus, goUp, goDown}">
                            <div v-if="pattern" ref="extra" class="d-flex align-items-center" @click="close">
                                <bm-icon icon="plus" />
                                <span class="pl-2 flex-fill">
                                    {{ $t("mail.folder.new.from_pattern", [pattern]) }}
                                </span>
                                <mail-mailbox-icon no-text :mailbox="mailboxes[0]" />
                            </div>
                            <div v-else class="d-flex align-items-center">
                                <mail-folder-input
                                    ref="extra"
                                    class="flex-fill pl-0"
                                    :mailboxes="mailboxes"
                                    @submit="
                                        name => {
                                            onSelected(createFolder(null, name, null, mailboxes[0]));
                                            close();
                                        }
                                    "
                                    @keydown.left.native.stop
                                    @keydown.right.native.stop
                                    @keydown.esc.native.stop="focus"
                                    @keydown.up.native.stop="
                                        focus();
                                        goUp();
                                    "
                                    @keydown.down.native.stop="
                                        focus();
                                        goDown();
                                    "
                                />
                                <mail-mailbox-icon no-text :mailbox="mailboxes[0]" />
                            </div>
                        </template>
                    </bm-form-autocomplete-input>
                    <bm-notice v-if="inputState === false" class="position-absolute w-100" :text="selectedExcluded" />
                </div>
            </div>
        </template>
    </bm-modal>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { BmFormAutocompleteInput, BmIcon, BmModal, BmNotice } from "@bluemind/styleguide";
import { FOLDER_BY_PATH } from "~/getters";
import { FilterFolderMixin } from "~/mixins";
import { create as createFolder, folderExists, getFolder, translatePath } from "~/model/folder";
import { MailboxType } from "~/model/mailbox";
import MailFolderIcon from "./MailFolderIcon";
import MailFolderInput from "./MailFolderInput";
import MailMailboxIcon from "./MailMailboxIcon";

export default {
    name: "ChooseFolderModal",
    components: {
        BmFormAutocompleteInput,
        BmIcon,
        BmModal,
        BmNotice,
        MailFolderIcon,
        MailFolderInput,
        MailMailboxIcon
    },
    mixins: [FilterFolderMixin],
    props: {
        isExcluded: {
            type: Function,
            default: () => false
        },
        defaultFolders: {
            type: Array,
            default: () => []
        },
        mailboxes: {
            type: Array,
            required: true
        }
    },
    data() {
        return { selectedFolder: null, MailboxType };
    },
    computed: {
        ...mapGetters("mail", { FOLDER_BY_PATH }),
        ...mapState("mail", { allMailboxes: state => state.mailboxes }),
        folderNameExists() {
            return folderExists(this.pattern, this.folderByPath);
        },
        inputState() {
            return this.selectedFolder && this.selectedExcluded ? false : null;
        },
        selectedExcluded() {
            return this.isExcluded(this.selectedFolder);
        }
    },
    methods: {
        onSelected(folder) {
            this.selectedFolder = folder;
            this.pattern = this.translatePath(folder);
        },
        itemsOrDefaults() {
            return this.pattern ? this.matchingFolders(this.isExcluded, this.mailboxes) : this.defaultFolders;
        },
        doCancel() {
            this.pattern = "";
            this.selectedFolder = null;
        },
        onInputUpdate(value) {
            this.selectedFolder = value?.trim() ? this.getOrCreateFolder(value) : null;
        },
        getOrCreateFolder(pattern) {
            return getFolder(pattern, this.folderByPath) || createFolder(undefined, pattern, null, this.mailboxes[0]);
        },
        show() {
            this.$refs["choose-folder-modal"].show();
        },
        folderByPath: function (path) {
            return this.FOLDER_BY_PATH(path, this.mailboxes[0]);
        },
        createFolder,
        translatePath(folder) {
            return folder.path === "" ? folder.name : translatePath(folder.path);
        }
    }
};
</script>

<style lang="scss">
.choose-folder-modal-body {
    overflow: visible;
    .bm-form-autocomplete-input .suggestions {
        overflow: unset !important;
    }
}
.choose-folder-modal-header {
    .modal-title {
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }
}
</style>
