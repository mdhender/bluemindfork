import { withAlert } from "../../helpers/withAlert";

import { addFlag, deleteFlag, fetchMessageMetadata, moveMessages, removeMessages } from "./actions";
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
    MARK_MESSAGE_AS_FLAGGED,
    MARK_MESSAGE_AS_READ,
    MARK_MESSAGE_AS_UNFLAGGED,
    MARK_MESSAGE_AS_UNREAD,
    MARK_MESSAGES_AS_FLAGGED,
    MARK_MESSAGES_AS_READ,
    MARK_MESSAGES_AS_UNFLAGGED,
    MARK_MESSAGES_AS_UNREAD,
    REMOVE_ATTACHMENT,
    REMOVE_MESSAGES,
    SAVE_MESSAGE,
    SEND_MESSAGE
} from "~actions";

import { Flag } from "@bluemind/email";
import { MOVE_MESSAGES, MOVE_MESSAGES_TO_TRASH } from "../../types/actions";

const markAsUnread = ({ dispatch }, messages) => dispatch(DELETE_FLAG, { messages, flag: Flag.SEEN });
const markAsRead = ({ dispatch }, messages) => dispatch(ADD_FLAG, { messages, flag: Flag.SEEN });
const markAsFlagged = ({ dispatch }, messages) => dispatch(ADD_FLAG, { messages, flag: Flag.FLAGGED });
const markAsUnflagged = ({ dispatch }, messages) => dispatch(DELETE_FLAG, { messages, flag: Flag.FLAGGED });

export default {
    [ADD_ATTACHMENTS]: addAttachments,
    [ADD_FLAG]: addFlag,
    [CREATE_MESSAGE]: create,
    [DELETE_FLAG]: deleteFlag,
    [FETCH_MESSAGE_METADATA]: fetchMessageMetadata,
    [MARK_MESSAGE_AS_FLAGGED]: markAsFlagged,
    [MARK_MESSAGE_AS_READ]: markAsRead,
    [MARK_MESSAGE_AS_UNFLAGGED]: markAsUnflagged,
    [MARK_MESSAGE_AS_UNREAD]: markAsUnread,
    [MARK_MESSAGES_AS_FLAGGED]: withAlert(markAsFlagged, MARK_MESSAGES_AS_FLAGGED),
    [MARK_MESSAGES_AS_READ]: withAlert(markAsRead, MARK_MESSAGES_AS_READ),
    [MARK_MESSAGES_AS_UNFLAGGED]: withAlert(markAsUnflagged, MARK_MESSAGES_AS_UNFLAGGED),
    [MARK_MESSAGES_AS_UNREAD]: withAlert(markAsUnread, MARK_MESSAGES_AS_UNREAD),
    [MOVE_MESSAGES]: withAlert(moveMessages, MOVE_MESSAGES, "MoveMessages"),
    [MOVE_MESSAGES_TO_TRASH]: withAlert(moveMessages, MOVE_MESSAGES_TO_TRASH, "MoveMessages"),
    [REMOVE_ATTACHMENT]: removeAttachment,
    [REMOVE_MESSAGES]: withAlert(removeMessages, REMOVE_MESSAGES, "RemoveMessages"),
    [SAVE_MESSAGE]: save,
    [SEND_MESSAGE]: withAlert(send, SEND_MESSAGE, "SendMessage")
};
