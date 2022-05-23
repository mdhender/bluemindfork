import { inject } from "@bluemind/inject";
import FhConfirmBox from "~/components/ConfirmBox";
import FhMustDetachConfirmBox from "~/components/MustDetachConfirmBox";

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
    this.$bvModal.show("fh-modal");
    await Promise.all(files.map(file => this.$store.dispatch(`mail/ADD_FH_ATTACHMENT`, { file, message })));
    return { files: [], message };
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

class StopExecutionError extends Error {
    name = "StopExecution";
}
