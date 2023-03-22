import { MimeType } from "@bluemind/email";

export function isReport(part) {
    switch (part.mime) {
        case MimeType.MESSAGE_DISPOSITION_NOTIFICATION:
            return true;
        default:
            return false;
    }
}
