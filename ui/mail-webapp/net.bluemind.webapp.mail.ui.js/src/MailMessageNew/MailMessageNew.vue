<template>
    <bm-form class="mail-message-new mt-3 px-3">
        <bm-panel
            :title="panelTitle"
            @remove="close"
        >
            <template #body>
                <bm-row class="align-items-center">
                    <bm-col cols="11">
                        <bm-contact-input
                            ref="to"
                            :contacts.sync="message_.to"
                            class="mt-2"
                            :autocomplete-results="autocompleteResultsTo"
                            @search="(searchedPattern) => onSearch('to', searchedPattern)"
                        >
                            {{ $t("common.to") }}
                        </bm-contact-input>
                    </bm-col>
                    <bm-col
                        cols="1"
                        class="text-center"
                    >
                        <bm-button
                            v-if="mode_ == modes.TO"
                            variant="link"
                            class="text-blue"
                            @click="mode_= (modes.TO|modes.CC|modes.BCC)"
                        >
                            <bm-icon icon="chevron" />
                        </bm-button>
                    </bm-col>
                </bm-row>
                <hr class="mt-0 mb-2">

                <bm-row v-if="mode_> modes.TO">
                    <bm-col cols="11">
                        <bm-contact-input
                            ref="cc"
                            :contacts.sync="message_.cc"
                            :autocomplete-results="autocompleteResultsCc"
                            @search="(searchedPattern) => onSearch('cc', searchedPattern)"
                        >
                            {{ $t("common.cc") }}
                        </bm-contact-input>
                    </bm-col>
                    <bm-col
                        cols="1"
                        class="text-center"
                    >
                        <bm-button
                            v-if="mode_== (modes.TO|modes.CC)"
                            variant="link"
                            class="text-blue"
                            @click="mode_= (modes.TO|modes.CC|modes.BCC)"
                        >
                            {{ $t("common.bcc") }}
                        </bm-button>
                    </bm-col>
                </bm-row>
                <hr
                    v-if="mode_> modes.TO"
                    class="mt-0 mb-2"
                >

                <bm-contact-input
                    v-if="mode_== (modes.TO|modes.CC|modes.BCC)"
                    :contacts.sync="message_.bcc"
                    :autocomplete-results="autocompleteResultsBcc"
                    @search="(searchedPattern) => onSearch('bcc', searchedPattern)"
                >
                    {{ $t("common.bcc") }}
                </bm-contact-input>
                <hr
                    v-if="mode_== (modes.TO|modes.CC|modes.BCC)"
                    class="mt-0"
                >

                <bm-form-input
                    v-model="message_.subject"
                    :placeholder="$t('mail.new.subject.placeholder')"
                    :aria-label="$t('mail.new.subject.aria')"
                    type="text"
                    @keydown.enter.native.prevent
                />
                <bm-row class="d-block">
                    <hr class="bg-dark mt-1 mb-1">
                </bm-row>
                <bm-form-group>
                    <bm-form-textarea
                        v-model="message_.content"
                        :rows="10"
                        :max-rows="10000"
                        :aria-label="$t('mail.new.content.aria')"
                        class="mail-content"
                        no-resize
                    />
                </bm-form-group>
                <bm-button
                    v-if="previousMessage && previousMessage.content && !expandPreviousMessages"
                    variant="outline-dark"
                    class="pb-0"
                    @click="displayPreviousMessages"
                >
                    <bm-icon
                        icon="3dots"
                        size="sm"
                    />
                </bm-button>
            </template>
            <template #footer>
                <mail-message-new-footer
                    @save="save"
                    @close="close"
                    @send="send"
                />
            </template>
        </bm-panel>
    </bm-form>
</template>

