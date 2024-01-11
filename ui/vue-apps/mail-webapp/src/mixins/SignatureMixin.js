import { mapState } from "vuex";
import { INFO, REMOVE } from "@bluemind/alert.store";
import { draftUtils, mailTipUtils, signatureUtils } from "@bluemind/mail";
import { SET_DRAFT_CONTENT, UPDATE_SIGNATURE, TOGGLE_SIGNATURE } from "~/actions";

const { isNewMessage } = draftUtils;
const {
    CORPORATE_SIGNATURE_PLACEHOLDER,
    CORPORATE_SIGNATURE_SELECTOR,
    DISCLAIMER_SELECTOR,
    PERSONAL_SIGNATURE_SELECTOR,
    wrapCorporateSignature,
    wrapDisclaimer,
    wrapPersonalSignature
} = signatureUtils;
const { getMailTipContext } = mailTipUtils;

const corporateSignatureGotInserted = {
    alert: { name: "mail.CORPORATE_SIGNATURE_INSERTED", uid: "CORPORATE_SIGNATURE" },
    options: { area: "right-panel", renderer: "CorporateSignatureAlert" }
};
const corporateSignatureGotRemoved = {
    alert: { name: "mail.CORPORATE_SIGNATURE_REMOVED", uid: "CORPORATE_SIGNATURE" },
    options: { area: "right-panel", renderer: "CorporateSignatureAlert" }
};

/**
 * This mixin can be used only for MailComposerContent component
 *
 * BmRichEditor internal methods are used to toggle personal or corporate signatures with a placeholder
 *
 */
export default {
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("mail", {
            personalSignature: state => state.messageCompose.personalSignature,
            mailTips: state => state.messageCompose.mailTips,
            $_SignatureMixin_disclaimer: state => state.messageCompose.disclaimer,
            $_SignatureMixin_editorContent: state => state.messageCompose.editorContent
        }),
        signature() {
            return this.$store.getters["mail/signature"];
        },
        signByDefault() {
            return this.$store.state.settings.insert_signature === "true";
        }
    },
    data() {
        return { $_SignatureMixin_checkCorporateSignatureDone: false };
    },
    created() {
        this.$_SignatureMixin_refreshSignature();
    },
    watch: {
        async signature(updatedSignature, previous) {
            const editorRef = await this.getEditorRef();

            this.cleanSignatureFromContent(editorRef, previous);
            this.insertSignature(editorRef, updatedSignature);
            this.insertDisclaimer(editorRef);

            this.notifySignatureChange(previous, updatedSignature);

            this.$store.dispatch(`mail/${SET_DRAFT_CONTENT}`, {
                html: editorRef.getContent(),
                draft: this.message
            });
        },
        "message.from"() {
            this.$_SignatureMixin_refreshSignature();
        },
        mailTips: {
            handler(mailTips) {
                this.$store.dispatch(`mail/${UPDATE_SIGNATURE}`, { mailTips, signByDefault: this.signByDefault });

                this.$_SignatureMixin_checkCorporateSignatureDone = true;
            },
            immediate: true
        },

        $_SignatureMixin_disclaimer: {
            async handler(disclaimer) {
                const editorRef = await this.getEditorRef();
                if (disclaimer) {
                    editorRef.insertContent(wrapDisclaimer(disclaimer.html), {
                        editable: false,
                        tooltip: this.$t("mail.compose.corporate_signature.read_only")
                    });
                } else {
                    editorRef.removeContent(DISCLAIMER_SELECTOR, { editable: false });
                }
            },
            immediate: true
        }
    },
    methods: {
        async toggleSignature() {
            this.$store.dispatch(`mail/${TOGGLE_SIGNATURE}`);
        },
        $_SignatureMixin_resetAlerts() {
            this.$store.dispatch("alert/" + REMOVE, corporateSignatureGotRemoved.alert);
            this.$store.dispatch("alert/" + REMOVE, corporateSignatureGotInserted.alert);
        },
        $_SignatureMixin_refreshSignature() {
            this.$execute("get-mail-tips", { context: getMailTipContext(this.message), message: this.message });
        },

        cleanSignatureFromContent(editorRef, signature) {
            editorRef.removeContent(PERSONAL_SIGNATURE_SELECTOR(signature?.id));
            editorRef.removeContent(DISCLAIMER_SELECTOR, { editable: false });

            const options = signature?.uid
                ? signature.usePlaceholders
                    ? { movable: CORPORATE_SIGNATURE_PLACEHOLDER }
                    : { editable: false }
                : {};
            editorRef.removeContent(CORPORATE_SIGNATURE_SELECTOR, options);

            // remove placeholder when it has not been replaced by a signature
            // case where signature has changed and doesnt match draft anymore
            this.$_SignatureMixin_checkCorporateSignatureDone && editorRef.removeText(CORPORATE_SIGNATURE_PLACEHOLDER);
        },
        insertSignature(editorRef, signature) {
            if (signature?.id) {
                editorRef.insertContent(wrapPersonalSignature({ html: signature.html, id: signature.id }), {
                    triggerOnChange: !isNewMessage(this.message)
                });
            }

            if (signature?.uid) {
                const options = {};
                if (signature.usePlaceholder) {
                    options.movable = CORPORATE_SIGNATURE_PLACEHOLDER;
                    options.tooltip = this.$t("mail.compose.corporate_signature.use_placeholder");
                } else {
                    options.editable = false;
                    options.tooltip = this.$t("mail.compose.corporate_signature.read_only");
                }
                editorRef.insertContent(wrapCorporateSignature(signature.html), options);
            }
        },
        insertDisclaimer(editorRef) {
            if (this.$_SignatureMixin_disclaimer) {
                editorRef.insertContent(wrapDisclaimer(this.$_SignatureMixin_disclaimer.html), {
                    editable: false,
                    tooltip: this.$t("mail.compose.corporate_signature.read_only")
                });
            }
        },
        notifySignatureChange(removedSignature, insertedSignature) {
            if (insertedSignature?.uid) {
                this.$store.dispatch("alert/" + INFO, corporateSignatureGotInserted);
            }

            if (removedSignature?.uid && Boolean(insertedSignature?.id)) {
                this.$store.dispatch("alert/" + INFO, corporateSignatureGotRemoved);
            }
        }
    },
    destroyed() {
        this.$_SignatureMixin_resetAlerts();
        this.$store.dispatch(`mail/${UPDATE_SIGNATURE}`, { mailTips: null });
    }
};
