import { MimeType } from "@bluemind/email";
import { PartsHelper } from "@bluemind/backend.mail.store";

export function currentMessageContent(state, getters) {
    const partsContent = state.messages.itemsParts[state.currentMessageKey];
    if (state.currentMessageKey && partsContent.length > 0) {
        const parts = state.currentMessageParts.inlines
            .map(part => {
                const content = getters["messages/getPartContent"](state.currentMessageKey, part.address);
                if (content) {
                    return Object.assign({}, part, { content: content });
                }
            })
            .filter(part => !!part);
        const html = parts.filter(part => part.mime === MimeType.TEXT_HTML);
        const images = parts.filter(part => MimeType.isImage(part) && part.contentId);
        const inlined = PartsHelper.insertInlineImages(html, images);
        const others = parts.filter(part => part.mime !== MimeType.TEXT_HTML && !inlined.includes(part.contentId));
        return [...html, ...others];
    }
    return [];
}
