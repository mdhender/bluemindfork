import debounce from "lodash/debounce";
import { VCardQueryOrderBy } from "@bluemind/addressbook.api";
import { VCardInfoAdaptor } from "@bluemind/contact";
import { inject } from "@bluemind/inject";
import { mapMutations } from "vuex";
import { SET_MESSAGE_BCC, SET_MESSAGE_CC, SET_MESSAGE_TO } from "~/mutations";

const recipientModes = { TO: 1, CC: 2, BCC: 4 }; // flags for the display mode of MailComposer's recipients fields
export default {
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
