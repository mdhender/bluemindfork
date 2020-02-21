import roundToOneDecimal from "./roundToOneDecimal";

export default function(fileSize, unit) {
    if (unit === "Go") {
        return roundToOneDecimal(fileSize / Math.pow(10, 9)) + " Go";
    } else if (unit === "Mo") {
        return roundToOneDecimal(fileSize / Math.pow(10, 6)) + " Mo";
    } else if (unit === "Ko") {
        return roundToOneDecimal(fileSize / Math.pow(10, 3)) + " Ko";
    } else if (unit === "o") {
        return fileSize + " o";
    }
}
