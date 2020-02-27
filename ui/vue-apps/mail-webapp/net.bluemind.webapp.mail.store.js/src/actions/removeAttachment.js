import injector from "@bluemind/inject";

export function removeAttachment({ commit, dispatch, getters, state }, attachmentUid) {
    const status = getters["draft/getAttachmentStatus"](attachmentUid);
    console.log(status);
    console.log(getters["draft/getAttachmentStatus"]);
    const partAddress = state.draft.parts.attachments.find(attachment => attachment.uid === attachmentUid).address;

    let promise = Promise.resolve();

    if (status !== "ERROR") {
        console.log("coucou");
        console.log(status);
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
