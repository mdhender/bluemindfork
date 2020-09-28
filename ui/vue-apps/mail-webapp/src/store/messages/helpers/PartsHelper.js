import { inject } from "@bluemind/inject";
import { MimeType } from "@bluemind/email";

export default {
    // FIXME: duplicated code with fetch action. Remove fetch action once MailViewer is refactored
    fetch: async (messageImapUid, folderUid, part, isAttachment) => {
        const stream = await inject("MailboxItemsPersistence", folderUid).fetch(
            messageImapUid,
            part.address,
            part.encoding,
            part.mime,
            part.charset
        );
        if (!isAttachment && (MimeType.isText(part) || MimeType.isHtml(part) || MimeType.isCalendar(part))) {
            return new Promise(resolve => {
                const reader = new FileReader();
                reader.readAsText(stream, part.encoding);
                reader.addEventListener("loadend", e => {
                    resolve(e.target.result);
                });
            });
        } else {
            return stream;
        }
    },

    clean: async (partAddresses, attachmentAddresses, service) => {
        const promises = [];
        Object.keys(partAddresses).forEach(mimeType => {
            partAddresses[mimeType].forEach(address => {
                promises.push(service.removePart(address));
            });
        });
        attachmentAddresses.forEach(address => promises.push(service.removePart(address)));
        return Promise.all(promises);
    }
};
