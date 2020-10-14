import { mapGetters, mapMutations, mapState } from "vuex";
import { addSignature, removeSignature, isHtmlSignaturePresent, isTextSignaturePresent } from "~/model/signature";
import { SET_DRAFT_EDITOR_CONTENT, SET_MESSAGE_SUBJECT } from "~/mutations";

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
        ...mapGetters("root-app", ["DEFAULT_IDENTITY"]),
        signature() {
            return this.DEFAULT_IDENTITY.signature;
        },
        isSignatureInserted() {
            return (
                this.signature &&
                (this.userPrefTextOnly
                    ? isTextSignaturePresent(this.messageCompose.editorContent, this.signature)
                    : isHtmlSignaturePresent(this.messageCompose.editorContent, this.signature))
            );
        }
    },
    methods: {
        ...mapMutations("mail", { SET_DRAFT_EDITOR_CONTENT, SET_MESSAGE_SUBJECT }),
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
            this.$refs.content.updateHtmlComposer();
        }
    }
};
