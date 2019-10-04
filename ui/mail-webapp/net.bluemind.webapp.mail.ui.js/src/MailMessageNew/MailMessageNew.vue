<template>
    <bm-form class="mail-message-new p-3 flex-grow-1 d-flex">
        <bm-panel :title="panelTitle" :closeable="false">
            <template #body>
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
                            v-if="mode == modes.TO"
                            variant="link"
                            class="text-blue"
                            @click="mode = modes.TO | modes.CC | modes.BCC"
                        >
                            <bm-icon icon="chevron" />
                        </bm-button>
                    </bm-col>
                </bm-row>
                <hr class="m-0" />

                <bm-row v-if="mode > modes.TO">
                    <bm-col cols="11">
                        <bm-contact-input
                            ref="cc"
                            :contacts.sync="message_.cc"
                            :autocomplete-results="autocompleteResultsCc"
                            @search="searchedPattern => onSearch('cc', searchedPattern)"
                        >
                            {{ $t("common.cc") }}
                        </bm-contact-input>
                    </bm-col>
                    <bm-col cols="1" class="text-center">
                        <bm-button
                            v-if="mode == (modes.TO | modes.CC)"
                            variant="link"
                            class="text-blue"
                            @click="mode = modes.TO | modes.CC | modes.BCC"
                        >
                            {{ $t("common.bcc") }}
                        </bm-button>
                    </bm-col>
                </bm-row>
                <hr v-if="mode > modes.TO" class="m-0" />

                <bm-contact-input
                    v-if="mode == (modes.TO | modes.CC | modes.BCC)"
                    :contacts.sync="message_.bcc"
                    :autocomplete-results="autocompleteResultsBcc"
                    @search="searchedPattern => onSearch('bcc', searchedPattern)"
                >
                    {{ $t("common.bcc") }}
                </bm-contact-input>
                <hr v-if="mode == (modes.TO | modes.CC | modes.BCC)" class="mt-0" />

                <bm-form-input
                    v-model="message_.subject"
                    :placeholder="$t('mail.new.subject.placeholder')"
                    :aria-label="$t('mail.new.subject.aria')"
                    type="text"
                    @keydown.enter.native.prevent
                />
                <bm-row class="d-block">
                    <hr class="bg-dark m-0" />
                </bm-row>
                <div class="flex-grow-1">
                    <bm-form-textarea
                        v-if="userPrefTextOnly"
                        v-model="message_.content"
                        :rows="10"
                        :max-rows="10000"
                        :aria-label="$t('mail.new.content.aria')"
                        class="mail-content h-100"
                        no-resize
                    />
                    <bm-rich-editor
                        v-else
                        v-model="message_.content"
                        :is-menu-bar-opened="userPrefIsMenuBarOpened"
                        class="h-100"
                    />
                </div>
                <bm-button
                    v-if="previousMessage && previousMessage.content && !expandPreviousMessages"
                    variant="outline-dark"
                    class="align-self-start"
                    @click="displayPreviousMessages"
                >
                    <bm-icon icon="3dots" size="sm" />
                </bm-button>
            </template>
            <template #footer>
                <mail-message-new-footer @save="saveDraft" @delete="deleteDraft" @send="send" />
            </template>
        </bm-panel>
    </bm-form>
</template>

<script>
import { mapGetters, mapActions, mapMutations } from "vuex";
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
    BmRow
} from "@bluemind/styleguide";
import debounce from "lodash/debounce";
import MailMessageNewFooter from "./MailMessageNewFooter";
import MailMessageNewModes from "./MailMessageNewModes";
import ServiceLocator from "@bluemind/inject";

export default {
    name: "MailMessageNew",
    components: {
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
        MailMessageNewFooter
    },
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
        }
    },
    data() {
        return {
            expandPreviousMessages: false,
            modes: MailMessageNewModes,
            autocompleteResults: [],
            autocompleteResultsTo: [],
            autocompleteResultsCc: [],
            autocompleteResultsBcc: [],
            debouncedSave: debounce(this.saveDraft, 1000),
            userPrefTextOnly: false, // TODO: initialize this with user setting
            userPrefIsMenuBarOpened: false, // TODO: initialize this with user setting
            message_: {
                to: this.message ? this.message.to : [],
                cc: this.message ? this.message.cc : [],
                bcc: this.message ? this.message.bcc : [],
                subject: this.message ? this.message.subject : "",
                content: "",
                headers: [],
                previousMessage: this.previousMessage,
                type: undefined
            }
        };
    },
    computed: {
        //FIXME: move draft.
        ...mapGetters("mail-webapp", ["lastRecipients"]),
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
                this.setDraft({ draft: this.message_ });
                this.debouncedSave();
            },
            deep: true
        }
    },
    created: function() {
        this.setDraft({ draft: this.message_, isNew: true });
        this.message_.type = this.userPrefTextOnly ? "text" : "html";
    },
    mounted: function() {
        this.$refs.to.focus();
    },
    methods: {
        ...mapActions("mail-webapp", ["saveDraft"]),
        ...mapMutations("mail-webapp", ["setDraft"]),
        displayPreviousMessages() {
            this.message_.content += "\n\n\n" + this.previousMessage.content;
            this.expandPreviousMessages = true;
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
                return ServiceLocator.getProvider("AddressBooksPersistance")
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
        /** Navigate to the parent path: from a/b/c to a/b */
        navigateToParent() {
            const path = this.$router.history.current.path;
            const parentPath = path.substring(0, path.lastIndexOf("/") + 1);
            this.$router.push(parentPath);
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
    padding: map-get($spacers, 2) map-get($spacers, 3);
}
</style>
