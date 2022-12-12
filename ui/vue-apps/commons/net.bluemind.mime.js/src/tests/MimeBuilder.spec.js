import MimeBuilder from "../MimeBuilder";
import { Blob } from "buffer";
import basic from "./datas/bodyStructures/basic";
import alternative from "./datas/bodyStructures/alternative";
import withAttachment from "./datas/bodyStructures/withAttachment";
import withInlineImage from "./datas/bodyStructures/withInlineImage";

const getContent = async part => {
    let blob;
    if (part.mime === "text/plain") {
        blob = new Blob(["text"]);
    } else if (part.mime === "text/html") {
        blob = new Blob(["<div>text</div>"]);
    } else if (part.mime === "image/png") {
        blob = new Blob(["a mocked image"], { type: "image/png" });
    }
    const content = await blob.arrayBuffer();
    return Promise.resolve(new Uint8Array(content));
};
const builder = new MimeBuilder(getContent);

describe("MimeBuilder", () => {
    test("basic", async () => {
        const mimeTree = await builder.build(basic);
        expect(removeRandomBoundaries(mimeTree)).toMatchSnapshot();
    });
    test("alternative", async () => {
        const mimeTree = await builder.build(alternative);
        expect(removeRandomBoundaries(mimeTree)).toMatchSnapshot();
    });

    test("with attachment", async () => {
        const mimeTree = await builder.build(withAttachment);
        expect(removeRandomBoundaries(mimeTree)).toMatchSnapshot();
    });

    test("with inline image", async () => {
        const mimeTree = await builder.build(withInlineImage);
        expect(removeRandomBoundaries(mimeTree)).toMatchSnapshot();
    });
});

// Remove boundaries randomly generated to allow a snapshot match
function removeRandomBoundaries(mimeTree) {
    const regex = /---.*/g;
    return mimeTree.replaceAll(regex, "");
}
