export default function (fileSize, vueI18N, options = null) {
    if (fileSize == null) {
        return "-";
    }
    let fileType = "common.unit.byte";
    if (fileSize / Math.pow(10, 9) >= 1) {
        fileSize = fileSize / Math.pow(10, 9);
        fileType = "common.unit.gigabyte";
    } else if (fileSize / Math.pow(10, 6) >= 1) {
        fileSize = fileSize / Math.pow(10, 6);
        fileType = "common.unit.megabyte";
    } else if (fileSize / Math.pow(10, 3) >= 1) {
        fileSize = fileSize / Math.pow(10, 3);
        fileType = "common.unit.kilobyte";
    }

    let fileSizeFormatted = fileSize.toFixed(1);
    if (fileType == "common.unit.byte") {
        fileSizeFormatted = fileSize.toFixed(0);
    } else if (options !== null && options.precision !== undefined) {
        fileSizeFormatted = formatWithPrecision(fileSize, options.precision);
    }
    return vueI18N.t(fileType, { size: fileSizeFormatted });
}

function formatWithPrecision(fileSize, precision) {
    const fileSizeStr = fileSize.toString();
    const integersBeforeDecimal = fileSizeStr.indexOf(".");

    let precisionUpdated = 0;
    if (integersBeforeDecimal !== -1) {
        precisionUpdated = Math.max(0, precision - integersBeforeDecimal);
    } else {
        precisionUpdated = Math.max(0, precision - fileSizeStr.length);
    }
    return fileSize.toFixed(precisionUpdated);
}
