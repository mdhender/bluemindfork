import { withAlert } from "@bluemind/alert.store";

import { addFlag, deleteFlag, fetchMessageMetadata, removeMessages } from "./actions";
import actionTypes from "../../actionTypes";
import save from "./save";
import send from "./send";

export default {
    [actionTypes.ADD_FLAG]: addFlag,
    [actionTypes.DELETE_FLAG]: deleteFlag,
    [actionTypes.FETCH_MESSAGE_METADATA]: fetchMessageMetadata,
    [actionTypes.REMOVE_MESSAGES]: removeMessages,
    [actionTypes.SAVE_MESSAGE]: save,
    [actionTypes.SEND_MESSAGE]: withAlert(send, "MSG_SEND", true, propsProvider)
};

function propsProvider(store, payload) {
    const mySentBox = store.getters["MY_SENT"];
    const subject = store.state[payload.draftKey].subject;
    return {
        loadingProps: { subject },
        successProps: {
            subject: subject,
            subjectLink: {
                name: "v:mail:message",
                params: { message: payload.draftKey, folder: mySentBox.path } // FIXME: we need the message key in sent folder, not draft one which doesnt exist anymore
            }
        },
        errorProps: { subject }
    };
}
