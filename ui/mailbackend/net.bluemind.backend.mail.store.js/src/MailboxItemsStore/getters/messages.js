import Message from "../Message";

export function messages(state) {
    return (state.sortedIds || []).map(id => {
        return state.items[id] && new Message(state.items[id]);
    });
}
