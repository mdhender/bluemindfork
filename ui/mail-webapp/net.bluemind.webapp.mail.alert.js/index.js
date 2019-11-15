import { AlertTypes } from "@bluemind/alert.store";

export default {
    "MSG_MOVED_LOADING": {
        type: AlertTypes.LOADING,
        key: "alert.mail.move.loading"
    },
    "MSG_MOVE_OK": {
        type: AlertTypes.SUCCESS,
        key: "alert.mail.move.ok"
    },
    "MSG_MOVE_ERROR": {
        type: AlertTypes.ERROR,
        key: "alert.mail.move.error"
    },
    "MSG_REMOVED_LOADING": {
        type: AlertTypes.LOADING,
        key : "alert.common.remove.loading"
    },
    "MSG_REMOVED_OK": {
        type: AlertTypes.SUCCESS,
        key: "alert.common.remove.ok"
    },
    "MSG_REMOVED_ERROR": {
        type: AlertTypes.ERROR,
        key: "alert.common.remove.error"
    },
    "MSG_SEND_LOADING": {
        type: AlertTypes.LOADING,
        key: "alert.mail.send.loading"
    },
    "MSG_SENT_OK": {
        type: AlertTypes.SUCCESS,
        key: "alert.mail.message.sent.ok"
    },
    "MSG_SENT_ERROR": {
        type: AlertTypes.ERROR,
        key: "alert.mail.message.sent.error"
    },
    "MSG_PURGE_LOADING": {
        type: AlertTypes.LOADING,
        key: "alert.common.purge.loading"
    },
    "MSG_PURGE_OK": {
        type: AlertTypes.SUCCESS,
        key: "alert.common.purge.ok"
    },
    "MSG_PURGE_ERROR": {
        type: AlertTypes.ERROR,
        key: "alert.common.purge.error"
    },
    "MSG_DRAFT_DELETE_OK": {
        type: AlertTypes.SUCCESS,
        key: "alert.mail.message.draft.delete.ok"
    },
    "MSG_DRAFT_DELETE_ERROR": {
        type: AlertTypes.ERROR,
        key: "alert.mail.message.draft.delete.error"
    }
};