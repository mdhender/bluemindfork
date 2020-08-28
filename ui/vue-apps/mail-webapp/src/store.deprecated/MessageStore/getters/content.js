import { MimeType, PartsHelper } from "@bluemind/email";

export function content(state, getters, rootState, rootGetters) {
    const partsContent = rootState["mail-webapp"]["messages"].itemsParts[state.key];
    if (state.key && partsContent) {
        const parts = state.parts.inlines
            .map(part => {
                const content = rootGetters["mail-webapp/messages/getPartContent"](state.key, part.address);
                if (content) {
                    return Object.assign({}, part, { content });
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
