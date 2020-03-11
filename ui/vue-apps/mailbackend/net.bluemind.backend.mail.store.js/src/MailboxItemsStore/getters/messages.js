import Message from "../Message";
import { DateRange } from "@bluemind/date";

const Range = (() => {
    const TODAY = DateRange.today();
    TODAY.name = "mail.list.range.today";
    const YESTERDAY = DateRange.yesterday();
    YESTERDAY.name = "mail.list.range.yesterday";
    const THIS_WEEK = DateRange.thisWeek();
    THIS_WEEK.name = "mail.list.range.this_week";
    const OLDER = DateRange.past(THIS_WEEK.start);
    OLDER.name = "mail.list.range.older";
    const RANGES = [TODAY, YESTERDAY, THIS_WEEK, OLDER];

    return {
        getRange: date => RANGES.find(r => r.contains(date)) || RANGES[RANGES.length - 1]
    };
})();

export function messages(state) {
    return state.itemKeys
        .filter(key => state.items[key])
        .map(key => new Message(key, state.items[key]))
        .map((message, index, arr) => {
            message.range = Range.getRange(message.date);
            message.hasSeparator = index === 0 || message.range !== arr[index - 1].range;
            return message;
        });
}
