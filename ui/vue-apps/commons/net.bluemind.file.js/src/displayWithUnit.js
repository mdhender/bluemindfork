export default function (fileSize, powerOfTen, vueI18N) {
    if (powerOfTen === 9) {
        return vueI18N.t("common.unit.gigabyte", { size: (fileSize / Math.pow(10, 9)).toFixed(1) });
    } else if (powerOfTen === 6) {
        return vueI18N.t("common.unit.megabyte", { size: (fileSize / Math.pow(10, 6)).toFixed(1) });
    } else if (powerOfTen === 3) {
        return vueI18N.t("common.unit.kilobyte", { size: (fileSize / Math.pow(10, 3)).toFixed(1) });
    }
    return vueI18N.t("common.unit.byte", { size: fileSize });
}
