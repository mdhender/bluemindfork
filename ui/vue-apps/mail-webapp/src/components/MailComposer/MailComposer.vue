<template>
    <bm-form class="mail-composer p-lg-3 flex-grow-1 d-flex h-100">
        <bm-panel>
            <template #header>
                <span
                    v-bm-tooltip.hover
                    class="d-none d-lg-flex text-nowrap text-truncate card-header px-2 py-1"
                    :title="panelTitle"
                    >{{ panelTitle }}</span
                >
            </template>
            <template #body>
                <div class="pl-3">
                    <bm-row class="align-items-center">
                        <bm-col cols="11">
                            <bm-contact-input
                                ref="to"
                                :contacts.sync="message_.to"
                                :autocomplete-results="autocompleteResultsTo"
                                @search="searchedPattern => onSearch('to', searchedPattern)"
                                @update:contacts="saveDraft"
                            >
                                {{ $t("common.to") }}
                            </bm-contact-input>
                        </bm-col>
                        <bm-col cols="1" class="text-center">
                            <bm-button
                                v-if="mode_ == modes.TO"
                                variant="simple-dark"
                                class="text-blue"
                                @click="mode_ = modes.TO | modes.CC | modes.BCC"
                            >
                                <bm-icon icon="chevron" />
                            </bm-button>
                        </bm-col>
                    </bm-row>
                    <hr class="m-0" />

                    <div v-if="mode_ > modes.TO" class="d-flex">
                        <div class="d-flex flex-grow-1">
                            <bm-contact-input
                                :contacts.sync="message_.cc"
                                :autocomplete-results="autocompleteResultsCc"
                                class="w-100"
                                @search="searchedPattern => onSearch('cc', searchedPattern)"
                                @update:contacts="saveDraft"
                            >
                                {{ $t("common.cc") }}
                            </bm-contact-input>
                        </div>
                        <bm-button
                            v-if="mode_ == (modes.TO | modes.CC)"
                            variant="simple-dark"
                            class="text-blue my-2 mr-1"
                            @click="mode_ = modes.TO | modes.CC | modes.BCC"
                        >
                            {{ $t("common.bcc") }}
                        </bm-button>
                    </div>
                    <hr v-if="mode_ > modes.TO" class="m-0" />

                    <bm-contact-input
                        v-if="mode_ == (modes.TO | modes.CC | modes.BCC)"
                        :contacts.sync="message_.bcc"
                        :autocomplete-results="autocompleteResultsBcc"
                        @search="searchedPattern => onSearch('bcc', searchedPattern)"
                        @update:contacts="saveDraft"
                    >
                        {{ $t("common.bcc") }}
                    </bm-contact-input>
                    <hr v-if="mode_ == (modes.TO | modes.CC | modes.BCC)" class="m-0" />

                    <bm-form-input
                        v-model="message_.subject"
                        class="mail-composer-subject d-flex align-items-center"
                        :placeholder="$t('mail.new.subject.placeholder')"
                        :aria-label="$t('mail.new.subject.aria')"
                        type="text"
                        @keydown.enter.native.prevent
                        @input="saveDraft"
                    />
                </div>
                <bm-row class="d-block m-0"><hr class="bg-dark m-0" /></bm-row>
                <bm-row class="mt-1 mb-2">
                    <bm-col cols="12">
                        <bm-file-drop-zone
                            class="z-index-110 attachments"
                            file-type-regex="image/(jpeg|jpg|png|gif)"
                            @dropFiles="addAttachments($event)"
                        >
                            <template #dropZone>
                                <bm-icon icon="paper-clip" size="2x" />
                                <h2 class="text-center p-2">
                                    {{ $tc("mail.new.attachments.images.drop.zone", draggedFilesCount) }}
                                </h2>
                            </template>
                            <mail-attachments-block :attachments="parts.attachments" editable expanded />
                        </bm-file-drop-zone>
                    </bm-col>
                </bm-row>
                <bm-file-drop-zone
                    class="z-index-110 as-attachments"
                    file-type-regex="^(?!.*image/(jpeg|jpg|png|gif)).*$"
                    at-least-one-match
                    @dropFiles="addAttachments($event)"
                    @filesCount="draggedFilesCount = $event"
                >
                    <template #dropZone>
                        <h2 class="text-center p-2">{{ $tc("mail.new.attachments.drop.zone", draggedFilesCount) }}</h2>
                        <bm-icon icon="arrow-up" size="2x" />
                    </template>
                    <bm-file-drop-zone class="z-index-110" inline file-type-regex="image/(jpeg|jpg|png|gif)">
                        <template #dropZone>
                            <bm-icon class="text-dark" icon="file-type-image" size="2x" />
                            <h2 class="text-center p-2">{{ $tc("mail.new.images.drop.zone", draggedFilesCount) }}</h2>
                        </template>
                        <bm-form-textarea
                            v-if="userPrefTextOnly"
                            ref="message-content"
                            v-model="message_.content"
                            :rows="10"
                            :max-rows="10000"
                            :aria-label="$t('mail.new.content.aria')"
                            class="mail-content"
                            no-resize
                            @input="saveDraft"
                        />
                        <bm-rich-editor
                            v-else
                            ref="message-content"
                            v-model="message_.content"
                            :is-menu-bar-opened="userPrefIsMenuBarOpened"
                            class="h-100"
                            @input="saveDraft"
                        >
                            <bm-button
                                v-if="previousMessage && !message_.isReplyExpanded"
                                variant="outline-dark"
                                class="align-self-start ml-3 mb-2"
                                @click="displayPreviousMessages"
                            >
                                <bm-icon icon="3dots" size="sm" />
                            </bm-button>
                        </bm-rich-editor>
                    </bm-file-drop-zone>
                </bm-file-drop-zone>
                <bm-button
                    v-if="userPrefTextOnly && previousMessage && !message_.isReplyExpanded"
                    variant="outline-dark"
                    class="align-self-start"
                    @click="displayPreviousMessages"
                >
                    <bm-icon icon="3dots" size="sm" />
                </bm-button>
            </template>
            <template #footer>
                <mail-composer-footer
                    :user-pref-text-only="userPrefTextOnly"
                    :user-pref-is-menu-bar-opened="userPrefIsMenuBarOpened"
                    @toggleTextFormat="userPrefIsMenuBarOpened = !userPrefIsMenuBarOpened"
                    @delete="deleteDraft"
                    @send="send"
                />
            </template>
        </bm-panel>
    </bm-form>
