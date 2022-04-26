import debounce from "lodash/debounce";
import { searchVCardsHelper, VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { mapActions, mapMutations } from "vuex";
import { CHECK_CORPORATE_SIGNATURE } from "~/actions";
import { SET_MESSAGE_BCC, SET_MESSAGE_CC, SET_MESSAGE_TO } from "~/mutations";
import ComposerActionsMixin from "./ComposerActionsMixin";

const recipientModes = { TO: 1, CC: 2, BCC: 4 }; // flags for the display mode of MailComposer's recipients fields
export default {
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
             * $_EditRecipientsMixin_mode = (TO|CC|BCC) means we want to display all 3 fields
             * $_EditRecipientsMixin_mode = TO means we want to display TO field only
             */
            $_EditRecipientsMixin_mode: recipientModes.TO | recipientModes.CC | recipientModes.BCC,
            autocompleteResults: [],
            autocompleteResultsTo: [],
            autocompleteResultsCc: [],
            autocompleteResultsBcc: []
        };
    },
    computed: {
        displayedRecipientFields: {
            get() {
                return (
                    this._data.$_EditRecipientsMixin_mode |
                    (this.message.to.length > 0 && recipientModes.TO) |
                    (this.message.cc.length > 0 && recipientModes.CC) |
                    (this.message.bcc.length > 0 && recipientModes.BCC)
                );
            },
            set(mode) {
                this._data.$_EditRecipientsMixin_mode = mode;
            }
        }
    },
    watch: {
        autocompleteResults: function () {
            this.autocompleteResultsTo = this.getAutocompleteResults("to");
            this.autocompleteResultsCc = this.getAutocompleteResults("cc");
            this.autocompleteResultsBcc = this.getAutocompleteResults("bcc");
        }
    },
    async mounted() {
        this._data.$_EditRecipientsMixin_mode = this.isReplyOrForward
            ? recipientModes.TO
            : recipientModes.TO | recipientModes.CC;
        if (this.message.to.length === 0) {
            await this.$nextTick();
            this.$refs.to.focus();
        }
    },
    methods: {
        ...mapActions("mail", { CHECK_CORPORATE_SIGNATURE }),
        ...mapMutations("mail", { SET_MESSAGE_TO, SET_MESSAGE_CC, SET_MESSAGE_BCC }),
        onSearch(fieldFocused, searchedPattern) {
            this.fieldFocused = fieldFocused;
            this.search(searchedPattern);
        },
        search: debounce(function (searchedRecipient) {
            if (searchedRecipient === "") {
                this.autocompleteResults = [];
            } else {
                return inject("AddressBooksPersistence")
                    .search(searchVCardsHelper(searchedRecipient))
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
            this.CHECK_CORPORATE_SIGNATURE({ message: this.message });
            this.debouncedSave();
        },
        updateCc(contacts) {
            this.SET_MESSAGE_CC({ messageKey: this.message.key, cc: contacts });
            this.CHECK_CORPORATE_SIGNATURE({ message: this.message });
            this.debouncedSave();
        },
        updateBcc(contacts) {
            this.SET_MESSAGE_BCC({ messageKey: this.message.key, bcc: contacts });
            this.CHECK_CORPORATE_SIGNATURE({ message: this.message });
            this.debouncedSave();
        },
        validateAddress: EmailValidator.validateAddress,
        validateDnAndAddress: EmailValidator.validateDnAndAddress
    }
};
