import { AlertTypes } from "@bluemind/alert.store";

export default {
    "MSG_MOVED_LOADING": {
        type: AlertTypes.LOADING,
        key: "mail.alert.move.loading"
    },
    "MSG_MOVE_OK": {
        type: AlertTypes.SUCCESS,
        key: "mail.alert.move.ok"
    },
    "MSG_MOVE_ERROR": {
        type: AlertTypes.ERROR,
        key: "mail.alert.move.error"
    },
    "MSG_REMOVED_LOADING": {
        type: AlertTypes.LOADING,
        key : "common.alert.remove.loading"
    },
    "MSG_REMOVED_OK": {
        type: AlertTypes.SUCCESS,
        key: "common.alert.remove.ok"
    },
    "MSG_REMOVED_ERROR": {
        type: AlertTypes.ERROR,
        key: "common.alert.remove.error"
    },
    "MSG_SEND_LOADING": {
        type: AlertTypes.LOADING,
        key: "mail.alert.send.loading"
    },
    "MSG_SENT_OK": {
        type: AlertTypes.SUCCESS,
        key: "mail.alert.message.sent.ok"
    },
    "MSG_SENT_ERROR": {
        type: AlertTypes.ERROR,
        key: "mail.alert.message.sent.error"
    },
    "MSG_PURGE_LOADING": {
        type: AlertTypes.LOADING,
        key: "common.alert.purge.loading"
    },
    "MSG_PURGE_OK": {
        type: AlertTypes.SUCCESS,
        key: "common.alert.purge.ok"
    },
    "MSG_PURGE_ERROR": {
        type: AlertTypes.ERROR,
        key: "common.alert.purge.error"
    },
    "MSG_DRAFT_DELETE_OK": {
        type: AlertTypes.SUCCESS,
        key: "mail.alert.message.draft.delete.ok"
    },
    "MSG_DRAFT_DELETE_ERROR": {
        type: AlertTypes.ERROR,
        key: "mail.alert.message.draft.delete.error"
    }
};