import { inject } from "@bluemind/inject";
import UUIDGenerator from "@bluemind/uuid";
import { ADD_ATTACHMENT } from "~/actions";
import { SET_ATTACHMENT_HEADERS, SET_ATTACHMENT_PROGRESS, SET_ATTACHMENT_STATUS } from "~/mutations";
import { create, AttachmentStatus } from "~/model/attachment";
import { createFromFile as createPartFromFile } from "~/model/part";
import ChainOfResponsability from "./ChainOfResponsability";

export default class extends ChainOfResponsability {
    constructor(vm) {
        super(vm, 10);
    }

    async addAttachments(files, message) {
        const service = inject("AttachmentClientPersistence");
        const config = await service.getConfiguration();
        const promises = [];

        const unhandled = files.filter(file => {
            if (file.size >= config.autoDetachmentLimit) {
                promises.push(this.share(file, message));
            } else {
                return true;
            }
        });
        promises.push(this.next?.addAttachments(unhandled, message));
        return Promise.all(promises);
    }

    async share(file, message) {
        const service = inject("AttachmentPersistence");
        const attachmentFromFile = createPartFromFile(UUIDGenerator.generate(), {
            name: file.name,
            type: "application/octet-stream",
            size: 0
        });
        const attachment = create(
            {
                ...attachmentFromFile,
                type: "filehosting",
                extra: {
                    size: file.size,
                    mime: file.type,
                    dispositionType: "CLOUD"
                }
            },
            AttachmentStatus.NOT_LOADED
        );

        await this.vm.$store.dispatch(`mail/${ADD_ATTACHMENT}`, {
            message,
            attachment,
            content: ""
        });

        this.vm.$store.commit(`mail/${SET_ATTACHMENT_STATUS}`, {
            messageKey: message.key,
            address: attachment.address,
            status: AttachmentStatus.NOT_UPLOADED
        });

        const { publicUrl, name } = await service.share(
            file.name,
            file,
            null,
            createOnUploadProgress(this.vm.$store.commit, message.key, attachment)
        );
        // TODO handle expiration date

        this.vm.$store.commit(`mail/${SET_ATTACHMENT_HEADERS}`, {
            messageKey: message.key,
            address: attachment.address,
            headers: [
                {
                    name: "X-Mozilla-Cloud-Part",
                    values: [`cloudFile;url=${publicUrl};name=${name}`]
                },
                {
                    name: "X-BlueMind-Disposition",
                    values: [`filehosting;url=${publicUrl};name=${name};`]
                }
            ]
        });

        this.vm.$store.commit(`mail/${SET_ATTACHMENT_STATUS}`, {
            messageKey: message.key,
            address: attachment.address,
            status: AttachmentStatus.UPLOADED
        });
    }
}
function createOnUploadProgress(commit, messageKey, attachment) {
    return progress => {
        commit(`mail/${SET_ATTACHMENT_PROGRESS}`, {
            messageKey,
            address: attachment.address,
            loaded: progress.loaded,
            total: progress.total
        });
    };
}
