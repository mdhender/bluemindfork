import UUIDGenerator from "@bluemind/uuid";
import { attachmentUtils, fileUtils, partUtils } from "@bluemind/mail";
import { ADD_FH_FILE } from "./types/actions";
const { createFromFile: createPartFromFile } = partUtils;
const { create, AttachmentAdaptor } = attachmentUtils;
const { FileStatus } = fileUtils;
import { PartsBuilder } from "@bluemind/email";

export default async function addFhAttachment({ commit, dispatch }, { file, message, shareFn }) {
    const attachment = createFhAttachment(file, message);
    const adaptedAttachment = PartsBuilder.createAttachmentPart(attachment);

    const adaptedFile = AttachmentAdaptor.extractFiles([attachment], message).pop();

    commit("ADD_ATTACHMENT", { messageKey: message.key, attachment: adaptedAttachment });

    const { address, headers } = await dispatch(ADD_FH_FILE, { message, file: adaptedFile, content: file, shareFn });

    commit("SET_ATTACHMENT_ADDRESS", {
        messageKey: message.key,
        oldAddress: adaptedAttachment.address,
        address
    });
    commit("SET_ATTACHMENT_HEADERS", {
        messageKey: message.key,
        address,
        headers
    });
}

function createFhAttachment(file) {
    const attachmentFromFile = {
        ...file,
        ...createPartFromFile({ address: UUIDGenerator.generate(), name: file.name, size: 0 })
    };
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
        FileStatus.NOT_LOADED
    );
    return attachment;
}
