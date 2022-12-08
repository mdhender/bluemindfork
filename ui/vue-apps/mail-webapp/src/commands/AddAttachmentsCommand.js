import { partUtils, attachmentUtils, fileUtils } from "@bluemind/mail";
import { BmTooLargeBox } from "@bluemind/ui-components";
import UUIDGenerator from "@bluemind/uuid";
import { ADD_ATTACHMENT, DEBOUNCED_SAVE_MESSAGE } from "~/actions";

const { createFromFile: createPartFromFile } = partUtils;
const { create } = attachmentUtils;
const { FileStatus } = fileUtils;

export default {
    commands: {
        async addAttachments({ files, message, maxSize = this.maxSize }) {
            files = [...files];

            const totalSize = files.reduce((total, attachment) => total + attachment.size, message.size);
            if (totalSize > maxSize) {
                renderTooLargeFilesModal(this, files, maxSize);
                return;
            }

            const promises = files.map(file => {
                const part = createPartFromFile(UUIDGenerator.generate(), file);
                const attachment = create(part, FileStatus.NOT_LOADED);

                return this.$store.dispatch(`mail/${ADD_ATTACHMENT}`, {
                    message,
                    attachment,
                    content: file
                });
            });
            await Promise.all(promises);
            await this.$store.dispatch(`mail/${DEBOUNCED_SAVE_MESSAGE}`, {
                draft: message,
                messageCompose: this.$store.state.mail.messageCompose,
                files: message.attachments.map(({ fileKey }) => this.$store.state.mail.files[fileKey])
            });
        }
    },
    computed: {
        maxSize() {
            return this.$store.state.mail.messageCompose.maxMessageSize;
        }
    }
};

async function renderTooLargeFilesModal(vm, files, sizeLimit) {
    const content = vm.$createElement(BmTooLargeBox, {
        props: { sizeLimit, attachmentsCount: files.length },
        scopedSlots: { default: () => vm.$t("mail.actions.attach.max_size") }
    });

    const props = {
        title: vm.$tc("mail.actions.attach.too_large", files.length),
        okTitle: vm.$tc("common.got_it"),
        bodyClass: "pb-4",
        okVariant: "outline-accent",
        centered: true
    };

    await vm.$bvModal.msgBoxOk([content], props);
}
