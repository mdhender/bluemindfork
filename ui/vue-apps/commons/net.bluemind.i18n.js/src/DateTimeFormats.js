import { AvailableLanguages } from "./index";

export default function (timeformat) {
    let dateTimeFormats = {
        short_date: {
            day: "2-digit",
            month: "2-digit",
            year: "numeric"
        },
        short_time: {
            hour: "2-digit",
            minute: "2-digit"
        },
        relative_date: {
            weekday: "short",
            day: "2-digit",
            month: "2-digit"
        },
        day_month: {
            day: "2-digit",
            month: "long"
        },
        full_date: {
            weekday: "short",
            day: "2-digit",
            month: "2-digit",
            year: "numeric"
        },
        full_date_long: {
            weekday: "long",
            day: "2-digit",
            month: "long",
            year: "numeric"
        },
        full_date_time: {
            weekday: "long",
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        },
        full_date_time_long: {
            weekday: "long",
            day: "2-digit",
            month: "long",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        },
        month: {
            month: "long"
        }
    };

    const hour12 = timeformat === "h:mma";
    if (hour12) {
        Object.entries(dateTimeFormats).map(entry => {
            dateTimeFormats[entry[0]] = { ...entry[1], hour12: true };
        });
    }

    return AvailableLanguages.reduce((obj, item) => Object.assign(obj, { [item.value]: dateTimeFormats }), {});
}