<script>
import { mapGetters } from "vuex";
import { OrderBy } from "@bluemind/addressbook.api";
import { VCardInfoAdaptor } from "@bluemind/contact";
import BmButton from "@bluemind/styleguide/components/buttons/BmButton";
import BmCol from "@bluemind/styleguide/components/layout/BmCol";
import BmContactInput from "@bluemind/styleguide/components/form/BmContactInput";
import BmFormTextarea from "@bluemind/styleguide/components/form/BmFormTextarea";
import BmFormInput from "@bluemind/styleguide/components/form/BmFormInput";
import BmForm from "@bluemind/styleguide/components/form/BmForm";
import BmFormGroup from "@bluemind/styleguide/components/form/BmFormGroup";
import BmIcon from "@bluemind/styleguide/components/BmIcon";
import BmPanel from "@bluemind/styleguide/components/BmPanel/BmPanel";
import BmRow from "@bluemind/styleguide/components/layout/BmRow";
import CommonL10N from "@bluemind/l10n";
import debounce from "lodash/debounce";
import MailMessageNewFooter from "./MailMessageNewFooter";
import ServiceLocator from "@bluemind/inject";
import uuid from "uuid/v4";

/**
 * Flags for the display mode of MailMessageNew's recipients fields.
 *
 * @example
 * MailMessageNew.mode = (TO|CC|BCC) // means we would like to display all 3 fields
 * MailMessageNew.mode = TO // means we would like the TO field only
 */
export const MailMessageNewModes = {
    NONE: 0,
    TO: 1,
    CC: 2,
    BCC: 4
};

export default {
    name: "MailMessageNew",
    components: {
        BmButton,
        BmCol,
        BmContactInput,
        BmFormTextarea,
        BmFormInput,
        BmForm,
        BmFormGroup,
        BmIcon,
        BmPanel,
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
    i18n: { messages: CommonL10N },
    data() {
        return {
            message_: {
                to: this.message ? this.message.to : [],
                cc: this.message ? this.message.cc : [],
                bcc: this.message ? this.message.bcc : [],
                subject: this.message ? this.message.subject : "",
                content: "",
                headers: []
            },
            expandPreviousMessages: false,
            modes: MailMessageNewModes,
            autocompleteResults: [],
            autocompleteResultsTo: [],
            autocompleteResultsCc: [],
            autocompleteResultsBcc: [],
            mode_: this.mode
        };
    },
    computed: {
        ...mapGetters("backend.mail/items", { lastRecipients: "getLastRecipients" }),
        panelTitle() {
            return this.message_.subject ? this.message_.subject : this.$t("mail.main.new");
        }
    },
    watch: {
        autocompleteResults: function() {
            this.autocompleteResultsTo = this.getAutocompleteResults("to");
            this.autocompleteResultsCc = this.getAutocompleteResults("cc");
            this.autocompleteResultsBcc = this.getAutocompleteResults("bcc");
        }
    },
    mounted: function() {
        this.$refs.to.focus();
    },
    methods: {
        displayPreviousMessages() {
            this.message_.content += "\n\n\n" + this.previousMessage.content;
            this.expandPreviousMessages = true;
        },
        send() {
            const messageToSend = JSON.parse(JSON.stringify(this.message_));
            if (this.previousMessage && this.previousMessage.content && !this.expandPreviousMessages) {
                messageToSend.content += "\n\n\n" + this.previousMessage.content;
            }

            let outboxUid = this.$store.state["backend.mail/folders"].folders.find(function(folder) {
                return folder.displayName === "Outbox";
            }).uid;

            this.$store
                .dispatch("backend.mail/items/send", {
                    message: messageToSend,
                    isAReply: !!this.previousMessage,
                    previousMessage: this.previousMessage,
                    outboxUid
                })
                .then(taskrefId => {
                    this.$store.commit("alert/addSuccess", {
                        uid: uuid(),
                        message: "Message successfully sent (" + (taskrefId ? taskrefId.id : "N/A") + ")"
                    });
                    this.close();
                })
                .catch(reason =>
                    this.$store.commit("alert/addError", { uid: uuid(), message: "Failed to send message " + reason })
                );
        },
        close() {
            if (this.previousMessage) {
                let indexOfLastSlash = this.$store.state.route.path.lastIndexOf("/");
                let newPath = this.$route.path.substring(0, indexOfLastSlash);
                this.$router.push({ path: newPath });
            } else {
                this.$router.push({ path: "/mail/" });
            }
        },
        save() {
            // Not implemented yet
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
        }
    }
};
</script>

<style>
.mail-message-new input,
.mail-message-new textarea {
    border: none;
}

.mail-message-new input:focus,
.mail-message-new textarea:focus {
    box-shadow: none;
}

.mail-content {
    overflow: auto !important;
}
</style>
