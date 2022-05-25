export class UnhandledRequestError extends Error {
    constructor() {
        super("Unhandled api request");
    }
}
