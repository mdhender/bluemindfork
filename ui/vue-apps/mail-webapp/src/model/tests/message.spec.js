import { create, isEmpty } from "../message";

const emptyContent = "";

test("check emptiness (basic)", () => {
    const message = create();
    expect(isEmpty(message, emptyContent)).toBe(true);
});
test("check content emptiness", () => {
    const message = create();
    const content = '<div id="bm-composer-content-wrapper"><style></style></div>';
    expect(isEmpty(message, content)).toBe(true);
    expect(isEmpty(message, emptyContent)).toBe(true);
});
test("check recipients emptiness", () => {
    const message = create();
    message.to = ["aaa@aaa.aaa"];
    expect(isEmpty(message, emptyContent)).toBe(false);
    message.to = [];
    expect(isEmpty(message, emptyContent)).toBe(true);
    message.cc = ["aaa@aaa.aaa"];
    expect(isEmpty(message, emptyContent)).toBe(false);
    message.cc = [];
    expect(isEmpty(message, emptyContent)).toBe(true);
    message.bcc = ["aaa@aaa.aaa"];
    expect(isEmpty(message, emptyContent)).toBe(false);
});
test("check subject emptiness", () => {
    const message = create();
    message.subject = "Hello world";
    expect(isEmpty(message, emptyContent)).toBe(false);
    message.subject = "";
    expect(isEmpty(message, emptyContent)).toBe(true);
    message.subject = " ";
    expect(isEmpty(message, emptyContent)).toBe(true);
});
