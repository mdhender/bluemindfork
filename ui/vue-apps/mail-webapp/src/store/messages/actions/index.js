import { withAlert } from "../../helpers/withAlert";

import { addFlag, deleteFlag, fetchMessageMetadata, removeMessages } from "./actions";
import actionTypes from "../../actionTypes";
import addAttachments from "./addAttachments";
import create from "./create";
import removeAttachment from "./removeAttachment";
import save from "./save";
import send from "./send";
import { ADD_FLAG, MARK_MESSAGE_AS_READ } from "../../types/actions";
import { Flag } from "@bluemind/email";

export default {
    [actionTypes.ADD_ATTACHMENTS]: addAttachments,
    [actionTypes.ADD_FLAG]: addFlag,
    [actionTypes.CREATE_MESSAGE]: create,
    [actionTypes.DELETE_FLAG]: deleteFlag,
    [actionTypes.FETCH_MESSAGE_METADATA]: fetchMessageMetadata,
    [actionTypes.REMOVE_ATTACHMENT]: removeAttachment,
    [actionTypes.REMOVE_MESSAGES]: removeMessages,
    [actionTypes.SAVE_MESSAGE]: save,
    [actionTypes.SEND_MESSAGE]: withAlert(send, actionTypes.SEND_MESSAGE, "SendMessage"),
    [MARK_MESSAGE_AS_READ]: (context, messageKeys) => {
        if (messageKeys.length === 1) {
            return withAlert(addFlag, MARK_MESSAGE_AS_READ)(context, { messageKeys, flag: Flag.SEEN });
        } else {
            return context.dispatch(ADD_FLAG, { messageKeys, flag: Flag.SEEN });
        }
    }
};
