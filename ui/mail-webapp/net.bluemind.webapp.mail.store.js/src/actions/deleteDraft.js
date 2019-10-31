import { Alert, AlertTypes } from "@bluemind/alert.store";
import { DraftStatus } from "@bluemind/backend.mail.store";
import { TranslationHelper } from "@bluemind/i18n";
import injector from "@bluemind/inject";
import MailAppL10N from "@bluemind/webapp.mail.l10n";

/** Delete the draft (hard delete, not moved in Trash box). */
export function deleteDraft({ commit, state, getters }) {
    const draft = state.draft;

    if (!draft.id || draft.status == DraftStatus.DELETED) {
        // no saved draft to delete, just close the composer
        return Promise.resolve();
    }

    let service;
    let userSession;
    return new Promise(resolve => {
        // initialize service, session and status
        const draftbox = getters["folders/defaultFolders"].DRAFTS;

        service = injector.getProvider("MailboxItemsPersistence").get(draftbox.uid);
        userSession = injector.getProvider("UserSession").get();
        commit("updateDraft", { status: DraftStatus.DELETING });
        return resolve();
    })
        .then(() => {
            // request a delete on core side
            return service.deleteById(draft.id);
        })
        .then(() => {
            const key = "mail.alert.message.draft.delete.ok";
            const success = new Alert({
                type: AlertTypes.SUCCESS,
                code: "ALERT_CODE_MSG_DRAFT_DELETE_OK",
                key,
                message: TranslationHelper.getLocalizedProperty(
                    MailAppL10N, 
                    userSession,
                    key, 
                    { subject: draft.subject }),
                props: { subject: draft.subject }
            });
            commit("alert/addAlert", success, { root: true });
            commit("updateDraft", { status: DraftStatus.DELETED });
        })
        .catch(reason => {
            const key = "mail.alert.message.draft.delete.error";
            const error = new Alert({
                code: "ALERT_CODE_MSG_DRAFT_DELETE_ERROR",
                key,
                message: TranslationHelper.getLocalizedProperty(
                    MailAppL10N, 
                    userSession, 
                    key, 
                    { subject: draft.subject, reason: reason }),
                props: {
                    subject: draft.subject,
                    reason
                }
            });
            commit("alert/addAlert", error, { root: true });
            commit("updateDraft", { status: DraftStatus.DELETE_ERROR });
        });
}