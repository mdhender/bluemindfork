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
            $_EditRecipientsMixin_mode: recipientModes.TO | recipientModes.CC | recipientModes.BCC
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
    async mounted() {
        this._data.$_EditRecipientsMixin_mode = this.isReplyOrForward
            ? recipientModes.TO
            : recipientModes.TO | recipientModes.CC;
    }
};
