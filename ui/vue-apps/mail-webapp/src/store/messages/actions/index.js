import { withAlert } from "@bluemind/alert.store";

import { addFlag, deleteFlag, fetchMessageMetadata, removeMessages } from "./actions";
import actionTypes from "../../actionTypes";
import addAttachments from "./addAttachments";
import create from "./create";
import removeAttachment from "./removeAttachment";
import save from "./save";
import send from "./send";

export default {
    [actionTypes.ADD_ATTACHMENTS]: addAttachments,
    [actionTypes.ADD_FLAG]: addFlag,
    [actionTypes.CREATE_MESSAGE]: create,
    [actionTypes.DELETE_FLAG]: deleteFlag,
    [actionTypes.FETCH_MESSAGE_METADATA]: fetchMessageMetadata,
    [actionTypes.REMOVE_ATTACHMENT]: removeAttachment,
    [actionTypes.REMOVE_MESSAGES]: removeMessages,
    [actionTypes.SAVE_MESSAGE]: save,
    [actionTypes.SEND_MESSAGE]: withAlert(send, "MSG_SEND", true, propsProvider)
};

function propsProvider(store, payload) {
    const subject = store.state[payload.draftKey].subject;
    return {
        subject: subject
    };
}
