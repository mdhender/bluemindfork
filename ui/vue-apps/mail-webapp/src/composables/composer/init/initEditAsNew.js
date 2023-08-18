import store from "@bluemind/store";
import router from "@bluemind/router";
import { draftUtils, partUtils } from "@bluemind/mail";
import { getIdentityForNewMessage, setFrom } from "../ComposerFrom";
import { useComposerMerge } from "../ComposerMerge";
import { SET_MESSAGE_STRUCTURE } from "~/mutations";
import { buildEditAsNewStructure } from "./initStructure";

const { COMPOSER_CAPABILITIES } = draftUtils;
const { getPartsFromCapabilities } = partUtils;

export default async function initEditAsNew(message, related) {
    const { mergeBody, mergeHeaders, mergeRecipients, mergeSubject } = useComposerMerge();
    const {
        message: relatedMessage,
        attachments: relatedAttachments,
        inlinePartsByCapabilities: relatedInlinesPartsByCapabilities
    } = related;
    const relatedInlines = getPartsFromCapabilities(
        { inlinePartsByCapabilities: relatedInlinesPartsByCapabilities },
        COMPOSER_CAPABILITIES
    );

    const identity = getIdentityForNewMessage();
    await setFrom(identity, message);
    mergeRecipients(message, relatedMessage);
    mergeSubject(message, relatedMessage);
    await mergeBody(message, relatedMessage, relatedInlinesPartsByCapabilities);

    store.commit(`mail/${SET_MESSAGE_STRUCTURE}`, {
        messageKey: message.key,
        structure: buildEditAsNewStructure(relatedInlines, relatedAttachments)
    });
    mergeHeaders(message, relatedMessage);
    router.navigate({ name: "v:mail:message", params: { message: message } });
    return message;
}
