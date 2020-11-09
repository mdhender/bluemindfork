import { withAlert } from "../../helpers/withAlert";

import { addFlag, deleteFlag, fetchMessageMetadata, removeMessages } from "./actions";
import addAttachments from "./addAttachments";
import create from "./create";
import removeAttachment from "./removeAttachment";
import save from "./save";
import send from "./send";
import {
    ADD_ATTACHMENTS,
    ADD_FLAG,
    CREATE_MESSAGE,
    DELETE_FLAG,
    FETCH_MESSAGE_METADATA,
    MARK_MESSAGES_AS_READ,
    MARK_MESSAGE_AS_READ,
    REMOVE_ATTACHMENT,
    REMOVE_MESSAGES,
    SAVE_MESSAGE,
    SEND_MESSAGE
} from "~actions";

import { Flag } from "@bluemind/email";

export default {
    [ADD_ATTACHMENTS]: addAttachments,
    [ADD_FLAG]: addFlag,
    [CREATE_MESSAGE]: create,
    [DELETE_FLAG]: deleteFlag,
    [FETCH_MESSAGE_METADATA]: fetchMessageMetadata,
    [REMOVE_ATTACHMENT]: removeAttachment,
    [REMOVE_MESSAGES]: removeMessages,
    [SAVE_MESSAGE]: save,
    [SEND_MESSAGE]: withAlert(send, SEND_MESSAGE, "SendMessage"),
    [MARK_MESSAGE_AS_READ]: ({ dispatch }, key) => dispatch(ADD_FLAG, { messageKeys: [key], flag: Flag.SEEN }),
    [MARK_MESSAGES_AS_READ]: withAlert(
        ({ dispatch }, messageKeys) => dispatch(ADD_FLAG, { messageKeys, flag: Flag.SEEN }),
        MARK_MESSAGES_AS_READ
    )
};
