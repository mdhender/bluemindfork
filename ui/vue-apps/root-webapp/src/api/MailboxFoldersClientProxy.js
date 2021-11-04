import { MailboxFoldersClient } from "@bluemind/backend.mail.api";

let sequentialRequest = Promise.resolve();
export class MailboxFoldersClientProxy extends MailboxFoldersClient {
    importItems(...args) {
        sequentialRequest = sequentialRequest
            .then(() => super.importItems(...args))
            .catch(() => super.importItems(...args));
        return sequentialRequest;
    }
}
