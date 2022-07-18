import { SAVE_MESSAGE } from "~/actions";
import { SET_MESSAGE_COMPOSING } from "~/mutations";

export default {
    methods: {
        saveAndCloseOpenDrafts(conversation) {
            return Promise.all(
                conversation.messages.map(async messageKey => {
                    const message = this.$store.state.mail.conversations.messages[messageKey];
                    if (message?.composing) {
                        await this.$store.dispatch(`mail/${SAVE_MESSAGE}`, {
                            draft: message,
                            messageCompose: this.$store.state.mail.messageCompose,
                            files: this.message.attachments.map(({ fileKey }) => this.$store.state.mail.files[fileKey])
                        });
                        this.$store.commit(`mail/${SET_MESSAGE_COMPOSING}`, { messageKey, composing: false });
                    }
                })
            );
        }
    }
};
