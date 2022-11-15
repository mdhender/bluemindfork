import fs from "fs";
import path from "path";

export function base64ToArrayBuffer(base64: string): ArrayBuffer {
    const binary_string = atob(base64);
    const len = binary_string.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
        bytes[i] = binary_string.charCodeAt(i);
    }
    return bytes.buffer;
}

export function readFile(filename: string) {
    return fs.readFileSync(path.join(__dirname, `./data/${filename}`), "utf8");
}
