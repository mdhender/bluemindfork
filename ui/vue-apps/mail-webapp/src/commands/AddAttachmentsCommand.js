import i18n from "@bluemind/i18n";
import { partUtils, attachmentUtils, fileUtils } from "@bluemind/mail";
import { BmTooLargeBox } from "@bluemind/ui-components";
import UUIDGenerator from "@bluemind/uuid";
import { computed } from "vue";
import { ADD_ATTACHMENT, DEBOUNCED_SAVE_MESSAGE } from "~/actions";
import store from "@bluemind/store";
import { useCommand } from "@bluemind/command";

const { createFromFile: createPartFromFile } = partUtils;
const { create } = attachmentUtils;
const { FileStatus } = fileUtils;

async function addAttachments({ files, message, maxSize }) {
    const filesBlob = [...files];

    const totalSize = filesBlob.reduce((total, blob) => total + blob.size, message.size);
    if (totalSize > maxSize) {
        renderTooLargeFilesModal.call(this, files, maxSize);
        return;
    }

    const promises = filesBlob.map(fileBlob => {
        const part = createPartFromFile({
            address: UUIDGenerator.generate(),
            name: fileBlob.name,
            type: fileBlob.type,
            size: fileBlob.size
        });
        const attachment = create(part, FileStatus.NOT_LOADED);

        return store.dispatch(`mail/${ADD_ATTACHMENT}`, {
            message,
            attachment,
            content: fileBlob
        });
    });
    await Promise.all(promises);
    await store.dispatch(`mail/${DEBOUNCED_SAVE_MESSAGE}`, {
        draft: message,
        messageCompose: store.state.mail.messageCompose,
        files: message.attachments.map(({ fileKey }) => store.state.mail.files[fileKey])
    });
}

export default {
    commands: {
        addAttachments: function (payload) {
            return addAttachments({ maxSize: this.maxSize, ...payload });
        }
    },
    computed: {
        maxSize() {
            return store.state.mail.messageCompose.maxMessageSize;
        }
    }
};
export function useAddAttachmentsCommand() {
    const maxSize = computed(() => store.state.mail.messageCompose.maxMessageSize);
    return {
        maxSize,
        execAddAttachments: useCommand("addAttachments", function (payload) {
            return addAttachments.call(this, { maxSize: maxSize.value, ...payload });
        })
    };
}

async function renderTooLargeFilesModal(files, sizeLimit) {
    const content = this.$createElement(BmTooLargeBox, {
        props: { sizeLimit, attachmentsCount: files.length }
    });

    const props = {
        size: "md",
        title: i18n.tc("mail.actions.attach.too_large", files.length),
        okTitle: i18n.tc("common.got_it"),
        bodyClass: "pb-4",
        okVariant: "outline-accent"
    };

    await this.$bvModal.msgBoxOk([content], props);
}
