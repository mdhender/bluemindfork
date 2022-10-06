import {
    renderMustDetachConfirmBox,
    renderShouldDetachConfirmBox,
    renderFileHostingModal,
    renderTooLargeFilesModal
} from "../helpers/renderers";
import getContentWithLinks from "../helpers/getContentWithLinks";
import { StopExecutionError } from "./errors";
import { ADD_FH_ATTACHMENT, GET_CONFIGURATION } from "../store/types/actions";

let autoDetachmentLimit, maxFilesize;

export default async function ({ files, message, maxSize }, { forceFilehosting }) {
    files = [...files];
    if (!autoDetachmentLimit || !maxFilesize) {
        ({ autoDetachmentLimit, maxFilesize } = await this.$store.dispatch(`mail/${GET_CONFIGURATION}`));
    }

    const newAttachmentsSize = getFilesSize(files);
    if (maxFilesize && files.some(file => file.size > maxFilesize)) {
        renderTooLargeFilesModal(this, files, maxFilesize);
        throw new StopExecutionError();
    } else if (forceFilehosting) {
        return doDetach.call(this, files, message);
    } else if (message.size + newAttachmentsSize > maxSize) {
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
        await Promise.all(files.map(file => this.$store.dispatch(`mail/${ADD_FH_ATTACHMENT}`, { file, message })));
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
