import i18n from "@bluemind/i18n";
import router from "@bluemind/router";
import store from "@bluemind/store";
import { messageUtils, draftUtils } from "@bluemind/mail";
import { ERROR } from "@bluemind/alert.store";
import { MimeType } from "@bluemind/email";
import { buildBasicStructure } from "./initStructure";
import { getIdentityForNewMessage, setFrom } from "../ComposerFrom";
import { SET_MESSAGE_SUBJECT, SET_MESSAGE_STRUCTURE } from "~/mutations";
import apiMessages from "~/store/api/apiMessages";
import { useAddAttachmentsCommand } from "~/commands/AddAttachmentsCommand";

const { MessageCreationModes, createEmlName } = messageUtils;
const { computeSubject } = draftUtils;
export default function useForwardEml() {
    const { maxSize, execAddAttachments } = useAddAttachmentsCommand();

    async function initForwardEml(message, related) {
        const { message: relatedMessage } = related;

        const identity = getIdentityForNewMessage();
        await setFrom(identity, message);
        store.commit(`mail/${SET_MESSAGE_STRUCTURE}`, {
            messageKey: message.key,
            structure: buildBasicStructure()
        });

        const subject = computeSubject(MessageCreationModes.FORWARD, relatedMessage);
        store.commit(`mail/${SET_MESSAGE_SUBJECT}`, { messageKey: message.key, subject });
        try {
            const content = await apiMessages.fetchComplete(relatedMessage);
            const file = new File([content], createEmlName(relatedMessage, i18n.t("mail.viewer.no.subject")), {
                type: MimeType.EML
            });
            await execAddAttachments({ files: [file], message, maxSize: maxSize.value });
        } catch {
            store.dispatch(`alert/${ERROR}`, {
                alert: { name: "mail.forward_eml.fetch", uid: "FWD_EML_UID" }
            });
            const conversation = store.state.mail.conversations.conversationByKey[relatedMessage.conversationRef.key];
            router.navigate({ name: "v:mail:conversation", params: { conversation } });
            return;
        }
        return message;
    }

    return { initForwardEml };
}
