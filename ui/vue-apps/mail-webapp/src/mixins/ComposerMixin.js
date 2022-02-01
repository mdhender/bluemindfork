import { mapMutations, mapState } from "vuex";
import { INFO, REMOVE } from "@bluemind/alert.store";
import { addSignature, removeSignature, isSignaturePresent } from "~/model/signature";
import { RESET_COMPOSER, SET_DRAFT_EDITOR_CONTENT } from "~/mutations";
import { IS_SENDER_SHOWN } from "~/getters";
import { ComposerFromMixin } from "~/mixins";

const corporateSignatureGotInserted = {
    alert: { name: "mail.CORPORATE_SIGNATURE_INSERTED", uid: "CORPORATE_SIGNATURE" },
    options: { area: "right-panel", renderer: "CorporateSignatureAlert" }
};
const corporateSignatureGotRemoved = {
    alert: { name: "mail.CORPORATE_SIGNATURE_REMOVED", uid: "CORPORATE_SIGNATURE" },
    options: { area: "right-panel", renderer: "CorporateSignatureAlert" }
};

export default {
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    mixins: [ComposerFromMixin],
    data() {
        return {
            userPrefIsMenuBarOpened: false, // TODO: initialize this with user setting
            draggedFilesCount: -1
        };
    },
    computed: {
        ...mapState("mail", ["messageCompose"]),
        ...mapState("root-app", ["identities"]),
        signature() {
            return this.identities.find(
                i => i.email === this.message.from.address && i.displayname === this.message.from.dn
            ).signature;
        },
        isSignatureInserted() {
            return isSignaturePresent(this.messageCompose.editorContent, this.userPrefTextOnly);
        },
        isSenderShown() {
            return this.$store.getters["mail/" + IS_SENDER_SHOWN](this.$store.state.settings);
        },
        $_ComposerMixin_insertSignaturePref() {
            return this.$store.state.settings.insert_signature;
        }
    },
    watch: {
        "messageCompose.corporateSignature"(corporateSignature, oldCorporateSignature) {
            if (corporateSignature && this.isSignatureInserted) {
                this.$store.dispatch("alert/" + INFO, corporateSignatureGotInserted);
                this.$_ComposerMixin_SET_DRAFT_EDITOR_CONTENT(
                    removeSignature(this.messageCompose.editorContent, this.userPrefTextOnly, this.signature)
                );
            } else if (
                oldCorporateSignature &&
                !corporateSignature &&
                this.signature &&
                this.$_ComposerMixin_insertSignaturePref === "true"
            ) {
                this.$store.dispatch("alert/" + INFO, corporateSignatureGotRemoved);
                this.$_ComposerMixin_SET_DRAFT_EDITOR_CONTENT(
                    addSignature(this.messageCompose.editorContent, this.userPrefTextOnly, this.signature)
                );
            }
        }
    },
    destroyed() {
        this.$store.commit("mail/" + RESET_COMPOSER);
        this.$store.dispatch("alert/" + REMOVE, corporateSignatureGotRemoved.alert);
        this.$store.dispatch("alert/" + REMOVE, corporateSignatureGotInserted.alert);
    },
    methods: {
        ...mapMutations("mail", { $_ComposerMixin_SET_DRAFT_EDITOR_CONTENT: SET_DRAFT_EDITOR_CONTENT }),
        toggleSignature() {
            if (!this.isSignatureInserted) {
                this.$_ComposerMixin_SET_DRAFT_EDITOR_CONTENT(
                    addSignature(this.messageCompose.editorContent, this.userPrefTextOnly, this.signature)
                );
            } else {
                this.$_ComposerMixin_SET_DRAFT_EDITOR_CONTENT(
                    removeSignature(this.messageCompose.editorContent, this.userPrefTextOnly, this.signature)
                );
            }
        },
        async checkAndRepairFrom() {
            const matchingIdentity = this.identities.find(
                i => i.email === this.message.from.address && i.displayname === this.message.from.dn
            );
            if (!matchingIdentity) {
                // eslint-disable-next-line no-console
                console.warn("identity changed because no identity matched message.from");
                const defaultIdentity = this.identities.find(identity => !!identity.isDefault);
                await this.setFrom(defaultIdentity, this.message);
            }
        }
    }
};
