import AvailableLanguages from "./AvailableLanguages";

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
        short_date_time: {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
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
        full_date_time_short: {
            weekday: "short",
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
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
        },
        short_month: {
            month: "short"
        },
        short_weekday: {
            weekday: "short"
        },
        year: {
            year: "numeric"
        }
    };

    const hour12 = timeformat === "h:mma";
    Object.entries(dateTimeFormats).map(entry => {
        dateTimeFormats[entry[0]] = { ...entry[1], hour12 };
    });

    return AvailableLanguages.reduce((obj, item) => Object.assign(obj, { [item.value]: dateTimeFormats }), {});
}
