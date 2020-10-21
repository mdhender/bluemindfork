import Message from "../Message";
import MessageAdaptor from "../../../../store/messages/helpers/MessageAdaptor";

// FIXME after store migration this code should be a computed in MessageList component
export function messages(state, getters, rootState, rootGetters) {
    return rootState.mail.messageList.messageKeys
        .filter(key => rootGetters["mail/isLoaded"](key))
        .map(key => new Message(key, MessageAdaptor.toMailboxItem(rootState.mail.messages[key])));
}
