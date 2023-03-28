import { inject } from "@bluemind/inject";
import { extractFolderUid } from "@bluemind/mbox";
import { conversationUtils } from "@bluemind/mail";
import { FolderAdaptor } from "~/store/folders/helpers/FolderAdaptor";

export default {
    computed: {
        link() {
            const conversation = conversationUtils.createConversationStub(
                this.originalMessage.remoteRef.internalId,
                this.originalMessage.folderRef
            );
            const folder = this.$store.state.mail.folders[this.originalMessage.folderRef.key];
            return {
                name: "v:mail:conversation",
                params: {
                    conversation,
                    folder: folder?.path,
                    mailbox: this.$store.state.mail.mailboxes[folder?.mailboxRef.key]?.name
                }
            };
        }
    },
    methods: {
        async findMessage(messageId) {
            const messages = this.$store.state.mail.conversations.messages;
            // search in store
            let message = Object.values(messages).find(value => value?.messageId === messageId);
            if (!message) {
                // search on server
                const mboxUid = this.$store.state.mail.folders[this.message.folderRef.key].mailboxRef.uid;
                const searchResult = await inject("MailboxFoldersPersistence", mboxUid).searchItems({
                    query: { messageId, maxResults: 1, scope: { folderScope: {} } }
                });
                const result = searchResult?.totalResults > 0 && searchResult.results[0];
                const fromOrToFn = fromOrTo => {
                    const isArray = Array.isArray(fromOrTo);
                    let asArray = isArray ? fromOrTo : [fromOrTo];
                    const normalized = asArray.map(({ displayName, address }) => ({ dn: displayName, address }));
                    return isArray ? normalized : normalized[0];
                };
                message = result
                    ? {
                          subject: result.subject,
                          date: new Date(result.date),
                          from: fromOrToFn(result.from),
                          to: fromOrToFn(result.to),
                          remoteRef: { internalId: result.itemId, imapUid: result.imapUid },
                          folderRef: FolderAdaptor.toRef(extractFolderUid(result.containerUid))
                      }
                    : undefined;
            }
            return message;
        }
    }
};
