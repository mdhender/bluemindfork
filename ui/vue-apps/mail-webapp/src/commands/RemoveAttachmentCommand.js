import store from "@bluemind/store";
import { REMOVE_ATTACHMENT } from "~/actions";
import { useCommand } from "@bluemind/command";

async function removeAttachment({ address, message }) {
    await store.dispatch(`mail/${REMOVE_ATTACHMENT}`, {
        messageKey: message.key,
        address
    });
}

export default { commands: { removeAttachment } };
export const useRemoveAttachmentCommand = () => useCommand("removeAttachment", removeAttachment);
