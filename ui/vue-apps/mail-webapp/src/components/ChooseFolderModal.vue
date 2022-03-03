<template>
    <bm-modal
        ref="choose-folder-modal"
        class="choose-folder-modal"
        v-bind="[$attrs, $props]"
        centered
        auto-focus-button="ok"
        :scrollable="false"
        @ok="$emit('ok', folderSelected)"
        @cancel="doCancel"
        @hide="doCancel"
    >
        <template #default>
            <div class="d-flex">
                <div class="flex-columns mr-4">
                    <bm-icon icon="folder" size="5x" class="mr-2 text-secondary" />
                </div>
                <div class="flex-columns modal-form-autocomplete">
                    <p>{{ $t("mail.actions.choose_folder.modal.combo.label") }}</p>
                    <bm-form-autocomplete-input
                        v-slot="{ item }"
                        v-model.trim="pattern"
                        variant="outline-secondary"
                        :items="itemsOrDefaults()"
                        icon="search"
                        actionable-icon
                        :max-results="maxFolders"
                        @selected="folderSelection"
                        @input="onInputUpdate"
                    >
                        <div class="d-flex align-items-center">
                            <span class="flex-fill"> {{ translatePath(item.path) }}</span>
                            <mail-mailbox-icon no-text :mailbox="mailboxes[item.mailboxRef.key]" />
                        </div>
                    </bm-form-autocomplete-input>
                </div>
            </div>
        </template>

        <template #modal-footer="{ ok, cancel }">
            <bm-button type="submit" variant="primary" :disabled="!folderSelected" @click.prevent="ok()">
                {{ okTitle }}
            </bm-button>
            <bm-button variant="outline-secondary" class="ml-2" @click.prevent="cancel()">
                {{ cancelTitle }}
            </bm-button>
        </template>
    </bm-modal>
</template>

<script>
import { mapState } from "vuex";
import { BmButton, BmFormAutocompleteInput, BmIcon, BmModal } from "@bluemind/styleguide";
import { FilterFolderMixin } from "~/mixins";
import { translatePath } from "~/model/folder";
import MailMailboxIcon from "./MailMailboxIcon";

export default {
    name: "ChooseFolderModal",
    components: { BmButton, BmFormAutocompleteInput, BmIcon, BmModal, MailMailboxIcon },
    mixins: [FilterFolderMixin],
    props: {
        excludedFolders: {
            type: Array,
            default: () => []
        },
        includedMailboxes: {
            type: Array,
            default: () => []
        }
    },
    data() {
        return { folderSelected: null };
    },
    computed: {
        ...mapState("mail", ["mailboxes"]),
        okTitle() {
            return this.$attrs["ok-title"] || this.$t("mail.actions.move");
        },
        cancelTitle() {
            return this.$attrs["cancel-title"] || this.$t("common.cancel");
        }
    },
    methods: {
        folderSelection(folder) {
            this.folderSelected = folder;
            this.pattern = translatePath(folder.path);
        },
        itemsOrDefaults() {
            return this.folderSelected ? [] : this.matchingFolders(this.excludedFolders, this.includedMailboxes);
        },
        translatePath(path) {
            return translatePath(path);
        },
        doCancel() {
            this.pattern = "";
            this.folderSelected = null;
        },
        onInputUpdate() {
            this.folderSelected = null;
        },
        show() {
            this.$refs["choose-folder-modal"].show();
        }
    }
};
</script>

<style lang="scss">
.choose-folder-modal {
    .modal-body {
        overflow: visible;
        padding-bottom: 1rem;
        .flex-columns.modal-form-autocomplete {
            width: 100%;
        }
    }
}
</style>
