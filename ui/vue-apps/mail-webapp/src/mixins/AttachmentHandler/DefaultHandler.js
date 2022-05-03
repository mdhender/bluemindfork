import UUIDGenerator from "@bluemind/uuid";
import { ADD_ATTACHMENT } from "~/actions";
import { createFromFile as createPartFromFile } from "~/model/part";
import { create, AttachmentStatus } from "~/model/attachment";
import ChainOfResponsability from "./ChainOfResponsability";

export default class extends ChainOfResponsability {
    async addAttachments(files, message) {
        const filesSize = files.reduce((totalSize, file) => totalSize + file.size, 0);

        if (filesSize > this.vm.$store.state.mail.messageCompose.maxMessageSize) {
            this.vm.$bvModal.msgBoxOk("Taille max dépassée");
            return Promise.resolve();
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
