import { LINKS_CLASSNAME, renderLinksWithFrameComponent } from "./renderers";
import { fileUtils, messageUtils, signatureUtils } from "@bluemind/mail";
import { GET_FH_FILE } from "../store/types/getters";

const { CORPORATE_SIGNATURE_SELECTOR, PERSONAL_SIGNATURE_SELECTOR } = signatureUtils;
const { FileStatus } = fileUtils;
const { MessageReplyAttributeSeparator, MessageForwardAttributeSeparator } = messageUtils;

export default function (vm, message) {
    const messageCompose = vm.$store.state.mail.messageCompose;
    const template = document.createElement("template");
    template.innerHTML = messageCompose.editorContent;
    const fragment = template.content;
    const previousLinks = getPreviousNode(fragment);
    if (previousLinks) {
        previousLinks.remove();
    }
    const files = getUploadedFiles(vm, message);
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

function getUploadedFiles(vm, message) {
    return message.attachments.flatMap(attachment => {
        const fhFile = vm.$store.getters[`mail/${GET_FH_FILE}`]({ key: attachment.fileKey });
        const file = vm.$store.state.mail.files[attachment.fileKey];

        return fhFile && file.status === FileStatus.UPLOADED ? { ...file, ...fhFile } : [];
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
