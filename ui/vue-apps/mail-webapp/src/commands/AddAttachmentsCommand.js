import UUIDGenerator from "@bluemind/uuid";
import { partUtils, attachmentUtils, draftUtils } from "@bluemind/mail";
import { ADD_ATTACHMENT, DEBOUNCED_SAVE_MESSAGE } from "~/actions";
import TooLargeBox from "~/components/MailAttachment/Modals/TooLargeBox";

const { createFromFile: createPartFromFile } = partUtils;
const { create, AttachmentStatus } = attachmentUtils;
const { isNewMessage } = draftUtils;

export default {
    commands: {
        async addAttachments({ files, message, maxSize }) {
            files = [...files];
            const isNew = isNewMessage(this.message);

            const filesSize = files.reduce((totalSize, file) => totalSize + file.size, 0);
            if (filesSize > maxSize) {
                const { content, props } = renderTooLargeOKBox(this, files, maxSize);
                await this.$bvModal.msgBoxOk([content], props);
                return;
            }

            const promises = files.map(file => {
                const part = createPartFromFile(UUIDGenerator.generate(), file);
                const attachment = create(part, AttachmentStatus.NOT_LOADED);

                return this.$store.dispatch(`mail/${ADD_ATTACHMENT}`, {
                    message,
                    attachment,
                    content: file
                });
            });
            await Promise.all(promises);
            await this.$store.dispatch(`mail/${DEBOUNCED_SAVE_MESSAGE}`, {
                draft: message,
                messageCompose: this.$store.state.mail.messageCompose
            });
            this.updateRoute(isNew);
        }
    },
    computed: {
        maxSize() {
            return this.$store.state.mail.messageCompose.maxMessageSize;
        }
    }
};

function renderTooLargeOKBox(vm, files, sizeLimit) {
    const content = vm.$createElement(TooLargeBox, { props: { sizeLimit, attachmentsCount: files.length } });

    const props = {
        title: vm.$tc("mail.filehosting.add.too_large", files.length),
        okTitle: vm.$tc("common.got_it"),
        bodyClass: "pb-4",
        okVariant: "outline-secondary",
        centered: true
    };

    return { content, props };
}
