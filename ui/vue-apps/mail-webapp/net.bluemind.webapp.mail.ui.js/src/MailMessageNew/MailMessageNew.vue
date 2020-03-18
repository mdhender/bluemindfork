<template>
    <bm-form class="mail-message-new p-lg-3 flex-grow-1 d-flex">
        <bm-panel>
            <template #header>
                <span
                    v-bm-tooltip.hover.ds500
                    class="d-none d-lg-flex text-nowrap text-truncate card-header"
                    :title="panelTitle"
                    >{{ panelTitle }}</span
                >
            </template>
            <template #body>
                <div class="px-3">
                    <bm-row class="align-items-center">
                        <bm-col cols="11">
                            <bm-contact-input
                                ref="to"
                                :contacts.sync="message_.to"
                                :autocomplete-results="autocompleteResultsTo"
                                @search="searchedPattern => onSearch('to', searchedPattern)"
                            >
                                {{ $t("common.to") }}
                            </bm-contact-input>
                        </bm-col>
                        <bm-col cols="1" class="text-center">
                            <bm-button
                                v-if="mode_ == modes.TO"
                                variant="link"
                                class="text-blue"
                                @click="mode_ = modes.TO | modes.CC | modes.BCC"
                            >
                                <bm-icon icon="chevron" />
                            </bm-button>
                        </bm-col>
                    </bm-row>
                    <hr class="m-0" />

                    <bm-row v-if="mode_ > modes.TO">
                        <bm-col cols="11">
                            <bm-contact-input
                                :contacts.sync="message_.cc"
                                :autocomplete-results="autocompleteResultsCc"
                                @search="searchedPattern => onSearch('cc', searchedPattern)"
                            >
                                {{ $t("common.cc") }}
                            </bm-contact-input>
                        </bm-col>
                        <bm-col cols="1" class="text-center">
                            <bm-button
                                v-if="mode_ == (modes.TO | modes.CC)"
                                variant="link"
                                class="text-blue"
                                @click="mode_ = modes.TO | modes.CC | modes.BCC"
                            >
                                {{ $t("common.bcc") }}
                            </bm-button>
                        </bm-col>
                    </bm-row>
                    <hr v-if="mode_ > modes.TO" class="m-0" />

                    <bm-contact-input
                        v-if="mode_ == (modes.TO | modes.CC | modes.BCC)"
                        :contacts.sync="message_.bcc"
                        :autocomplete-results="autocompleteResultsBcc"
                        @search="searchedPattern => onSearch('bcc', searchedPattern)"
                    >
                        {{ $t("common.bcc") }}
                    </bm-contact-input>
                    <hr v-if="mode_ == (modes.TO | modes.CC | modes.BCC)" class="m-0" />

                    <bm-form-input
                        v-model="message_.subject"
                        :placeholder="$t('mail.new.subject.placeholder')"
                        :aria-label="$t('mail.new.subject.aria')"
                        type="text"
                        @keydown.enter.native.prevent
                    />
                </div>
                <bm-row class="d-block m-0"><hr class="bg-dark m-0"/></bm-row>
                <bm-row>
                    <bm-col cols="12">
                        <mail-message-content-attachments-block :attachments="parts.attachments" editable expanded />
                    </bm-col>
                </bm-row>
                <bm-file-drop-zone
                    class="flex-grow-1 z-index-110"
                    :text="$t('mail.new.attachments.drop.zone')"
                    @dropFiles="addAttachments($event)"
                >
                    <bm-form-textarea
                        v-if="userPrefTextOnly"
                        ref="message-content"
                        v-model="message_.content"
                        :rows="10"
                        :max-rows="10000"
                        :aria-label="$t('mail.new.content.aria')"
                        class="mail-content"
                        no-resize
                    />
                    <bm-rich-editor
                        v-else
                        ref="message-content"
                        v-model="message_.content"
                        :is-menu-bar-opened="userPrefIsMenuBarOpened"
                        class="h-100"
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
                <mail-message-new-footer
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
import { RouterMixin } from "@bluemind/router";
import debounce from "lodash/debounce";
import MailMessageNewFooter from "./MailMessageNewFooter";
import MailMessageNewModes from "./MailMessageNewModes";
import ServiceLocator from "@bluemind/inject";
import MailMessageContentAttachmentsBlock from "../MailMessageContent/MailMessageContentAttachmentsBlock";
import { Message } from "@bluemind/backend.mail.store";

export default {
    name: "MailMessageNew",
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
        MailMessageNewFooter,
        MailMessageContentAttachmentsBlock
    },
    directives: { BmTooltip },
    mixins: [RouterMixin],
    props: {
        message: {
            type: Object,
            default: () => null
        },
        mode: {
            type: Number,
            default: MailMessageNewModes.TO | MailMessageNewModes.CC
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
            modes: MailMessageNewModes,
            autocompleteResults: [],
            autocompleteResultsTo: [],
            autocompleteResultsCc: [],
            autocompleteResultsBcc: [],
            debouncedSave: debounce(this.saveDraft, 1000),
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
            }
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
        autocompleteResults: function() {
            this.autocompleteResultsTo = this.getAutocompleteResults("to");
            this.autocompleteResultsCc = this.getAutocompleteResults("cc");
            this.autocompleteResultsBcc = this.getAutocompleteResults("bcc");
        },
        message_: {
            handler: function() {
                this.debouncedSave.cancel();
                if (!this.isMessageEmpty()) {
                    this.updateDraft(this.message_);
                    this.debouncedSave();
                }
            },
            deep: true
        }
    },
    created: function() {
        this.message_.type = this.userPrefTextOnly ? "text" : "html";
        this.clearDraft();
        this.updateDraft(this.message_);
    },
    mounted: function() {
        if (this.message && (this.message.to.length > 0 || this.message.cc.length > 0)) {
            this.$refs["message-content"].focus();
        } else {
            this.$refs.to.focus();
        }
    },
    destroyed: function() {
        this.clearDraft();
    },
    methods: {
        ...mapActions("mail-webapp", ["saveDraft", "addAttachments"]),
        ...mapMutations("mail-webapp/draft", { clearDraft: "clear", updateDraft: "update" }),
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
            this.$store.dispatch("mail-webapp/send").then(() => this.navigateToParent());
        },
        deleteDraft() {
            this.debouncedSave.cancel();
            // delete the draft then close the composer
            this.$store.dispatch("mail-webapp/deleteDraft").then(() => this.navigateToParent());
        },
        onSearch(fieldFocused, searchedPattern) {
            this.fieldFocused = fieldFocused;
            this.search(searchedPattern);
        },
        search: debounce(function(searchedRecipient) {
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
        },
        isMessageEmpty() {
            return new Message(null, this.message_).isEmpty();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-message-new input,
.mail-message-new textarea {
    border: none;
}

.mail-message-new input:focus,
.mail-message-new textarea:focus {
    box-shadow: none;
}

.mail-message-new .bm-rich-editor-content .ProseMirror {
    min-height: 12rem;
}

.mail-message-new .mail-content {
    overflow: auto !important;
}

.mail-message-new .ProseMirror,
.mail-message-new .mail-content {
    padding: $sp-2 $sp-3;
}
</style>
