import UUIDGenerator from "@bluemind/uuid";
import { ADD_ATTACHMENT } from "~/actions";
import { createFromFile as createPartFromFile } from "~/model/part";
import { create, AttachmentStatus } from "~/model/attachment";
import ChainOfResponsability from "./ChainOfResponsability";
import TooLargeBox from "../../components/MailAttachment/Modals/TooLargeBox";

export default class extends ChainOfResponsability {
    async addAttachments(files, message) {
        const filesSize = files.reduce((totalSize, file) => totalSize + file.size, 0);
        const maxFilesize = this.vm.$store.state.mail.messageCompose.maxMessageSize;
        if (filesSize > maxFilesize) {
            const { content, props } = renderTooLargeOKBox(this.vm, files, maxFilesize);
            await this.vm.$bvModal.msgBoxOk([content], props);
        }

        const promises = files.map(file => {
            const part = createPartFromFile(UUIDGenerator.generate(), file);
            const attachment = create(part, AttachmentStatus.NOT_LOADED);

            return this.vm.$store.dispatch(`mail/${ADD_ATTACHMENT}`, {
                message,
                attachment,
                content: file
            });
        });
        return Promise.all(promises);
    }
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
