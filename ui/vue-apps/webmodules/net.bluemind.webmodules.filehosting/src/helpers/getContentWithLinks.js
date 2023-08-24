import { LINKS_CLASSNAME, renderLinksWithFrameComponent } from "./renderers";
import { attachmentUtils, fileUtils, messageUtils, signatureUtils } from "@bluemind/mail";
import store from "@bluemind/store";
import { getFhHeader, getFhInfos } from "./index";

const { CORPORATE_SIGNATURE_SELECTOR, PERSONAL_SIGNATURE_SELECTOR } = signatureUtils;
const { FileStatus } = fileUtils;
const { MessageReplyAttributeSeparator, MessageForwardAttributeSeparator, computeParts } = messageUtils;
const { AttachmentAdaptor } = attachmentUtils;

export default function (vm, message) {
    const messageCompose = store.state.mail.messageCompose;
    const template = document.createElement("template");
    template.innerHTML = messageCompose.editorContent;
    const fragment = template.content;
    const previousLinks = getPreviousNode(fragment);
    if (previousLinks) {
        previousLinks.remove();
    }
    const files = getUploadedFiles(message);

    const composerLinks = renderLinksWithFrameComponent(vm, files);
    composerLinks.$mount();
    const signatureNode = getSignatureNode.call(this, fragment, messageCompose);
    if (signatureNode) {
        signatureNode.parentNode.insertBefore(composerLinks.$el, signatureNode);
    } else {
        fragment.appendChild(composerLinks.$el);
    }
    return template.innerHTML;
}

function getSignatureNode(fragment, messageCompose) {
    let signature = fragment.querySelector(CORPORATE_SIGNATURE_SELECTOR);
    if (!signature) {
        signature = fragment.querySelector(PERSONAL_SIGNATURE_SELECTOR(messageCompose.personalSignature.id));
    }
    return signature;
}

function getUploadedFiles(message) {
    const parts = computeParts(message.structure);
    const files = AttachmentAdaptor.extractFiles(parts.attachments, message);

    return files.flatMap(file => {
        const isFh = !!getFhHeader(file.headers);
        if (isFh && file.status === FileStatus.UPLOADED) {
            const uploadedFile = Object.values(store.state.mail.messageCompose.uploadingFiles).find(
                ({ address }) => file.address === address
            );
            return {
                ...file,
                ...getFhInfos(file.headers),
                ...uploadedFile
            };
        }
        return [];
    });
}

function isCorrectNode(fragment, node) {
    const reply = fragment.querySelector(`#${MessageReplyAttributeSeparator}`);
    const forward = fragment.querySelector(`#${MessageForwardAttributeSeparator}`);
    return !((reply && reply.contains(node)) || (forward && forward.contains(node)));
}

function getPreviousNode(fragment) {
    const previousNodes = fragment.querySelectorAll(`.${LINKS_CLASSNAME}`);
    for (const node of previousNodes) {
        if (isCorrectNode(fragment, node)) {
            return node;
        }
    }
}
