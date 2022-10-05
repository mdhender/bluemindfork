import { attachmentUtils, fileUtils } from "@bluemind/mail";
import EmlParser from "../helpers/EmlParser";

import { ADD_MESSAGES, SET_PART_DATA } from "~/mutations";
import MessageAdaptor from "../helpers/MessageAdaptor";
import { ADD_LOCAL_ATTACHMENT } from "~/actions";

export default async function ({ commit, dispatch }, { emlUrl }) {
    const fetched = await fetch(emlUrl);
    const blob = await fetched.blob();
    const parsed = await EmlParser.parseEml(blob);

    const message = MessageAdaptor.fromMailboxItem(
        { internalId: 0, value: parsed, version: 0 },
        { key: "no-folder", uid: "no-remote-ref-uid" }
    );
    commit(ADD_MESSAGES, { messages: [message] });

    const visit = async part => {
        const partData = parsed.partsData[part.address];
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
    await visit(parsed.body.structure);

    return message;
}
