import i18n from "@bluemind/i18n";

const weekday = {
    MO: "common.monday",
    TU: "common.tuesday",
    WE: "common.wednesday",
    TH: "common.thursday",
    FR: "common.friday",
    SA: "common.saturday",
    SU: "common.sunday"
};

export default {
    compute(abbreviation) {
        return i18n.t(weekday[abbreviation]);
    }
};
