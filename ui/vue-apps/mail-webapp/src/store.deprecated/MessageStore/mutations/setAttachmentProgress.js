import Vue from "vue";

export function setAttachmentProgress(state, { attachmentUid, loaded, total, canceller }) {
    Vue.set(state.attachmentProgresses, attachmentUid, { loaded, total, canceller });
}
