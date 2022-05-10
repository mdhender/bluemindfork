import DefaultHandler from "../AttachmentHandler/DefaultHandler";
import FileHostingHandler from "../AttachmentHandler/FileHostingHandler";
import ChainOfResponsability from "../AttachmentHandler/ChainOfResponsability";
const vm = { $store: { commit: jest.fn(), dispatch: jest.fn() } };

describe("Sort Handlers with priority", () => {
    test("between default and high priority", () => {
        const defaultHandler = new ChainOfResponsability(vm);
        expect(defaultHandler.priority).toStrictEqual(0);

        const highPriority = new ChainOfResponsability(vm, 100);
        const attachmentHandler = defaultHandler.chain(highPriority);
        expect(attachmentHandler).toBe(highPriority);

        const attachmentHandler2 = highPriority.chain(defaultHandler);
        expect(attachmentHandler2).toBe(highPriority);

        expect(highPriority.next).toBe(defaultHandler);
        expect(defaultHandler.next).not.toBe(highPriority);
    });

    test("with many unsorted handlers", () => {
        const defaultHandler = new ChainOfResponsability(vm);
        const one = new ChainOfResponsability(vm, 1);
        const two = new ChainOfResponsability(vm, 2);
        const ten = new ChainOfResponsability(vm, 10);
        const ninety = new ChainOfResponsability(vm, 90);
        const oneHundred = new ChainOfResponsability(vm, 100);

        const unsortedHandlers = [one, ninety, ten, oneHundred, defaultHandler, two];
        const handler = unsortedHandlers.reduce((previous, handler) => previous.chain(handler));

        expect(handler).toBe(oneHundred);
        expect(oneHundred.next).toBe(ninety);
        expect(ninety.next).toBe(ten);
        expect(ten.next).toBe(two);
        expect(two.next).toBe(one);
        expect(one.next).toBe(defaultHandler);

        const unsortedHandlers2 = [two, defaultHandler, ninety, oneHundred, one, ten];
        const handler2 = unsortedHandlers2.reduce((previous, handler) => previous.chain(handler));

        expect(handler2).toBe(oneHundred);
        expect(oneHundred.next).toBe(ninety);
        expect(ninety.next).toBe(ten);
        expect(ten.next).toBe(two);
        expect(two.next).toBe(one);
        expect(one.next).toBe(defaultHandler);
    });

    test("with real handlers", () => {
        const fileHostingHandler = new FileHostingHandler(vm);
        const defaultHandler = new DefaultHandler(vm);
        const attachmentHandler = fileHostingHandler.chain(defaultHandler);
        expect(attachmentHandler).toBe(fileHostingHandler);

        expect(fileHostingHandler.next).toBe(defaultHandler);
        expect(defaultHandler.next).not.toBe(fileHostingHandler);

        const fileHostingHandler2 = new FileHostingHandler(vm);
        const defaultHandler2 = new DefaultHandler(vm);

        const attachmentHandler2 = defaultHandler2.chain(fileHostingHandler2);
        expect(attachmentHandler2).toBe(fileHostingHandler2);

        expect(fileHostingHandler2.next).toBe(defaultHandler2);
        expect(defaultHandler2.next).not.toBe(fileHostingHandler2);
    });
});
