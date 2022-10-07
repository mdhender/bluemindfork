import UUIDGenerator from "@bluemind/uuid";
import { attachmentUtils, fileUtils, partUtils } from "@bluemind/mail";
import { ADD_FH_FILE } from "./types/actions";
const { createFromFile: createPartFromFile } = partUtils;
const { create, AttachmentAdaptor } = attachmentUtils;
const { FileStatus } = fileUtils;

export default async function addFhAttachment({ commit, dispatch }, { file, message, shareFn }) {
    const attachment = createFhAttachment(file, message);
    const { files, attachments } = AttachmentAdaptor.extractFiles([attachment]);
    const adaptedFile = files[0];
    const adaptedAttachment = attachments[0];
    commit("ADD_ATTACHMENT", { messageKey: message.key, attachment: adaptedAttachment });
    commit("SET_MESSAGE_HAS_ATTACHMENT", { key: message.key, hasAttachment: true });

    const address = await dispatch(ADD_FH_FILE, { message, file: adaptedFile, content: file, shareFn });

    commit("SET_ATTACHMENT_ADDRESS", {
        messageKey: message.key,
        oldAddress: adaptedAttachment.address,
        address
    });
}

function createFhAttachment(file) {
    const attachmentFromFile = {
        ...file,
        ...createPartFromFile(UUIDGenerator.generate(), {
            name: file.name,
            size: 0
        })
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
