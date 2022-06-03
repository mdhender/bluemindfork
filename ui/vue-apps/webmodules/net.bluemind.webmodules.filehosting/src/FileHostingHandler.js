import { inject } from "@bluemind/inject";
import { CORPORATE_SIGNATURE_SELECTOR, PERSONAL_SIGNATURE_SELECTOR } from "./model/signature";
import { AttachmentStatus } from "./model/attachment";
import {
    renderMustDetachConfirmBox,
    renderShouldDetachConfirmBox,
    renderFileHostingModal,
    renderLinksComponent
} from "./renderers";

let autoDetachmentLimit, maxFilesize;
const LINKS_CLASSNAME = "filehosting-links";

export default async function ({ files, message, maxSize }) {
    try {
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
    } catch {
        return { files: [], message, maxSize };
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
    const { content, props } = renderFileHostingModal(this, message);
    this.$bvModal.open(content, props);
    await Promise.all(files.map(file => this.$store.dispatch(`mail/ADD_FH_ATTACHMENT`, { file, message })));
    updateEditorContent.call(this, message);
    return { files: [], message };
}

function updateEditorContent(message) {
    const template = document.createElement("template");
    template.innerHTML = this.$store.state.mail.messageCompose.editorContent;
    const fragment = template.content;
    const previousLinks = fragment.querySelector(`.${LINKS_CLASSNAME}${message.key}`);
    if (previousLinks) {
        previousLinks.remove();
    }
    const attachments = getUploadedAttachments(this, message);
    if (attachments.length < 1) {
        return;
    }
    const composerLinks = renderLinksComponent(this, message, attachments, LINKS_CLASSNAME);
    composerLinks.$mount();
    const signatureNode = getSignatureNode.call(this, fragment);
    if (signatureNode) {
        fragment.insertBefore(composerLinks.$el, signatureNode);
    } else {
        fragment.appendChild(composerLinks.$el);
    }
    this.$store.state.mail.messageCompose.editorContent = template.innerHTML;
}

function getSignatureNode(fragment) {
    let signature = fragment.querySelector(CORPORATE_SIGNATURE_SELECTOR);
    if (!signature) {
        signature = fragment.querySelector(
            PERSONAL_SIGNATURE_SELECTOR(this.$store.state.mail.messageCompose.personalSignature.id)
        );
    }
    return signature;
}

function getFilesSize(files) {
    return files.reduce((totalSize, next) => totalSize + next.size, 0);
}

function getUploadedAttachments(vm, message) {
    return vm.$store.state.mail.conversations.messages[message.key].attachments.flatMap(
        attachment =>
            attachment.status === AttachmentStatus.UPLOADED &&
            vm.$store.getters["mail/GET_FH_ATTACHMENT"](message, attachment)
    );
}

class StopExecutionError extends Error {
    name = "StopExecution";
}
