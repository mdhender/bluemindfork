import { MimeType } from "@bluemind/email";

export default class GetMultiPartAddressesVisitor {
    constructor() {
        this.results = {};
    }

    visit(part) {
        if (MimeType.isMultipart(part)) {
            this.results[part.mime] = part.address;
        }
    }

    result() {
        return this.results;
    }
}
