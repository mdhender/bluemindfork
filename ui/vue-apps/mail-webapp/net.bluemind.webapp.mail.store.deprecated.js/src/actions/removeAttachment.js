import injector from "@bluemind/inject";

export function removeAttachment({ commit, dispatch, getters, state }, attachmentUid) {
    const status = getters["draft/getAttachmentStatus"](attachmentUid);
    const partAddress = state.draft.parts.attachments.find(attachment => attachment.uid === attachmentUid).address;

    let promise = Promise.resolve();

    if (status !== "ERROR") {
        const draftbox = getters.my.DRAFTS;
        promise = injector
            .getProvider("MailboxItemsPersistence")
            .get(draftbox.uid)
            .removePart(partAddress);
    }
    return promise.then(() => {
        commit("draft/removeAttachment", attachmentUid);
        dispatch("saveDraft");
    });
}
