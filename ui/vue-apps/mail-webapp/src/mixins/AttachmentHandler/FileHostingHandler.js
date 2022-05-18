import { inject } from "@bluemind/inject";
import UUIDGenerator from "@bluemind/uuid";
import {
    SET_ATTACHMENT_HEADERS,
    SET_ATTACHMENT_PROGRESS,
    SET_ATTACHMENT_STATUS,
    SET_MESSAGE_HAS_ATTACHMENT,
    SET_ATTACHMENT_ADDRESS,
    ADD_ATTACHMENT
} from "~/mutations";
import { create, AttachmentStatus } from "~/model/attachment";
import { createFromFile as createPartFromFile } from "~/model/part";
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
            const promises = Promise.all(files.map(file => this.addFhAttachment(file, message)));
            this.vm.$bvModal.show("fh-modal");
            return promises;
        }
    }

    async shouldDetachFiles(files, message, maxMessageSize) {
        const { content, props } = renderShouldDetachConfirmBox(this.vm, files, maxMessageSize);
        const res = await this.vm.$bvModal.msgBoxConfirm([content], props);
        if (res) {
            this.vm.$bvModal.show("fh-modal");
            return Promise.all(files.map(file => this.addFhAttachment(file, message)));
        } else {
            return this.next?.addAttachments(files, message);
        }
    }

    async cannotHandleFiles(files, maxFilesize) {
        const { content, props } = renderTooLargeOKBox(this.vm, files, maxFilesize);
        await this.vm.$bvModal.msgBoxOk([content], props);
        return null;
    }

    async addFhAttachment(file, message) {
        const attachment = this.createFhAttachment(file, message);

        this.vm.$store.commit(`mail/${ADD_ATTACHMENT}`, { messageKey: message.key, attachment });
        this.vm.$store.commit(`mail/${SET_ATTACHMENT_STATUS}`, {
            messageKey: message.key,
            address: attachment.address,
            status: AttachmentStatus.NOT_UPLOADED
        });
        this.vm.$store.commit(`mail/${SET_MESSAGE_HAS_ATTACHMENT}`, { key: message.key, hasAttachment: true });
        global.cancellers = global.cancellers || {};
        global.cancellers[attachment.address + message.key] = { cancel: undefined };

        const service = inject("MailboxItemsPersistence", message.folderRef.uid);
        const address = await service.uploadPart("", global.cancellers[attachment.address + message.key]);

        await this.vm.$store.commit(`mail/${SET_ATTACHMENT_ADDRESS}`, {
            messageKey: message.key,
            oldAddress: attachment.address,
            address
        });
        await this.share(file, message, address);

        this.vm.$store.commit(`mail/${SET_ATTACHMENT_STATUS}`, {
            messageKey: message.key,
            address,
            status: AttachmentStatus.UPLOADED
        });
    }

    async share(file, message, address) {
        const service = inject("AttachmentPersistence");

        const { publicUrl, name } = await service.share(
            file.name,
            file,
            null,
            createOnUploadProgress(this.vm.$store.commit, message.key, address)
        );
        // TODO handle expiration date

        this.vm.$store.commit(`mail/${SET_ATTACHMENT_HEADERS}`, {
            messageKey: message.key,
            address,
            headers: [
                {
                    name: "X-Mozilla-Cloud-Part",
                    values: [`cloudFile;url=${publicUrl};name=${name}`]
                },
                {
                    name: "X-BM-Disposition",
                    values: [`filehosting;url=${publicUrl};name=${name};size=${file.size};mime=${file.type}`]
                }
            ]
        });
    }

    createFhAttachment(file) {
        const attachmentFromFile = createPartFromFile(UUIDGenerator.generate(), {
            name: file.name,
            size: 0
        });
        const attachment = create(
            {
                ...attachmentFromFile,
                headers: [
                    {
                        name: "X-BM-Disposition",
                        values: [`filehosting;name=${file.name};size=${file.size};mime=${file.type}`]
                    }
                ]
            },
            AttachmentStatus.NOT_LOADED
        );
        return attachment;
    }
}

function createOnUploadProgress(commit, messageKey, address) {
    return progress => {
        commit(`mail/${SET_ATTACHMENT_PROGRESS}`, {
            messageKey,
            address,
            loaded: progress.loaded,
            total: progress.total
        });
    };
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
