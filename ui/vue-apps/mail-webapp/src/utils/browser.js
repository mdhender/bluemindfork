export function canDisplayType(mimeType) {
    return mimeType.startsWith("image/") || mimeType.startsWith("text/") || mimeType === "application/pdf";
}
