declare module "emailjs-mime-parser" {
    export default function parse(eml: string): MimeNode;
    type MimeNode = {
        childNodes: MimeNode[];
        contentType: Header;
        content: Uint8Array;
        raw: string;
    };
    type Header = {
        value: string;
        params: { [key: string]: string };
        initial: string;
        type: string;
    };
}
