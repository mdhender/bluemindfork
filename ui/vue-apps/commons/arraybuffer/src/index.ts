export function binaryToArrayBuffer(binaryString: string): ArrayBuffer {
    const len = binaryString.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
        bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes.buffer;
}

export function base64ToArrayBuffer(base64: string): ArrayBuffer {
    const binary_string = atob(base64);
    return binaryToArrayBuffer(binary_string);
}
