import { MimeType } from "@bluemind/email";

export class CalendarPartVisitor {
    constructor() {
        this.results = [];
    }

    visit(part) {
        if (part.mime === MimeType.TEXT_CALENDAR) {
            this.results.push(part);
        }
    }

    result() {
        return this.results.slice();
    }
}
