import injector from "@bluemind/inject";

export function removeAttachment({ commit, dispatch, getters, state, rootGetters }, attachmentUid) {
    const status = getters["draft/getAttachmentStatus"](attachmentUid);
    const partAddress = state.draft.parts.attachments.find(attachment => attachment.uid === attachmentUid).address;

    let promise = Promise.resolve();

    if (status !== "ERROR") {
        promise = injector
            .getProvider("MailboxItemsPersistence")
            .get(rootGetters["mail/MY_DRAFTS"].remoteRef.uid)
            .removePart(partAddress);
    }
    return promise.then(() => {
        commit("draft/removeAttachment", attachmentUid);
        dispatch("saveDraft");
    });
}
