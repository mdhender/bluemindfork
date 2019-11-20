import { MimeType } from "@bluemind/email";
import { PartsHelper } from "@bluemind/backend.mail.store";

export function currentMessageContent(state) {
    const partsContent = state.messages.parts[state.currentMessageId];
    if (state.currentMessageId && !!partsContent) {
        const parts = state.currentMessageParts.inlines
            .filter(({ address }) => !!partsContent[address])
            .map(part => Object.assign({}, part, { content: partsContent[part.address] }));
        const html = parts.filter(part => part.mime === "text/html");
        const images = parts.filter(part => MimeType.isImage(part) && part.contentId);
        const inlined = PartsHelper.insertInlineImages(html, images);
        const others = parts.filter(part => part.mime !== "text/html" && !inlined.includes(part.contentId));
        return [...html, ...others];
    }
    return [];
}
