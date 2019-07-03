import Message from "./Message.js";

export function messages(state) {
    return (state.items || []).map(item => {
        return new Message(item);
    });
}

export function currentMessage(state, getters) {
    if (state.current) {
        let message = getters.messageByUid(state.current);
        message.flags.push("hasAttachment");
        return message;
    }
}

export function currentParts(state) {
    if (state.current) {
        return state.partsToDisplay;
    }
}

export function attachments(state) {
    if (state.current) {
        return state.attachments;
    }
}

export function messageByUid(state) {
    return uid => {
        const mailboxItem = state.items.find(item => item.uid == uid);
        if (mailboxItem) {
            return new Message(mailboxItem);
        }
    };
}

export function countUnreadMessages(state, getters) {
    return getters.messages.filter(message => message.states.includes("not-seen")).length;
}
