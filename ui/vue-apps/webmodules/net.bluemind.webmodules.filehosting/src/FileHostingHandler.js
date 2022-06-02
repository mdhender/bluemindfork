import Vue from "vue";
import { inject } from "@bluemind/inject";
import { CORPORATE_SIGNATURE_SELECTOR, PERSONAL_SIGNATURE_SELECTOR } from "./model/signature";
import { renderMustDetachConfirmBox, renderShouldDetachConfirmBox } from "./modals";
import ComposerLinks from "~/components/ComposerLinks";
var ComposerLinksClass = Vue.extend(ComposerLinks);

const LINKS_CLASSNAME = "filehosting-links";

export default async function ({ files, message, maxSize }) {
    const service = inject("AttachmentPersistence");
    const { autoDetachmentLimit, maxFilesize } = await service.getConfiguration();

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
    this.$bvModal.show("file-hosting-modal");
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
    const composerLinks = new ComposerLinksClass({
        propsData: {
            attachments: Object.values(this.$store.state.mail.filehosting.values[message.key]),
            className: LINKS_CLASSNAME + message.key,
            i18n: this.$i18n
        }
    });
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

class StopExecutionError extends Error {
    name = "StopExecution";
}
