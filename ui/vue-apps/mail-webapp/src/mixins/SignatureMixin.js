import { mapState } from "vuex";
import { INFO, REMOVE } from "@bluemind/alert.store";
import { draftUtils, signatureUtils } from "@bluemind/mail";
import { CHECK_CORPORATE_SIGNATURE } from "~/actions";
import { SET_DRAFT_EDITOR_CONTENT } from "~/mutations";

import WaitForMixin from "./WaitForMixin";

const { isNewMessage } = draftUtils;
const {
    CORPORATE_SIGNATURE_PLACEHOLDER,
    CORPORATE_SIGNATURE_SELECTOR,
    PERSONAL_SIGNATURE_SELECTOR,
    wrapCorporateSignature,
    wrapPersonalSignature
} = signatureUtils;
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
    mixins: [WaitForMixin],
    props: {
        isSignatureInserted: {
            type: Boolean,
            required: true
        }
    },
    computed: {
        ...mapState("mail", {
            personalSignature: state => state.messageCompose.personalSignature,
            $_SignatureMixin_corporateSignature: state => state.messageCompose.corporateSignature,
            $_SignatureMixin_editorContent: state => state.messageCompose.editorContent,
            $_SignatureMixin_insertSignaturePref() {
                return this.$store.state.settings.insert_signature;
            }
        }),
        $_SignatureMixin_signature() {
            return this.$store.state["root-app"].identities.find(
                i => i.email === this.message.from.address && i.displayname === this.message.from.dn
            ).signature;
        }
    },
    data() {
        return {
            componentGotMounted: false,
            $_SignatureMixin_checkCorporateSignatureDone: false
        };
    },
    created() {
        this.$_SignatureMixin_refreshSignature();
    },
    mounted() {
        this.componentGotMounted = true;
    },
    watch: {
        "message.from"() {
            this.$_SignatureMixin_refreshSignature();
        },
        personalSignature: {
            async handler(personalSignature, old) {
                const editorRef = await this.$_SignatureMixin_getEditorRef();
                if (
                    this.$_SignatureMixin_corporateSignature &&
                    this.$_SignatureMixin_containsPersonalSignature(personalSignature, old, editorRef)
                ) {
                    this.$store.dispatch("alert/" + INFO, corporateSignatureGotInserted);
                    editorRef.removeContent(PERSONAL_SIGNATURE_SELECTOR(personalSignature.id));
                    editorRef.removeContent(PERSONAL_SIGNATURE_SELECTOR(old.id));
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
                    const content = wrapPersonalSignature(personalSignature);
                    editorRef.insertContent(content, { triggerOnChange: !isNewMessage(this.message) });
                    this.$store.commit(`mail/${SET_DRAFT_EDITOR_CONTENT}`, editorRef.getContent());
                }
                this.$_SignatureMixin_onPersonalSignatureChange();
            },
            immediate: true
        },
        $_SignatureMixin_corporateSignature: {
            async handler(corpSign, old) {
                const editorRef = await this.$_SignatureMixin_getEditorRef();
                if (old?.usePlaceholder) {
                    editorRef.removeContent(CORPORATE_SIGNATURE_SELECTOR, { editable: true });
                }
                if (corpSign?.usePlaceholder) {
                    const options = {
                        editable: true,
                        placeholder: CORPORATE_SIGNATURE_PLACEHOLDER,
                        tooltip: this.$t("mail.compose.corporate_signature.use_placeholder")
                    };
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
                        editorRef.insertContent(wrapPersonalSignature(this.personalSignature));
                        this.$store.commit(`mail/${SET_DRAFT_EDITOR_CONTENT}`, editorRef.getContent());
                    }
                }
                this.$_SignatureMixin_onPersonalSignatureChange();
                this.$_SignatureMixin_removePlaceholder();
            },
            immediate: true
        }
    },
    methods: {
        async toggleSignature() {
            const editorRef = await this.$_SignatureMixin_getEditorRef();
            const selector = PERSONAL_SIGNATURE_SELECTOR(this.personalSignature.id);
            if (editorRef.hasContent(selector)) {
                editorRef.removeContent(selector);
            } else {
                editorRef.insertContent(wrapPersonalSignature(this.personalSignature));
            }
            this.$_SignatureMixin_onPersonalSignatureChange();
        },
        async $_SignatureMixin_onPersonalSignatureChange() {
            const editorRef = await this.$_SignatureMixin_getEditorRef();
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
        async $_SignatureMixin_getEditorRef() {
            await this.$waitFor("componentGotMounted");
            await this.$waitFor("loading", loading => loading === false); // component must be loaded to be able to use ref
            return this.$refs["message-content"];
        },
        async $_SignatureMixin_refreshSignature() {
            await this.$store.dispatch("mail/" + CHECK_CORPORATE_SIGNATURE, { message: this.message });
            this.$_SignatureMixin_checkCorporateSignatureDone = true;
        },
        async $_SignatureMixin_removePlaceholder() {
            if (this.$_SignatureMixin_checkCorporateSignatureDone) {
                const editorRef = await this.$_SignatureMixin_getEditorRef();
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
