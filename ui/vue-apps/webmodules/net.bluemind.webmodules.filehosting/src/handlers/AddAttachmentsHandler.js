import { inject } from "@bluemind/inject";
import { renderMustDetachConfirmBox, renderShouldDetachConfirmBox, renderFileHostingModal } from "./renderers";
import getContentWithLinks from "./getContentWithLinks";
import { StopExecutionError } from "./errors";

let autoDetachmentLimit, maxFilesize;

export default async function ({ files, message, maxSize }) {
    files = [...files];
    const service = inject("AttachmentPersistence");
    if (!autoDetachmentLimit || !maxFilesize) {
        ({ autoDetachmentLimit, maxFilesize } = await service.getConfiguration());
    }

    const messageSize = getFilesSize(message.attachments);
    const newAttachmentsSize = getFilesSize(files);
    if (maxFilesize && files.some(file => file.size > maxFilesize)) {
        return { files, message, maxSize: maxFilesize };
    } else if (messageSize + newAttachmentsSize > maxSize) {
        return mustDetachFiles.call(this, files, message, maxSize);
    } else if (autoDetachmentLimit && newAttachmentsSize > autoDetachmentLimit) {
        return shouldDetachFiles.call(this, files, message, maxSize);
    }
}

async function mustDetachFiles(files, message, maxMessageSize) {
    const { content, props } = renderMustDetachConfirmBox(this, files, maxMessageSize, message);
    const res = await this.$bvModal.msgBoxConfirm([content], props);
    if (res) {
        return doDetach.call(this, files, message);
    }
    throw new StopExecutionError();
}

async function shouldDetachFiles(files, message, maxMessageSize) {
    const { content, props } = renderShouldDetachConfirmBox(this, files, maxMessageSize);
    const res = await this.$bvModal.msgBoxConfirm([content], props);
    if (res) {
        return doDetach.call(this, files, message);
    }
}

async function doDetach(files, message) {
    try {
        const { content, props } = renderFileHostingModal(this, message);
        this.$bvModal.open(content, props);
        await Promise.all(files.map(file => this.$store.dispatch(`mail/ADD_FH_ATTACHMENT`, { file, message })));
        const newContent = getContentWithLinks(this, message);
        this.$store.commit("mail/SET_DRAFT_EDITOR_CONTENT", newContent);
    } catch (e) {
        // eslint-disable-next-line no-console
        console.warn(e);
    }
    return { files: [], message };
}

function getFilesSize(files) {
    return files.reduce((totalSize, next) => totalSize + next.size, 0);
}