</template>

<script>
import { mapGetters, mapActions, mapMutations, mapState } from "vuex";
import { OrderBy } from "@bluemind/addressbook.api";
import { VCardInfoAdaptor } from "@bluemind/contact";
import {
    BmButton,
    BmCol,
    BmContactInput,
    BmFormInput,
    BmForm,
    BmFormTextarea,
    BmIcon,
    BmPanel,
    BmRichEditor,
    BmRow,
    BmTooltip,
    BmFileDropZone
} from "@bluemind/styleguide";
import debounce from "lodash/debounce";
import MailAttachmentsBlock from "../MailAttachment/MailAttachmentsBlock";
import MailComposerFooter from "./MailComposerFooter";
import MailComposerModes from "./MailComposerModes";
import ServiceLocator from "@bluemind/inject";

export default {
    name: "MailComposer",
    components: {
        BmButton,
        BmCol,
        BmContactInput,
        BmFileDropZone,
        BmFormInput,
        BmForm,
        BmFormTextarea,
        BmIcon,
        BmPanel,
        BmRichEditor,
        BmRow,
        MailComposerFooter,
        MailAttachmentsBlock
    },
    directives: { BmTooltip },
    props: {
        message: {
            type: Object,
            default: () => null
        },
        mode: {
            type: Number,
            default: MailComposerModes.TO | MailComposerModes.CC
        },
        previousMessage: {
            type: Object,
            default: null
        },
        userPrefTextOnly: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            modes: MailComposerModes,
            autocompleteResults: [],
            autocompleteResultsTo: [],
            autocompleteResultsCc: [],
            autocompleteResultsBcc: [],
            debouncedSave: debounce(this.save, 1000),
            userPrefIsMenuBarOpened: false, // TODO: initialize this with user setting
            mode_: this.mode,
            message_: {
                to: this.message ? this.message.to : [],
                cc: this.message ? this.message.cc : [],
                bcc: this.message ? this.message.bcc : [],
                subject: this.message ? this.message.subject : "",
                content: "",
                headers: [],
                previousMessage: this.previousMessage,
                type: undefined,
                isReplyExpanded: false
            },
            draggedFilesCount: -1
        };
    },
    computed: {
        ...mapGetters("mail-webapp", ["lastRecipients"]),
        ...mapState("mail-webapp/draft", ["parts"]),
        panelTitle() {
            return this.message_.subject ? this.message_.subject : this.$t("mail.main.new");
        }
    },
    watch: {
        autocompleteResults: function () {
            this.autocompleteResultsTo = this.getAutocompleteResults("to");
            this.autocompleteResultsCc = this.getAutocompleteResults("cc");
            this.autocompleteResultsBcc = this.getAutocompleteResults("bcc");
        }
    },
    created: function () {
        this.deleteAllSelectedMessages();
        this.clearDraft();
        this.message_.type = this.userPrefTextOnly ? "text" : "html";
        this.message_.previousMessage = this.previousMessage;
        this.updateDraft(this.message_);
    },
    mounted: function () {
        if (this.message && (this.message.to.length > 0 || this.message.cc.length > 0)) {
            this.$refs["message-content"].focus();
        } else {
            this.$refs.to.focus();
        }
    },
    destroyed: function () {
        this.clearDraft();
    },
    methods: {
        ...mapActions("mail-webapp", { save: "saveDraft", addAttachments: "addAttachments" }),
        ...mapMutations("mail-webapp", ["deleteAllSelectedMessages"]),
        ...mapMutations("mail-webapp/draft", { clearDraft: "clear", updateDraft: "update" }),
        ...mapGetters("mail-webapp/draft", { isMessageEmpty: "isEmpty" }),
        displayPreviousMessages() {
            this.message_.content += this.previousMessage.content;
            this.$nextTick(() => {
                if (!this.userPrefTextOnly) {
                    this.$refs["message-content"].updateContent();
                    this.$refs["message-content"].focus("start");
                } else {
                    this.$refs["message-content"].focus();
                    this.$refs["message-content"].setSelectionRange(0, 0);
                }
            });
            this.message_.isReplyExpanded = true;
        },
        send() {
            this.debouncedSave.cancel();
            // send then close the composer
            this.$store.dispatch("mail-webapp/send").then(() => this.$router.navigate("v:mail:message"));
        },
        saveDraft() {
            this.updateDraft(this.message_);
            this.debouncedSave.cancel();
            if (!this.isMessageEmpty()) {
                this.debouncedSave();
            }
        },
        deleteDraft() {
            this.debouncedSave.cancel();
            // delete the draft then close the composer
            this.$store.dispatch("mail-webapp/deleteDraft").then(() => this.$router.navigate("v:mail:message"));
        },
        onSearch(fieldFocused, searchedPattern) {
            this.fieldFocused = fieldFocused;
            this.search(searchedPattern);
        },
        search: debounce(function (searchedRecipient) {
            if (searchedRecipient === "") {
                this.autocompleteResults = [];
            } else {
                return ServiceLocator.getProvider("AddressBooksPersistence")
                    .get()
                    .search({
                        from: 0,
                        size: 5,
                        query: searchedRecipient,
                        orderBy: OrderBy.Pertinance,
                        escapeQuery: false
                    })
                    .then(results => {
                        if (results.values.length === 0) {
                            this.autocompleteResults = undefined;
                        } else {
                            this.autocompleteResults = results.values.map(vcardInfo =>
                                VCardInfoAdaptor.toContact(vcardInfo)
                            );
                        }
                    });
            }
        }, 200),
        getAutocompleteResults(fromField) {
            if (fromField !== this.fieldFocused || this.autocompleteResults === undefined) {
                return [];
            }
            if (this.autocompleteResults.length > 0) {
                return this.autocompleteResults;
            } else {
                return this.lastRecipients;
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-composer {
    .mail-composer-subject input,
    .bm-contact-input input,
    textarea {
        border: none;
    }

    input:focus,
    textarea:focus {
        box-shadow: none;
    }

    .bm-rich-editor-content .ProseMirror {
        min-height: 12rem;
    }

    .mail-content {
        overflow: auto !important;
    }

    .ProseMirror,
    .mail-content {
        padding: $sp-2 $sp-3;
    }

    .mail-composer-subject {
        min-height: 2.5rem;
    }

    .bm-contact-input .btn {
        min-width: 3rem;
        text-align: left;
    }

    .bm-contact-input .bm-form-autocomplete-input .suggestions {
        z-index: 200;
    }

    .bm-file-drop-zone.attachments .bm-dropzone-active-content {
        min-height: 7em;
    }

    .bm-file-drop-zone.as-attachments.bm-dropzone-active,
    .bm-file-drop-zone.as-attachments.bm-dropzone-hover {
        background: url("~@bluemind/styleguide/assets/attachment.png") no-repeat center center;
    }
}
</style>
