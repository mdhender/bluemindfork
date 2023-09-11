import store from "@bluemind/store";
import getContentWithLinks from "../helpers/getContentWithLinks";
export default async function ({ message }) {
    const newContent = getContentWithLinks(this, message);
    store.dispatch("mail/SET_DRAFT_CONTENT", { html: newContent, draft: message });
}
