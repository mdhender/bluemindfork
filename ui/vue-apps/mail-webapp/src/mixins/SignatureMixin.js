import { mapState } from "vuex";
import { INFO, REMOVE } from "@bluemind/alert.store";
import { draftUtils, mailTipUtils, signatureUtils } from "@bluemind/mail";
import {
    SET_DRAFT_EDITOR_CONTENT,
    SET_DISCLAIMER,
    SET_CORPORATE_SIGNATURE,
    UNSET_CORPORATE_SIGNATURE
} from "~/mutations";

const { isNewMessage } = draftUtils;
const {
    CORPORATE_SIGNATURE_PLACEHOLDER,
    CORPORATE_SIGNATURE_SELECTOR,
    DISCLAIMER_SELECTOR,
    PERSONAL_SIGNATURE_SELECTOR,
    isCorporateSignature,
    isDisclaimer,
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
        isSignatureInserted: {
            type: Boolean,
            required: true
        }
    },
    computed: {
        ...mapState("mail", {
            personalSignature: state => state.messageCompose.personalSignature,
            mailTips: state => state.messageCompose.mailTips,
            $_SignatureMixin_corporateSignature: state => state.messageCompose.corporateSignature,
            $_SignatureMixin_disclaimer: state => state.messageCompose.disclaimer,
            $_SignatureMixin_editorContent: state => state.messageCompose.editorContent,
            $_SignatureMixin_insertSignaturePref() {
                return this.$store.state.settings.insert_signature;
            }
        })
    },
    data() {
        return { $_SignatureMixin_checkCorporateSignatureDone: false };
    },
    created() {
        this.$_SignatureMixin_refreshSignature();
    },
    watch: {
        "message.from"() {
            this.$_SignatureMixin_refreshSignature();
        },
        mailTips: {
            handler(mailTips) {
                if (mailTips.length > 0) {
                    const matchingTips = mailTips[0].matchingTips;

                    const disclaimer = matchingTips.find(isDisclaimer);
                    this.$store.commit(SET_DISCLAIMER, disclaimer ? JSON.parse(disclaimer.value) : null);

                    const corporateSignature = matchingTips.find(isCorporateSignature);
                    if (corporateSignature) {
                        this.$store.commit(SET_CORPORATE_SIGNATURE, JSON.parse(corporateSignature.value));
                    } else {
                        this.$store.commit(UNSET_CORPORATE_SIGNATURE);
                    }
                } else {
                    this.$store.commit(SET_DISCLAIMER, null);
                    this.$store.commit(UNSET_CORPORATE_SIGNATURE);
                }
                this.$_SignatureMixin_checkCorporateSignatureDone = true;
            },
            immediate: true
        },
        personalSignature: {
            async handler(personalSignature, old) {
                const editorRef = await this.getEditorRef();
                if (
                    this.$_SignatureMixin_corporateSignature &&
                    this.$_SignatureMixin_containsPersonalSignature(personalSignature, old, editorRef)
                ) {
                    this.$store.dispatch("alert/" + INFO, corporateSignatureGotInserted);
                    editorRef.removeContent(PERSONAL_SIGNATURE_SELECTOR(personalSignature.id));
                    if (old) {
                        editorRef.removeContent(PERSONAL_SIGNATURE_SELECTOR(old.id));
                    }
                }
                if (old && editorRef.hasContent(PERSONAL_SIGNATURE_SELECTOR(old.id))) {
                    editorRef.removeContent(PERSONAL_SIGNATURE_SELECTOR(old.id));
                }
                if (
                    !this.$_SignatureMixin_corporateSignature &&
                    personalSignature &&
                    !editorRef.hasContent(PERSONAL_SIGNATURE_SELECTOR(personalSignature.id)) &&
                    this.$_SignatureMixin_insertSignaturePref === "true"
                ) {
                    const content = wrapPersonalSignature({ html: personalSignature.html, id: personalSignature.id });
                    editorRef.insertContent(content, { triggerOnChange: !isNewMessage(this.message) });
                    editorRef.insertContent(document.createElement("br"));
                    this.$store.commit(`mail/${SET_DRAFT_EDITOR_CONTENT}`, editorRef.getContent());
                }
                this.$_SignatureMixin_onPersonalSignatureChange();
            },
            immediate: true
        },
        $_SignatureMixin_corporateSignature: {
            async handler(corpSign, old) {
                const editorRef = await this.getEditorRef();
                if (old) {
                    const options = old.usePlaceholder
                        ? { movable: CORPORATE_SIGNATURE_PLACEHOLDER }
                        : { editable: false };
                    editorRef.removeContent(CORPORATE_SIGNATURE_SELECTOR, options);
                }
                if (corpSign) {
                    const options = {};
                    if (corpSign.usePlaceholder) {
                        options.movable = CORPORATE_SIGNATURE_PLACEHOLDER;
                        options.tooltip = this.$t("mail.compose.corporate_signature.use_placeholder");
                    } else {
                        options.editable = false;
                        options.tooltip = this.$t("mail.compose.corporate_signature.read_only");
                    }
                    editorRef.insertContent(wrapCorporateSignature(corpSign.html), options);
                    this.$store.commit(`mail/${SET_DRAFT_EDITOR_CONTENT}`, editorRef.getContent());
                }

                if (corpSign && editorRef.hasContent(PERSONAL_SIGNATURE_SELECTOR(this.personalSignature.id))) {
                    editorRef.removeContent(PERSONAL_SIGNATURE_SELECTOR(this.personalSignature.id));
                    this.$store.dispatch("alert/" + INFO, corporateSignatureGotInserted);
                }

                if (old && !corpSign) {
                    this.$store.dispatch("alert/" + INFO, corporateSignatureGotRemoved);
                    if (
                        this.personalSignature &&
                        !editorRef.hasContent(PERSONAL_SIGNATURE_SELECTOR(this.personalSignature.id)) &&
                        this.$_SignatureMixin_insertSignaturePref === "true"
                    ) {
                        editorRef.insertContent(
                            wrapPersonalSignature({ html: this.personalSignature.html, id: this.personalSignature.id })
                        );
                        editorRef.insertContent(document.createElement("br"));
                        this.$store.commit(`mail/${SET_DRAFT_EDITOR_CONTENT}`, editorRef.getContent());
                    }
                }
                this.$_SignatureMixin_onPersonalSignatureChange();
                this.$_SignatureMixin_removePlaceholder();
            },
            immediate: true
        },
        $_SignatureMixin_disclaimer: {
            async handler(disclaimer) {
                const editorRef = await this.getEditorRef();
                if (disclaimer) {
                    const options = {
                        editable: false,
                        tooltip: this.$t("mail.compose.corporate_signature.read_only")
                    };
                    editorRef.insertContent(wrapDisclaimer(disclaimer.html), options);
                } else {
                    editorRef.removeContent(DISCLAIMER_SELECTOR, { editable: false });
                }
            },
            immediate: true
        }
    },
    methods: {
        async toggleSignature() {
            const editorRef = await this.getEditorRef();
            const selector = PERSONAL_SIGNATURE_SELECTOR(this.personalSignature.id);
            if (editorRef.hasContent(selector)) {
                editorRef.removeContent(selector);
            } else {
                editorRef.insertContent(
                    wrapPersonalSignature({ html: this.personalSignature.html, id: this.personalSignature.id })
                );
                editorRef.insertContent(document.createElement("br"));
            }
            this.$_SignatureMixin_onPersonalSignatureChange();
        },
        async $_SignatureMixin_onPersonalSignatureChange() {
            const editorRef = await this.getEditorRef();
            const isSignatureInserted = editorRef.hasContent(PERSONAL_SIGNATURE_SELECTOR(this.personalSignature.id));
            this.$emit("update:is-signature-inserted", isSignatureInserted);
        },
        $_SignatureMixin_resetAlerts() {
            this.$store.dispatch("alert/" + REMOVE, corporateSignatureGotRemoved.alert);
            this.$store.dispatch("alert/" + REMOVE, corporateSignatureGotInserted.alert);
        },
        $_SignatureMixin_containsPersonalSignature(personalSignature, old, editorRef) {
            return (
                (personalSignature && editorRef.hasContent(PERSONAL_SIGNATURE_SELECTOR(personalSignature.id))) ||
                (old && editorRef.hasContent(PERSONAL_SIGNATURE_SELECTOR(old.id)))
            );
        },
        async $_SignatureMixin_refreshSignature() {
            await this.$execute("get-mail-tips", { context: getMailTipContext(this.message) });
        },
        async $_SignatureMixin_removePlaceholder() {
            if (this.$_SignatureMixin_checkCorporateSignatureDone) {
                const editorRef = await this.getEditorRef();
                // remove placeholder when it has not been replaced by a signature
                // case where signature has changed and doesnt match draft anymore
                editorRef.removeText(CORPORATE_SIGNATURE_PLACEHOLDER);
            }
        }
    },
    destroyed() {
        this.$_SignatureMixin_resetAlerts();
    }
};
