import { mapState } from "vuex";
import { inject } from "@bluemind/inject";
import { computeIdentityForReplyOrForward, findIdentityFromMailbox } from "~/model/draft";
import { MessageHeader } from "~/model/message";
import { replaceSignature } from "~/model/signature";
import { CHECK_CORPORATE_SIGNATURE } from "~/actions";
import { SET_DRAFT_EDITOR_CONTENT, SET_MESSAGE_FROM, SET_MESSAGE_HEADERS } from "~/mutations";
import { CURRENT_MAILBOX, MAILBOX_SENT } from "~/getters";
import { DEFAULT_FOLDER_NAMES } from "~/store/folders/helpers/DefaultFolders";
import { MailboxAdaptor } from "../store/helpers/MailboxAdaptor";

export default {
    data() {
        return {
            userPrefTextOnly: false // FIXME: https://forge.bluemind.net/jira/browse/FEATWEBML-88
        };
    },
    computed: {
        ...mapState("root-app", { $_ComposerFromMixin_identities: "identities" })
    },
    methods: {
        async setFrom(identity, message) {
            this.$store.commit("mail/" + SET_MESSAGE_FROM, {
                messageKey: message.key,
                from: { address: identity.email, dn: identity.displayname }
            });
            const fullIdentity = this.$_ComposerFromMixin_identities.find(
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
                    const headers = message.headers;
                    const xBmSentFolder = { name: MessageHeader.X_BM_SENT_FOLDER, values: [sentFolderUid] };
                    this.$store.commit("mail/" + SET_MESSAGE_HEADERS, {
                        messageKey: message.key,
                        headers: [...headers, xBmSentFolder]
                    });
                }
            }
        },
        getIdentityForNewMessage() {
            const currentMailbox = this.$store.getters["mail/" + CURRENT_MAILBOX];
            const identities = this.$store.state["root-app"].identities;
            const autoSelectFromPref = this.$store.state.settings.auto_select_from;
            const defaultIdentity = this.$store.getters["root-app/DEFAULT_IDENTITY"];
            if (autoSelectFromPref === "replies_and_new_messages") {
                return findIdentityFromMailbox(currentMailbox, identities, defaultIdentity);
            }
            return defaultIdentity;
        },
        getIdentityForReplyOrForward(previousMessage) {
            const currentMailbox = this.$store.getters["mail/" + CURRENT_MAILBOX];
            const identities = this.$store.state["root-app"].identities;
            const autoSelectFromPref = this.$store.state.settings.auto_select_from;
            if (autoSelectFromPref === "only_replies" || autoSelectFromPref === "replies_and_new_messages") {
                const defaultIdentity = this.$store.getters["root-app/DEFAULT_IDENTITY"];
                return computeIdentityForReplyOrForward(previousMessage, identities, currentMailbox, defaultIdentity);
            }
            return this.getIdentityForNewMessage();
        },
        async changeFrom(identity, message) {
            await this.setFrom(identity, message);
            const content = await this.handleSignature(message, this.$store.state.mail.messageCompose.editorContent);
            this.$store.commit(`mail/${SET_DRAFT_EDITOR_CONTENT}`, content);
        },
        async handleSignature(message, content, preservePersonalSignature = false) {
            await this.$store.dispatch("mail/" + CHECK_CORPORATE_SIGNATURE, { message });
            const signature = this.$store.state["root-app"].identities.find(
                i => i.email === message.from.address && i.displayname === message.from.dn
            ).signature;
            if (
                !this.$store.state.mail.messageCompose.corporateSignature &&
                signature &&
                this.$store.state.settings.insert_signature === "true" &&
                !preservePersonalSignature
            ) {
                return replaceSignature(content, this.userPrefTextOnly, signature);
            }
            return content;
        }
    }
};
