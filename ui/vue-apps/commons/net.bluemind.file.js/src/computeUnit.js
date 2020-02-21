import roundToOneDecimal from "./roundToOneDecimal";

export default function(fileSize) {
    if (fileSize / Math.pow(10, 9) >= 1) {
        return roundToOneDecimal(fileSize / Math.pow(10, 9)) + " Go";
    } else if (fileSize / Math.pow(10, 6) >= 1) {
        return roundToOneDecimal(fileSize / Math.pow(10, 6)) + " Mo";
    } else if (fileSize / Math.pow(10, 3) >= 1) {
        return roundToOneDecimal(fileSize / Math.pow(10, 3)) + " Ko";
    } else {
        return fileSize + " o";
    }
}
