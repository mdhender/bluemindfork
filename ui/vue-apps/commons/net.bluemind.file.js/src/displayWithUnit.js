export default function (fileSize, unit) {
    if (unit === "Go") {
        return (fileSize / Math.pow(10, 9)).toFixed(1) + " Go";
    } else if (unit === "Mo") {
        return (fileSize / Math.pow(10, 6)).toFixed(1) + " Mo";
    } else if (unit === "Ko") {
        return (fileSize / Math.pow(10, 3)).toFixed(1) + " Ko";
    } else if (unit === "o") {
        return fileSize + " o";
    }
}
