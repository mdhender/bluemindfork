export function setParts(state, { attachments, inlines }) {
    Object.assign(state.parts, { attachments, inlines });
}
