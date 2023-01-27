export function splitHeadersAndContent(content: string): { body: string; headers: string } {
    const separator = "\r\n\r\n";
    const separatorIndex = content.indexOf(separator);
    return {
        body: content.substring(separatorIndex + separator.length),
        headers: content.substring(0, separatorIndex)
    };
}

export function extractContentType(headers: string): string {
    const match = new RegExp(/content-type:\s?((?:(?![\w-]+:).*(?:\r\n|$))*)/gi).exec(headers);
    console.log(match);
    return match && match[1] ? match[1] : "";
}
