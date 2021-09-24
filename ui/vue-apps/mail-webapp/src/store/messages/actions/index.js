import { withAlert } from "../../helpers/withAlert";

import {
    addFlag,
    deleteFlag,
    fetchMessageIfNotLoaded,
    fetchMessageMetadata,
    moveMessages,
    removeMessages
} from "./actions";
import addAttachments from "./addAttachments";
import removeAttachment from "./removeAttachment";
import { debouncedSave, saveAsap } from "./save";
import send from "./send";
import {
    ADD_ATTACHMENTS,
    ADD_FLAG,
    DEBOUNCED_SAVE_MESSAGE,
    DELETE_FLAG,
    FETCH_MESSAGE_IF_NOT_LOADED,
    FETCH_MESSAGE_METADATA,
    MARK_MESSAGE_AS_FLAGGED,
    MARK_MESSAGE_AS_READ,
    MARK_MESSAGE_AS_UNFLAGGED,
    MARK_MESSAGE_AS_UNREAD,
    MARK_MESSAGES_AS_FLAGGED,
    MARK_MESSAGES_AS_READ,
    MARK_MESSAGES_AS_UNFLAGGED,
    MARK_MESSAGES_AS_UNREAD,
    MOVE_MESSAGES,
    REMOVE_ATTACHMENT,
    REMOVE_MESSAGES,
    SAVE_MESSAGE,
    SAVE_AS_DRAFT,
    SAVE_AS_TEMPLATE,
    SEND_MESSAGE
} from "~/actions";

import { Flag } from "@bluemind/email";

const markAsUnread = ({ dispatch }, messages) => dispatch(DELETE_FLAG, { messages, flag: Flag.SEEN });
const markAsRead = ({ dispatch }, messages) => dispatch(ADD_FLAG, { messages, flag: Flag.SEEN });
const markAsFlagged = ({ dispatch }, messages) => dispatch(ADD_FLAG, { messages, flag: Flag.FLAGGED });
const markAsUnflagged = ({ dispatch }, messages) => dispatch(DELETE_FLAG, { messages, flag: Flag.FLAGGED });
const saveAs = (context, { message, messageCompose }) => saveAsap(context, { draft: message, messageCompose });
export default {
    [ADD_ATTACHMENTS]: addAttachments,
    [ADD_FLAG]: addFlag,
    [DEBOUNCED_SAVE_MESSAGE]: debouncedSave,
    [DELETE_FLAG]: deleteFlag,
    [FETCH_MESSAGE_IF_NOT_LOADED]: fetchMessageIfNotLoaded,
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
    [REMOVE_ATTACHMENT]: removeAttachment,
    [REMOVE_MESSAGES]: withAlert(removeMessages, REMOVE_MESSAGES, "RemoveMessages"),
    [SAVE_MESSAGE]: saveAsap,
    [SAVE_AS_TEMPLATE]: withAlert(saveAs, SAVE_AS_TEMPLATE, "SaveMessageAs"),
    [SAVE_AS_DRAFT]: withAlert(saveAs, SAVE_AS_DRAFT, "SaveMessageAs"),
    [SEND_MESSAGE]: withAlert(send, SEND_MESSAGE, "SendMessage")
};
