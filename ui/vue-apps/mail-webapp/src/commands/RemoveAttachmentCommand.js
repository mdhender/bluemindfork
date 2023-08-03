import store from "@bluemind/store";
import { REMOVE_ATTACHMENT } from "~/actions";
import { useCommand } from "@bluemind/command";

async function removeAttachment({ attachment, message }) {
    await store.dispatch(`mail/${REMOVE_ATTACHMENT}`, {
        messageKey: message.key,
        attachment,
        messageCompose: store.state.mail.messageCompose
    });
}

export default { commands: { removeAttachment } };
export const useRemoveAttachmentCommand = () => useCommand("removeAttachment", removeAttachment);
