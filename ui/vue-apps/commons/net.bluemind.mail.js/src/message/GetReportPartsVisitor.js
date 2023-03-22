import { isReport } from "../report";

export default class GetReportPartsVisitor {
    constructor() {
        this.results = [];
    }

    visit(part) {
        if (isReport(part)) {
            this.results.push(part);
        }
    }

    result() {
        return this.results;
    }
}
