import DraftStatus from "../mailbackend/MailboxItemsStore/DraftStatus";
import injector from "@bluemind/inject";
import mutationTypes from "../../store/mutationTypes";

export async function deleteDraft({ commit, rootGetters }, message) {
    const draft = { ...message };
    if (!draft.remoteRef.internalId || draft.status === DraftStatus.DELETED) {
        return Promise.resolve();
    }

    const draftbox = rootGetters["mail/MY_DRAFTS"];
    const service = injector.getProvider("MailboxItemsPersistence").get(draftbox.uid);
    commit("mail/" + mutationTypes.SET_DRAFT_STATUS, DraftStatus.DELETING, { root: true });

    try {
        // FIXME ? maybe duplicated code with PURGE_MESSAGE action
        await service.deleteById(draft.remoteRef.internalId);
        commit("mail/" + mutationTypes.REMOVE_MESSAGES, [message.key], { root: true });
        commit("mail/" + mutationTypes.SET_DRAFT_STATUS, DraftStatus.DELETED, { root: true });
    } catch (reason) {
        commit(
            "addApplicationAlert",
            {
                code: "MSG_DRAFT_DELETE_ERROR",
                props: { subject: draft.subject, reason }
            },
            { root: true }
        );
        commit("mail/" + mutationTypes.SET_DRAFT_STATUS, DraftStatus.DELETE_ERROR, { root: true });
    }
}
