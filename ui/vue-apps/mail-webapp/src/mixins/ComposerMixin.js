import { mapGetters, mapMutations, mapState } from "vuex";
import { INFO, REMOVE } from "@bluemind/alert.store";
import {
    addSignature,
    removeSignature,
    replaceSignature,
    isHtmlSignaturePresent,
    isTextSignaturePresent
} from "~/model/signature";
import { SET_DRAFT_EDITOR_CONTENT, SET_MESSAGE_SUBJECT, SHOW_SENDER } from "~/mutations";
import { IS_SENDER_SHOWN } from "~/getters";

const corporateSignatureGotInserted = {
    alert: { name: "mail.CORPORATE_SIGNATURE_INSERTED", uid: "CORPORATE_SIGNATURE_INSERTED" },
    options: {
        area: "right-panel",
        renderer: "ReadMoreAlert",
        link: "https://forge.bluemind.net/confluence/display/BM4/Signatures+d%27entreprise"
    }
};

const corporateSignatureGotRemoved = {
    alert: { name: "mail.CORPORATE_SIGNATURE_REMOVED", uid: "CORPORATE_SIGNATURE_REMOVED" },
    options: {
        area: "right-panel",
        renderer: "ReadMoreAlert",
        link: "https://forge.bluemind.net/confluence/display/BM4/Signatures+d%27entreprise"
    }
};

export default {
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            userPrefIsMenuBarOpened: false, // TODO: initialize this with user setting
            draggedFilesCount: -1
        };
    },
    computed: {
        ...mapState("mail", ["messageCompose"]),
        ...mapState("root-app", ["identities"]),
        ...mapState("session", { userSettings: ({ settings }) => settings.remote }),
        ...mapGetters("mail", { IS_SENDER_SHOWN }),
        signature() {
            return this.identities.find(
                i => i.email === this.message.from.address && i.displayname === this.message.from.dn
            ).signature;
        },
        isSignatureInserted() {
            return (
                this.signature &&
                (this.userPrefTextOnly
                    ? isTextSignaturePresent(this.messageCompose.editorContent, this.signature)
                    : isHtmlSignaturePresent(this.messageCompose.editorContent, this.signature))
            );
        },
        isSenderShown() {
            return this.IS_SENDER_SHOWN(this.userSettings);
        },
        $_ComposerMixin_insertSignaturePref() {
            return this.$store.state.session.settings.remote.insert_signature;
        }
    },
    watch: {
        "messageCompose.corporateSignature"(corporateSignature, oldCorporateSignature) {
            if (corporateSignature && this.isSignatureInserted) {
                this.$store.dispatch("alert/" + REMOVE, corporateSignatureGotRemoved.alert);
                this.$store.dispatch("alert/" + INFO, corporateSignatureGotInserted);
                this.SET_DRAFT_EDITOR_CONTENT(
                    removeSignature(this.messageCompose.editorContent, this.userPrefTextOnly, this.signature)
                );
            } else if (
                oldCorporateSignature &&
                !corporateSignature &&
                this.signature &&
                this.$_ComposerMixin_insertSignaturePref === "true"
            ) {
                this.$store.dispatch("alert/" + REMOVE, corporateSignatureGotInserted.alert);
                this.$store.dispatch("alert/" + INFO, corporateSignatureGotRemoved);
                this.SET_DRAFT_EDITOR_CONTENT(
                    addSignature(this.messageCompose.editorContent, this.userPrefTextOnly, this.signature)
                );
            }
        }
    },
    destroyed() {
        this.SHOW_SENDER(false);
        this.$store.dispatch("alert/" + REMOVE, corporateSignatureGotRemoved.alert);
        this.$store.dispatch("alert/" + REMOVE, corporateSignatureGotInserted.alert);
    },
    methods: {
        ...mapMutations("mail", { SET_DRAFT_EDITOR_CONTENT, SET_MESSAGE_SUBJECT, SHOW_SENDER }),
        updateSubject(subject) {
            this.SET_MESSAGE_SUBJECT({ messageKey: this.message.key, subject });
            this.debouncedSave();
        },
        toggleSignature() {
            if (!this.isSignatureInserted) {
                this.SET_DRAFT_EDITOR_CONTENT(
                    addSignature(this.messageCompose.editorContent, this.userPrefTextOnly, this.signature)
                );
            } else {
                this.SET_DRAFT_EDITOR_CONTENT(
                    removeSignature(this.messageCompose.editorContent, this.userPrefTextOnly, this.signature)
                );
            }
        },
        updateSignatureIfNeeded() {
            if (this.isSignatureInserted) {
                this.SET_DRAFT_EDITOR_CONTENT(
                    replaceSignature(this.messageCompose.editorContent, this.userPrefTextOnly, this.signature)
                );
            }
        }
    }
};
