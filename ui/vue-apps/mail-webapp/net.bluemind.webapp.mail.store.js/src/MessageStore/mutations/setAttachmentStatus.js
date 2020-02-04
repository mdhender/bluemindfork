import Vue from "vue";

export function setAttachmentStatus(state, {attachmentUid, status}) {
    Vue.set(state.attachmentStatuses, attachmentUid, status);
}
