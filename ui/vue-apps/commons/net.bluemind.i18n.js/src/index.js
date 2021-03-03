import generateDateTimeFormats from "./DateTimeFormats";
import FirstDayOfWeek from "./FirstDayOfWeek";
import InheritTranslationsMixin from "./InheritTranslationsMixin";
import TranslationHelper from "./TranslationHelper";
import WeekDay from "./WeekDay";

const AvailableLanguages = [
    { text: "Deutsch", value: "de" },
    { text: "English", value: "en" },
    { text: "Español", value: "es" },
    { text: "Français", value: "fr" },
    { text: "Italiano", value: "it" },
    { text: "Polski", value: "pl" },
    { text: "Slovenský", value: "sk" },
    { text: "中国的", value: "zh" }
];

export {
    AvailableLanguages,
    FirstDayOfWeek,
    generateDateTimeFormats,
    InheritTranslationsMixin,
    TranslationHelper,
    WeekDay
};
