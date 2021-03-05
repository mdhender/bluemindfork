import { zones } from "tzdata";
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

const AvailablesTimezones = Object.keys(zones);

export { AvailableDateFormats, AvailableTimeFormats, AvailablesTimezones, DateComparator, DateRange, WeekDayCodes };
