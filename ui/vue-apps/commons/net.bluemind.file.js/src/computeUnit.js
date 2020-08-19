export default function (fileSize) {
    if (fileSize / Math.pow(10, 9) >= 1) {
        return (fileSize / Math.pow(10, 9)).toFixed(1) + " Go";
    } else if (fileSize / Math.pow(10, 6) >= 1) {
        return (fileSize / Math.pow(10, 6)).toFixed(1) + " Mo";
    } else if (fileSize / Math.pow(10, 3) >= 1) {
        return (fileSize / Math.pow(10, 3)).toFixed(1) + " Ko";
    } else {
        return fileSize + " o";
    }
}
