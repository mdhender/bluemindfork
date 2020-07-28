const weekday = {
    MO: "common.monday",
    TU: "common.tuesday",
    WE: "common.wednesday",
    TH: "common.thursday",
    FR: "common.friday",
    SA: "common.saturday",
    SU: "common.sunday"
};

import injector from "@bluemind/inject";

export default {
    compute(abbreviation) {
        return injector
            .getProvider("i18n")
            .get()
            .t(weekday[abbreviation]);
    }
};
