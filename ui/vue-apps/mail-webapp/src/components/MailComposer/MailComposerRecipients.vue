<template>
    <div class="mail-composer-recipients">
        <bm-row class="align-items-center">
            <bm-col cols="11">
                <bm-contact-input
                    ref="to"
                    :contacts="message.to"
                    :autocomplete-results="autocompleteResultsTo"
                    @search="searchedPattern => onSearch('to', searchedPattern)"
                    @update:contacts="updateTo"
                >
                    {{ $t("common.to") }}
                </bm-contact-input>
            </bm-col>
            <bm-col cols="1" class="text-center">
                <bm-button
                    v-if="displayedRecipientFields == recipientModes.TO"
                    variant="simple-dark"
                    @click="displayedRecipientFields = recipientModes.TO | recipientModes.CC | recipientModes.BCC"
                >
                    <bm-icon icon="chevron" />
                </bm-button>
            </bm-col>
        </bm-row>
        <hr class="m-0" />

        <div v-if="displayedRecipientFields > recipientModes.TO" class="d-flex">
            <div class="d-flex flex-grow-1">
                <bm-contact-input
                    :contacts="message.cc"
                    :autocomplete-results="autocompleteResultsCc"
                    class="w-100"
                    @search="searchedPattern => onSearch('cc', searchedPattern)"
                    @update:contacts="updateCc"
                >
                    {{ $t("common.cc") }}
                </bm-contact-input>
            </div>
            <bm-button
                v-if="displayedRecipientFields == (recipientModes.TO | recipientModes.CC)"
                variant="simple-dark"
                class="my-2 mr-1"
                @click="displayedRecipientFields = recipientModes.TO | recipientModes.CC | recipientModes.BCC"
            >
                {{ $t("common.bcc") }}
            </bm-button>
        </div>
        <hr v-if="displayedRecipientFields > recipientModes.TO" class="m-0" />

        <bm-contact-input
            v-if="displayedRecipientFields == (recipientModes.TO | recipientModes.CC | recipientModes.BCC)"
            :contacts="message.bcc"
            :autocomplete-results="autocompleteResultsBcc"
            @search="searchedPattern => onSearch('bcc', searchedPattern)"
            @update:contacts="updateBcc"
        >
            {{ $t("common.bcc") }}
        </bm-contact-input>
        <hr
            v-if="displayedRecipientFields == (recipientModes.TO | recipientModes.CC | recipientModes.BCC)"
            class="m-0"
        />
    </div>
</template>

<script>
import debounce from "lodash/debounce";
import { mapMutations } from "vuex";

import { VCardQueryOrderBy } from "@bluemind/addressbook.api";
import { VCardInfoAdaptor } from "@bluemind/contact";
import { inject } from "@bluemind/inject";
import { BmButton, BmCol, BmContactInput, BmIcon, BmRow } from "@bluemind/styleguide";

import { SET_MESSAGE_BCC, SET_MESSAGE_CC, SET_MESSAGE_TO } from "~mutations";
import { ComposerActionsMixin } from "~mixins";

const recipientModes = { TO: 1, CC: 2, BCC: 4 }; // flags for the display mode of MailComposer's recipients fields

export default {
    name: "MailComposerRecipients",
    components: {
        BmButton,
        BmCol,
        BmContactInput,
        BmIcon,
        BmRow
    },
    mixins: [ComposerActionsMixin],
    props: {
        message: {
            type: Object,
            required: true
        },
        isReplyOrForward: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            recipientModes,
            /**
             * @example
             * displayedRecipientFields = (TO|CC|BCC) means we want to display all 3 fields
             * displayedRecipientFields = TO means we want to display TO field only
             */
            displayedRecipientFields: recipientModes.TO | recipientModes.CC | recipientModes.BCC,
            autocompleteResults: [],
            autocompleteResultsTo: [],
            autocompleteResultsCc: [],
            autocompleteResultsBcc: []
        };
    },
    watch: {
        autocompleteResults: function () {
            this.autocompleteResultsTo = this.getAutocompleteResults("to");
            this.autocompleteResultsCc = this.getAutocompleteResults("cc");
            this.autocompleteResultsBcc = this.getAutocompleteResults("bcc");
        }
    },
    mounted() {
        this.displayedRecipientFields =
            this.isReplyOrForward && this.message.cc.length === 0
                ? recipientModes.TO
                : recipientModes.TO | recipientModes.CC;
    },
    methods: {
        ...mapMutations("mail", {
            SET_MESSAGE_TO,
            SET_MESSAGE_CC,
            SET_MESSAGE_BCC
        }),
        focus() {
            this.$refs.to.focus();
        },
        onSearch(fieldFocused, searchedPattern) {
            this.fieldFocused = fieldFocused;
            this.search(searchedPattern);
        },
        search: debounce(function (searchedRecipient) {
            if (searchedRecipient === "") {
                this.autocompleteResults = [];
            } else {
                const query =
                    "(value.identification.formatedName.value:" +
                    searchedRecipient +
                    " OR value.communications.emails.value:" +
                    searchedRecipient +
                    ") AND _exists_:value.communications.emails.value";
                return inject("AddressBooksPersistence")
                    .search({
                        from: 0,
                        size: 5,
                        query,
                        orderBy: VCardQueryOrderBy.Pertinance,
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
            }
        },
        updateTo(contacts) {
            this.SET_MESSAGE_TO({ messageKey: this.message.key, to: contacts });
            this.debouncedSave();
        },
        updateCc(contacts) {
            this.SET_MESSAGE_CC({ messageKey: this.message.key, cc: contacts });
            this.debouncedSave();
        },
        updateBcc(contacts) {
            this.SET_MESSAGE_BCC({ messageKey: this.message.key, bcc: contacts });
            this.debouncedSave();
        }
    }
};
</script>

<style lang="scss">
.mail-composer-recipients {
    .bm-contact-input .btn {
        min-width: 3rem;
        text-align: left;
    }
}
</style>
