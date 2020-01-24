import { DraftStatus } from "@bluemind/backend.mail.store";
import { updateDraft } from "./updateDraft";

/** Update or reset the draft in the store. */
export function setDraft(state, { draft, isNew }) {
    const draftCopy = Object.assign({}, draft);
    if (isNew) {
        draftCopy.status = DraftStatus.NEW;
        draftCopy.id = null;
        draftCopy.saveDate = null;
        draftCopy.attachments = [];
    }
    updateDraft(state, draftCopy);
}
