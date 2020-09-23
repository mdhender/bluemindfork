export default class PartsAddressesByMimeTypeVisitor {
    constructor() {
        this.results = {};
    }

    visit(part) {
        this.results[part.mime] = part.address;
    }

    result() {
        return this.results;
    }
}
