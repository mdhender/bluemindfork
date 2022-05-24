import { inject } from "@bluemind/inject";
import ChainOfResponsability from "./ChainOfResponsability";
import FhConfirmBox from "../../components/MailAttachment/Modals/FileHosting/FhConfirmBox";
import FhMustDetachConfirmBox from "../../components/MailAttachment/Modals/FileHosting/FhMustDetachConfirmBox";
import TooLargeBox from "../../components/MailAttachment/Modals/TooLargeBox";

export default class extends ChainOfResponsability {
    constructor(vm) {
        super(vm, 10);
    }

    async addAttachments(files, message) {
        const service = inject("AttachmentPersistence");
        const { autoDetachmentLimit, maxFilesize } = await service.getConfiguration();
        const maxMessageSize = this.vm.$store.state.mail.messageCompose.maxMessageSize;

        const messageSize = getFilesSize(message.attachments);
        const newAttachmentsSize = getFilesSize(files);
        if (maxFilesize && files.some(file => file.size > maxFilesize)) {
            return this.cannotHandleFiles(files, maxFilesize);
        } else if (messageSize + newAttachmentsSize > maxMessageSize) {
            return this.mustDetachFiles(files, message, maxMessageSize);
        } else if (autoDetachmentLimit && newAttachmentsSize > autoDetachmentLimit) {
            return this.shouldDetachFiles(files, message, maxMessageSize);
        }
        return this.next?.addAttachments(files, message);
    }

    async mustDetachFiles(files, message, maxMessageSize) {
        const { content, props } = renderMustDetachConfirmBox(this.vm, files, maxMessageSize, message);
        const res = await this.vm.$bvModal.msgBoxConfirm([content], props);
        if (res) {
            const promises = Promise.all(
                files.map(file => this.vm.$store.dispatch(`mail/ADD_FH_ATTACHMENT`, { file, message }))
            );
            this.vm.$bvModal.show("fh-modal");
            return promises;
        }
    }

    async shouldDetachFiles(files, message, maxMessageSize) {
        const { content, props } = renderShouldDetachConfirmBox(this.vm, files, maxMessageSize);
        const res = await this.vm.$bvModal.msgBoxConfirm([content], props);
        if (res) {
            this.vm.$bvModal.show("fh-modal");
            return Promise.all(files.map(file => this.vm.$store.dispatch(`mail/ADD_FH_ATTACHMENT`, { file, message })));
        } else {
            return this.next?.addAttachments(files, message);
        }
    }

    async cannotHandleFiles(files, maxFilesize) {
        const { content, props } = renderTooLargeOKBox(this.vm, files, maxFilesize);
        await this.vm.$bvModal.msgBoxOk([content], props);
        return null;
    }
}

function getFilesSize(files) {
    return files.reduce((totalSize, next) => totalSize + next.size, 0);
}

function renderMustDetachConfirmBox(vm, files, sizeLimit, message) {
    const content = vm.$createElement(FhMustDetachConfirmBox, {
        props: {
            attachments: files.map(file => {
                return {
                    fileName: file.name,
                    progress: { total: file.size, loaded: 0 }
                };
            }),
            sizeLimit,
            allAttachmentsCount: message.attachments?.length + files.length
        }
    });
    const props = {
        title: vm.$tc("mail.filehosting.add.large", files.length),
        okTitle: vm.$tc("mail.filehosting.share.start", files.length),
        cancelTitle: vm.$t("common.cancel"),
        bodyClass: "pb-4",
        cancelVariant: "simple-dark"
    };

    return { content, props };
}
function renderShouldDetachConfirmBox(vm, files) {
    const content = vm.$createElement(FhConfirmBox, {
        props: {
            attachments: files.map(file => {
                return {
                    fileName: file.name,
                    progress: { total: file.size, loaded: 0 }
                };
            })
        },
        scopedSlots: {
            text: () =>
                vm.$createElement("span", [
                    vm.$tc("mail.filehosting.threshold.almost_hit", files.length),
                    vm.$createElement("br"),
                    vm.$tc("mail.filehosting.share.start", files.length),
                    " ?"
                ])
        }
    });
    const props = {
        title: vm.$tc("mail.filehosting.add.large", files.length),
        okTitle: vm.$tc("mail.filehosting.share.start", files.length),
        cancelTitle: vm.$t("mail.actions.attach"), //TODO: use a better wording
        bodyClass: "pb-4",
        cancelVariant: "simple-dark"
    };

    return { content, props };
}

function renderTooLargeOKBox(vm, files, sizeLimit) {
    const content = vm.$createElement(TooLargeBox, { props: { sizeLimit, attachmentsCount: files.length } });

    const props = {
        title: vm.$tc("mail.filehosting.add.too_large", files.length),
        okTitle: vm.$tc("common.got_it"),
        bodyClass: "pb-4",
        okVariant: "outline-primary"
    };

    return { content, props };
}
