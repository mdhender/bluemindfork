import { mapGetters } from "vuex";
import { INFO, REMOVE } from "@bluemind/alert.store";
import { draftUtils, mailTipUtils, signatureUtils } from "@bluemind/mail";
import { SET_DRAFT_CONTENT, TOGGLE_SIGNATURE } from "~/actions";
import { SET_MAIL_TIPS, SIGNATURE_TOGGLED } from "~/mutations";
import { SIGNATURE, DISCLAIMER } from "~/getters";

const { isNewMessage } = draftUtils;
const {
    CORPORATE_SIGNATURE_PLACEHOLDER,
    CORPORATE_SIGNATURE_SELECTOR,
    DISCLAIMER_SELECTOR,
    PERSONAL_SIGNATURE_SELECTOR
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
 */
export default {
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", {
            $_SignatureMixin_disclaimer: DISCLAIMER,
            signature: SIGNATURE
        }),
        signByDefault() {
            return this.$store.state.settings.insert_signature === "true";
        },
        triggerOnChange() {
            return !isNewMessage(this.message);
        }
    },
    data() {
        return { $_SignatureMixin_checkCorporateSignatureDone: false };
    },
    created() {
        this.$_SignatureMixin_refreshSignature();
        this.$store.commit(`mail/${SIGNATURE_TOGGLED}`, this.signByDefault);

        const unwatch = this.$watch("$_SignatureMixin_disclaimer", async () => {
            // Handle disclaimer insertion on new message (case where no auto insert signature) at composer open
            const editorRef = await this.getEditorRef();
            await editorRef.removeContent(DISCLAIMER_SELECTOR, {
                editable: false,
                triggerOnChange: this.triggerOnChange
            });
            this.insertDisclaimer(editorRef);
            unwatch();
        });
    },

    watch: {
        signature: {
            async handler(updatedSignature, previous) {
                const editorRef = await this.getEditorRef();

                this.cleanSignatureFromContent(editorRef, previous, updatedSignature);
                this.insertSignature(editorRef, updatedSignature);
                this.insertDisclaimer(editorRef);

                this.notifySignatureChange(previous, updatedSignature);

                this.$store.dispatch(`mail/${SET_DRAFT_CONTENT}`, {
                    html: editorRef.getContent(),
                    draft: this.message
                });
            },
            immediate: true
        },

        "message.from"() {
            this.$_SignatureMixin_refreshSignature();
        }
    },
    methods: {
        $_SignatureMixin_refreshSignature() {
            this.$execute("get-mail-tips", { context: getMailTipContext(this.message), message: this.message });
        },

        cleanSignatureFromContent(editorRef, signature, newSignature) {
            /**
             * newSignature is required in case of
             * another identity signature on reopening a draft message
             */
            editorRef.removeContent(PERSONAL_SIGNATURE_SELECTOR(signature?.id || newSignature?.id), {
                triggerOnChange: this.triggerOnChange
            });
            editorRef.removeContent(
                DISCLAIMER_SELECTOR,
                { editable: false },
                { triggerOnChange: this.triggerOnChange }
            );
            editorRef.removeContent(CORPORATE_SIGNATURE_SELECTOR, this.editorInsertionOptions(signature), {
                triggerOnChange: this.triggerOnChange
            });

            // remove placeholder when it has not been replaced by a signature
            // case where signature has changed and doesnt match draft anymore
            /* this.$_SignatureMixin_checkCorporateSignatureDone && */
            editorRef.removeText(CORPORATE_SIGNATURE_PLACEHOLDER);
        },

        editorInsertionOptions(signature) {
            const options = { triggerOnChange: this.triggerOnChange };
            if (!signature?.uid) {
                return options;
            }
            if (signature.usePlaceholders) {
                options.movable = CORPORATE_SIGNATURE_PLACEHOLDER;
            } else {
                options.movable = false;
            }

            return options;
        },

        insertSignature(editorRef, signature) {
            if (signature?.id) {
                editorRef.insertContent(signature.html, {
                    triggerOnChange: this.triggerOnChange
                });
            }

            if (signature?.uid) {
                const tooltip = signature.usePlaceholder ? "use_placeholder" : "read_only";
                editorRef.insertContent(signature.html, {
                    ...this.editorInsertionOptions(signature),
                    tooltip: this.$t(`mail.compose.corporate_signature.${tooltip}`)
                });
            }
        },
        insertDisclaimer(editorRef) {
            if (this.$_SignatureMixin_disclaimer) {
                editorRef.insertContent(this.$_SignatureMixin_disclaimer, {
                    editable: false,
                    triggerOnChange: this.triggerOnChange,
                    tooltip: this.$t("mail.compose.corporate_signature.read_only")
                });
            }
        },
        notifySignatureChange(removedSignature, insertedSignature) {
            if (insertedSignature?.uid && !removedSignature?.uid) {
                this.$store.dispatch("alert/" + INFO, corporateSignatureGotInserted);
            }

            if (removedSignature?.uid && !insertedSignature?.uid) {
                this.$store.dispatch("alert/" + INFO, corporateSignatureGotRemoved);
            }
        },
        async toggleSignature() {
            this.$store.dispatch(`mail/${TOGGLE_SIGNATURE}`);
        },
        $_SignatureMixin_resetAlerts() {
            this.$store.dispatch("alert/" + REMOVE, corporateSignatureGotRemoved.alert);
            this.$store.dispatch("alert/" + REMOVE, corporateSignatureGotInserted.alert);
        }
    },
    destroyed() {
        this.$_SignatureMixin_resetAlerts();
        this.$store.commit(`mail/${SET_MAIL_TIPS}`, []);
    }
};
