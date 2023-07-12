import { inject } from "@bluemind/inject";
import { draftUtils, messageUtils, folderUtils } from "@bluemind/mail";
import { REMOVE_MESSAGE_HEADER, SET_MESSAGE_FROM, SET_MESSAGE_HEADERS, SET_PERSONAL_SIGNATURE } from "~/mutations";
import { CURRENT_MAILBOX, MAILBOX_SENT } from "~/getters";
import { MailboxAdaptor } from "../store/helpers/MailboxAdaptor";
import { mapGetters } from "vuex";

const { DEFAULT_FOLDERS } = folderUtils;
const { computeIdentityForReplyOrForward, findIdentityFromMailbox } = draftUtils;
const { MessageHeader } = messageUtils;

export default {
    data() {
        return {
            userPrefTextOnly: false // FIXME: https://forge.bluemind.net/jira/browse/FEATWEBML-88
        };
    },
    computed: {
        ...mapGetters("root-app", ["DEFAULT_IDENTITY"])
    },
    methods: {
        async setFrom(identity, message) {
            this.$store.commit("mail/" + SET_MESSAGE_FROM, {
                messageKey: message.key,
                from: { address: identity.email, dn: identity.displayname }
            });
            const fullIdentity = this.setIdentity(identity);
            const rawIdentity = await inject("UserMailIdentitiesPersistence").get(fullIdentity.id);

            let destinationMailboxUid;
            if (rawIdentity.sentFolder !== DEFAULT_FOLDERS.SENT) {
                destinationMailboxUid = rawIdentity.mailboxUid;
            } else {
                destinationMailboxUid = inject("UserSession").userId;
            }

            const mailboxes = this.$store.state.mail.mailboxes;
            let mailbox = mailboxes[`user.${destinationMailboxUid}`] || mailboxes[destinationMailboxUid];
            let sentFolderUid;
            if (mailbox) {
                sentFolderUid = this.$store.getters["mail/" + MAILBOX_SENT](mailbox)?.remoteRef.uid;
            } else {
                const mailboxContainer = await inject("ContainersPersistence").get(
                    "mailbox:acls-" + destinationMailboxUid
                );
                mailbox = MailboxAdaptor.fromMailboxContainer(mailboxContainer);
                const folderName = [mailbox.root, "Sent"].filter(Boolean).join("%2f");
                sentFolderUid = (await inject("MailboxFoldersPersistence", mailbox.remoteRef.uid).byName(folderName))
                    ?.uid;
            }
            if (sentFolderUid) {
                this.$store.commit(`mail/${REMOVE_MESSAGE_HEADER}`, {
                    messageKey: message.key,
                    headerName: MessageHeader.X_BM_SENT_FOLDER
                });
                const xBmSentFolder = { name: MessageHeader.X_BM_SENT_FOLDER, values: [sentFolderUid] };
                this.$store.commit("mail/" + SET_MESSAGE_HEADERS, {
                    messageKey: message.key,
                    headers: [...message.headers, xBmSentFolder]
                });
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
        setIdentity(identity) {
            const fullIdentity = this.$store.state["root-app"].identities.find(
                i => i.email === identity.email && i.displayname === identity.displayname
            );
            this.$store.commit("mail/" + SET_PERSONAL_SIGNATURE, { html: fullIdentity.signature, id: fullIdentity.id });
            return fullIdentity;
        }
    }
};
