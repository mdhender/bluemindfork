import { useCommand } from "@bluemind/command";
import { messageUtils } from "@bluemind/mail";
import initReplyOrForward from "../composables/composer/init/initReplyOrForward";
const { MessageCreationModes } = messageUtils;

async function forward({ message, previousInfos }) {
    const newMessage = await initReplyOrForward(message, MessageCreationModes.FORWARD, previousInfos);
    return { message: newMessage, previousInfos };
}

export default { commands: { forward } };
export const useForwardCommand = () => useCommand("forward", forward);
