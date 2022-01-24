import { mapMutations, mapState } from "vuex";
import { INFO, REMOVE } from "@bluemind/alert.store";
import { inject } from "@bluemind/inject";
import { CHECK_CORPORATE_SIGNATURE } from "~/actions";
import { MessageHeader } from "~/model/message";
import { addSignature, removeSignature, replaceSignature, isSignaturePresent } from "~/model/signature";
import { RESET_COMPOSER, SET_DRAFT_EDITOR_CONTENT, SET_MESSAGE_FROM, SET_MESSAGE_HEADERS } from "~/mutations";
import { IS_SENDER_SHOWN, MAILBOX_SENT } from "~/getters";
import { DEFAULT_FOLDER_NAMES } from "~/store/folders/helpers/DefaultFolders";
import { MailboxAdaptor } from "../store/helpers/MailboxAdaptor";

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
        signature() {
            return this.identities.find(
                i => i.email === this.message.from.address && i.displayname === this.message.from.dn
            ).signature;
        },
        isSignatureInserted() {
            return isSignaturePresent(this.messageCompose.editorContent, this.userPrefTextOnly);
        },
        isSenderShown() {
            return this.$store.getters["mail/" + IS_SENDER_SHOWN](this.userSettings);
        },
        $_ComposerMixin_insertSignaturePref() {
            return this.$store.state.session.settings.remote.insert_signature;
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
        async setFrom(identity) {
            this.$store.commit("mail/" + SET_MESSAGE_FROM, {
                messageKey: this.message.key,
                from: { address: identity.email, dn: identity.displayname }
            });
            const fullIdentity = this.identities.find(
                i => i.email === identity.email && i.displayname === identity.displayname
            );
            const rawIdentity = await inject("UserMailIdentitiesPersistence").get(fullIdentity.id);
            if (rawIdentity.sentFolder !== DEFAULT_FOLDER_NAMES.SENT) {
                const mailboxes = this.$store.state.mail.mailboxes;
                let mailbox = mailboxes[`user.${rawIdentity.mailboxUid}`] || mailboxes[rawIdentity.mailboxUid];
                let sentFolderUid;
                if (mailbox) {
                    sentFolderUid = this.$store.getters["mail/" + MAILBOX_SENT](mailbox)?.remoteRef.uid;
                } else {
                    const mailboxContainer = await inject("ContainersPersistence").get(
                        "mailbox:acls-" + rawIdentity.mailboxUid
                    );
                    mailbox = MailboxAdaptor.fromMailboxContainer(mailboxContainer);
                    const folderName = [mailbox.root, "Sent"].filter(Boolean).join("%2f");
                    sentFolderUid = (
                        await inject("MailboxFoldersPersistence", mailbox.remoteRef.uid).byName(folderName)
                    )?.uid;
                }
                if (sentFolderUid) {
                    const headers = this.message.headers;
                    const xBmSentFolder = { name: MessageHeader.X_BM_SENT_FOLDER, values: [sentFolderUid] };
                    this.$store.commit("mail/" + SET_MESSAGE_HEADERS, {
                        messageKey: this.message.key,
                        headers: [...headers, xBmSentFolder]
                    });
                    // FIXME: need A SET_MESSAGE_HEADER mutation
                }
            }
            await this.$store.dispatch("mail/" + CHECK_CORPORATE_SIGNATURE, { message: this.message });
            if (this.isSignatureInserted && !this.messageCompose.corporateSignature) {
                this.$_ComposerMixin_SET_DRAFT_EDITOR_CONTENT(
                    replaceSignature(this.messageCompose.editorContent, this.userPrefTextOnly, this.signature)
                );
            }
        },
        checkAndRepairFrom() {
            const matchingIdentity = this.identities.find(
                i => i.email === this.message.from.address && i.displayname === this.message.from.dn
            );
            if (!matchingIdentity) {
                // eslint-disable-next-line no-console
                console.warn("identity changed because no identity matched message.from");
                const defaultIdentity = this.identities.find(identity => !!identity.isDefault);
                this.setFrom(defaultIdentity);
            }
        }
    }
};
