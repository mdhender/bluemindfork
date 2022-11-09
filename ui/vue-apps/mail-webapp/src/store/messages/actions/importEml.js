import { attachmentUtils, fileUtils } from "@bluemind/mail";
import { messageUtils } from "@bluemind/mail";

import { ADD_MESSAGES, SET_PART_DATA } from "~/mutations";
import { ADD_LOCAL_ATTACHMENT } from "~/actions";

export default async function ({ commit, dispatch }, { emlUrl }) {
    const fetched = await fetch(emlUrl);
    const blob = await fetched.blob();
    const { body, partsData, uid } = await messageUtils.EmlParser.parseEml(blob);
    const message = messageUtils.MessageAdaptor.fromMailboxItem(
        { internalId: 0, value: { body, partsData }, version: 0 },
        {
            key: "importedEmlFakeFolderKey-" + uid,
            uid: "importedEmlFakeFolderKeyRemoteRefUid-" + uid
        }
    );
    commit(ADD_MESSAGES, { messages: [message] });

    const visit = async part => {
        const partData = partsData[part.address];
        if (part.dispositionType === "ATTACHMENT") {
            await dispatch(ADD_LOCAL_ATTACHMENT, {
                message,
                attachment: attachmentUtils.create(part, fileUtils.FileStatus.ONLY_LOCAL),
                content: partData
            });
        } else if (partData) {
            commit(SET_PART_DATA, {
                messageKey: message.key,
                data: partData,
                address: part.address
            });
        }
        part.children?.forEach(visit);
    };
    await visit(body.structure);

    return message;
}
