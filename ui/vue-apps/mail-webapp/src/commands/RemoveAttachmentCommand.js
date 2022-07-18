import { REMOVE_ATTACHMENT } from "~/actions";

export default {
    commands: {
        async removeAttachment({ attachment, message }) {
            await this.$store.dispatch(`mail/${REMOVE_ATTACHMENT}`, {
                messageKey: message.key,
                attachmentAddress: attachment.address,
                messageCompose: this.$store.state.mail.messageCompose
            });
        }
    }
};
