export default function (fileSize, vueI18N) {
    if (fileSize / Math.pow(10, 9) >= 1) {
        return vueI18N.t("common.unit.gigabyte", { size: (fileSize / Math.pow(10, 9)).toFixed(1) });
    } else if (fileSize / Math.pow(10, 6) >= 1) {
        return vueI18N.t("common.unit.megabyte", { size: (fileSize / Math.pow(10, 6)).toFixed(1) });
    } else if (fileSize / Math.pow(10, 3) >= 1) {
        return vueI18N.t("common.unit.kilobyte", { size: (fileSize / Math.pow(10, 3)).toFixed(1) });
    }
    return vueI18N.t("common.unit.byte", { size: fileSize });
}
