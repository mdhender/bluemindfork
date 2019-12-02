import { DraftStatus } from "@bluemind/backend.mail.store";
import injector from "@bluemind/inject";

/** Delete the draft (hard delete, not moved in Trash box). */
export function deleteDraft({ commit, state, getters }) {
    const draft = state.draft;

    if (!draft.id || draft.status == DraftStatus.DELETED) {
        // no saved draft to delete, just close the composer
        return Promise.resolve();
    }

    let service;

    return new Promise(resolve => {
        // initialize service, session and status
        const draftbox = getters.my.DRAFTS;

        service = injector.getProvider("MailboxItemsPersistence").get(draftbox.uid);
        commit("updateDraft", { status: DraftStatus.DELETING });
        return resolve();
    })
        .then(() => {
            // request a delete on core side
            return service.deleteById(draft.id);
        })
        .then(() => {
            commit("alert/add", { code: "MSG_DRAFT_DELETE_OK", props: { subject: draft.subject } }, { root: true });
            commit("updateDraft", { status: DraftStatus.DELETED });
        })
        .catch(reason => {
            commit(
                "alert/add",
                {
                    code: "MSG_DRAFT_DELETE_ERROR",
                    props: { subject: draft.subject, reason }
                },
                { root: true }
            );
            commit("updateDraft", { status: DraftStatus.DELETE_ERROR });
        });
}
