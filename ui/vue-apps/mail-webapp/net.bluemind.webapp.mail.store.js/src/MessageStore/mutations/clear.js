export function clear(state) {
    state.key = undefined;
    state.id = undefined;
    state.parts.attachments.splice(0);
    state.parts.inlines.splice(0);
    state.attachmentStatuses = {};
    state.attachmentProgresses = {};
    state.saveDate = null;
}
