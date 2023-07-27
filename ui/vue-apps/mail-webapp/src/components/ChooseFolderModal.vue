<template>
    <bm-modal
        ref="choose-folder-modal"
        class="choose-folder-modal"
        body-class="choose-folder-modal-body"
        v-bind="[$attrs, $props]"
        size="sm"
        :ok-disabled="!selectedFolder || inputState === false"
        auto-focus-button="ok"
        @ok="$emit('ok', selectedFolder)"
        @cancel="doCancel"
        @hide="doCancel"
    >
        <bm-form-group :label="$t('mail.actions.choose_folder.modal.combo.label')" label-for="autocomplete-input">
            <bm-form-autocomplete-input
                id="autocomplete-input"
                ref="autocomplete-input"
                v-model.trim="pattern"
                variant="outline"
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
                    <div class="d-flex flex-fill align-items-center">
                        <mail-folder-icon no-text :mailbox="itemMailbox(item)" :folder="item" />
                        <span class="pl-2 flex-fill" :title="itemTitle(item)"> {{ itemName(item) }}</span>
                        <mail-mailbox-icon :mailbox="itemMailbox(item)" />
                    </div>
                </template>
                <template v-if="!folderNameExists && !selectedExcluded" #extra="{ close, focus, goUp, goDown }">
                    <div v-if="pattern" ref="extra" class="d-flex align-items-center" @click="close">
                        <bm-icon icon="plus" />
                        <span class="pl-2 flex-fill">
                            {{ $t("mail.folder.new.from_pattern", [pattern]) }}
                        </span>
                        <mail-mailbox-icon :mailbox="mailboxes[0]" />
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
                        <mail-mailbox-icon :mailbox="mailboxes[0]" />
                    </div>
                </template>
            </bm-form-autocomplete-input>
            <bm-notice v-if="inputState === false" class="position-absolute" :text="selectedExcluded" />
        </bm-form-group>
    </bm-modal>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { BmFormGroup, BmFormAutocompleteInput, BmModal, BmNotice } from "@bluemind/ui-components";
import { folderUtils, mailboxUtils } from "@bluemind/mail";
import { FOLDER_BY_PATH } from "~/getters";
import { FilterFolderMixin } from "~/mixins";
import MailFolderIcon from "./MailFolderIcon";
import MailFolderInput from "./MailFolderInput";
import MailMailboxIcon from "./MailMailboxIcon";

const { create: createFolder, folderExists, getFolder, translatePath } = folderUtils;
const { MailboxType } = mailboxUtils;

export default {
    name: "ChooseFolderModal",
    components: {
        BmFormGroup,
        BmFormAutocompleteInput,
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
            this.pattern = this.itemName(folder);
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
        translatePath,
        itemName(item) {
            return item.path ? translatePath(item.path) : this.itemMailbox(item).dn;
        },
        itemTitle(item) {
            return item.path || item.name;
        },
        itemMailbox(item) {
            return this.allMailboxes[item.mailboxRef.key];
        }
    }
};
</script>

<style lang="scss">
.choose-folder-modal-body {
    overflow: visible !important;
    .bm-form-autocomplete-input .suggestions {
        overflow: unset !important;
    }

    .form-group {
        margin: 0;
    }
}
</style>
