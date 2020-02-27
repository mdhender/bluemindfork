import Vue from "vue";

export function removeAttachmentProgress(state, attachmentUid) {
    Vue.delete(state.attachmentProgresses, attachmentUid);
}
