import { AlertTypes } from "@bluemind/alert.store";

export default {
    MSG_MOVED_LOADING_MULTIPLE: {
        type: AlertTypes.LOADING,
        key: "alert.mail.move.multiple.loading",
        renderer: "MailAlertRenderer"
    },
    MSG_MOVE_OK: {
        type: AlertTypes.SUCCESS,
        key: "alert.mail.move.ok",
        renderer: "MailAlertRenderer"
    },
    MSG_MOVE_OK_MULTIPLE: {
        type: AlertTypes.SUCCESS,
        key: "alert.mail.move.multiple.ok",
        renderer: "MailAlertRenderer"
    },
    MSG_MOVE_ERROR: {
        type: AlertTypes.ERROR,
        key: "alert.mail.move.error",
        renderer: "MailAlertRenderer"
    },
    MSG_MOVE_ERROR_MULTIPLE: {
        type: AlertTypes.ERROR,
        key: "alert.mail.move.multiple.error",
        renderer: "MailAlertRenderer"
    },
    MSG_REMOVED_OK: {
        type: AlertTypes.SUCCESS,
        key: "alert.mail.message.remove.ok",
        renderer: "MailAlertRenderer"
    },
    MSG_REMOVED_ERROR: {
        type: AlertTypes.ERROR,
        key: "alert.mail.message.remove.error",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_REMOVED_LOADING: {
        type: AlertTypes.LOADING,
        key: "alert.mail.message.multiple.remove.loading",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_REMOVED_OK: {
        type: AlertTypes.SUCCESS,
        key: "alert.mail.message.multiple.remove.ok",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_REMOVED_ERROR: {
        type: AlertTypes.ERROR,
        key: "alert.mail.message.multiple.remove.error",
        renderer: "MailAlertRenderer"
    },
    MSG_SEND_LOADING: {
        type: AlertTypes.LOADING,
        key: "alert.mail.send.loading",
        renderer: "MailAlertRenderer"
    },
    MSG_SENT_OK: {
        type: AlertTypes.SUCCESS,
        key: "alert.mail.message.sent.ok",
        renderer: "MailAlertRenderer"
    },
    MSG_SENT_ERROR: {
        type: AlertTypes.ERROR,
        key: "alert.mail.message.sent.error",
        renderer: "MailAlertRenderer"
    },
    MSG_PURGE_OK: {
        type: AlertTypes.SUCCESS,
        key: "alert.mail.message.purge.ok",
        renderer: "MailAlertRenderer"
    },
    MSG_PURGE_ERROR: {
        type: AlertTypes.ERROR,
        key: "alert.mail.message.purge.error",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_PURGE_LOADING: {
        type: AlertTypes.LOADING,
        key: "alert.mail.message.multiple.purge.loading",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_PURGE_OK: {
        type: AlertTypes.SUCCESS,
        key: "alert.mail.message.multiple.purge.ok",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_PURGE_ERROR: {
        type: AlertTypes.ERROR,
        key: "alert.mail.message.multiple.purge.error",
        renderer: "MailAlertRenderer"
    },
    MSG_DRAFT_DELETE_OK: {
        type: AlertTypes.SUCCESS,
        key: "alert.mail.message.draft.delete.ok",
        renderer: "MailAlertRenderer"
    },
    MSG_DRAFT_DELETE_ERROR: {
        type: AlertTypes.ERROR,
        key: "alert.mail.message.draft.delete.error",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_MARK_AS_READ_LOADING: {
        type: AlertTypes.LOADING,
        key: "alert.mail.multiple.markasread.loading",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_MARK_AS_READ_ERROR: {
        type: AlertTypes.ERROR,
        key: "alert.mail.multiple.markasread.error",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_MARK_AS_READ_SUCCESS: {
        type: AlertTypes.SUCCESS,
        key: "alert.mail.multiple.markasread.success",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_MARK_AS_UNREAD_LOADING: {
        type: AlertTypes.LOADING,
        key: "alert.mail.multiple.markasunread.loading",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_MARK_AS_UNREAD_ERROR: {
        type: AlertTypes.ERROR,
        key: "alert.mail.multiple.markasunread.error",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_MARK_AS_UNREAD_SUCCESS: {
        type: AlertTypes.SUCCESS,
        key: "alert.mail.multiple.markasunread.success",
        renderer: "MailAlertRenderer"
    },
    MSG_FOLDER_REMOVE_ERROR: {
        type: AlertTypes.ERROR,
        key: "alert.folder.remove.error",
        renderer: "MailAlertRenderer"
    },
    MSG_FOLDER_REMOVE_SUCCESS: {
        type: AlertTypes.SUCCESS,
        key: "alert.folder.remove.success",
        renderer: "MailAlertRenderer"
    },
    MSG_FOLDER_RENAME_ERROR: {
        type: AlertTypes.ERROR,
        key: "alert.folder.rename.error",
        renderer: "MailAlertRenderer"
    },
    MSG_FOLDER_RENAME_SUCCESS: {
        type: AlertTypes.SUCCESS,
        key: "alert.folder.rename.success",
        renderer: "MailAlertRenderer"
    },
    MSG_FOLDER_MARKASREAD_ERROR: {
        type: AlertTypes.ERROR,
        key: "alert.folder.mark_as_read.error",
        renderer: "MailAlertRenderer"
    },
    MSG_FOLDER_MARKASREAD_SUCCESS: {
        type: AlertTypes.SUCCESS,
        key: "alert.folder.mark_as_read.success",
        renderer: "MailAlertRenderer"
    },
    MSG_FOLDER_CREATE_ERROR: {
        type: AlertTypes.ERROR,
        key: "alert.folder.create.error",
        renderer: "MailAlertRenderer"
    },
    MSG_FOLDER_CREATE_SUCCESS: {
        type: AlertTypes.SUCCESS,
        key: "alert.folder.create.success",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_MARK_AS_FLAGGED_LOADING: {
        type: AlertTypes.LOADING,
        key: "alert.mail.multiple.markasflagged.loading",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_MARK_AS_FLAGGED_ERROR: {
        type: AlertTypes.ERROR,
        key: "alert.mail.multiple.markasflagged.error",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_MARK_AS_FLAGGED_SUCCESS: {
        type: AlertTypes.SUCCESS,
        key: "alert.mail.multiple.markasflagged.success",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_MARK_AS_UNFLAGGED_LOADING: {
        type: AlertTypes.LOADING,
        key: "alert.mail.multiple.markasunflagged.loading",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_MARK_AS_UNFLAGGED_ERROR: {
        type: AlertTypes.ERROR,
        key: "alert.mail.multiple.markasunflagged.error",
        renderer: "MailAlertRenderer"
    },
    MSG_MULTIPLE_MARK_AS_UNFLAGGED_SUCCESS: {
        type: AlertTypes.SUCCESS,
        key: "alert.mail.multiple.markasunflagged.success",
        renderer: "MailAlertRenderer"
    }
};
