import tzdata from "tzdata";
import DateComparator from "./DateComparator";
import DateRange from "./DateRange";
import WeekDayCodes from "./WeekDayCodes";

const AvailableDateFormats = [
    { text: "31/12/2012", value: "dd/MM/yyyy" },
    { text: "2012-12-31", value: "yyyy-MM-dd" },
    { text: "12/31/2012", value: "MM/dd/yyyy" },
    { text: "31.12.2012", value: "dd.MM.yyyy" }
];

const AvailableTimeFormats = [
    { text: "1:00pm", value: "h:mma" },
    { text: "13:00", value: "HH:mm" }
];

const AvailablesTimezones = Object.keys(tzdata.zones);

const SECONDS_PER_MINUTE = 60;
const SECONDS_PER_HOUR = SECONDS_PER_MINUTE * 60;
const SECONDS_PER_DAY = SECONDS_PER_HOUR * 24;

export {
    AvailableDateFormats,
    AvailableTimeFormats,
    AvailablesTimezones,
    DateComparator,
    DateRange,
    SECONDS_PER_MINUTE,
    SECONDS_PER_HOUR,
    SECONDS_PER_DAY,
    WeekDayCodes
};
