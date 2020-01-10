import Message from "../Message";

export function messages(state) {
    return (state.itemKeys || []).map(key => {
        return state.items[key] && new Message(key, state.items[key]);
    });
}
