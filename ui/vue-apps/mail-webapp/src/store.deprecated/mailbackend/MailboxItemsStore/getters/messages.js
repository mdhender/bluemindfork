import { DateRange } from "@bluemind/date";
import Message from "../Message";
import MessageAdaptor from "../../../../store/messages/MessageAdaptor";

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

// FIXME after store migration this code should be a computed in MessageList component
//      range and separator can also be improved at this moment + need to fix : https://forge.bluemind.net/jira/browse/FEATWEBML-868
export function messages(state, getters, rootState, rootGetters) {
    return rootState.mail.messageList.messageKeys
        .filter(key => rootGetters["mail/isLoaded"](key))
        .map(key => new Message(key, MessageAdaptor.toMailboxItem(rootState.mail.messages[key])))
        .map((message, index, arr) => {
            message.range = Range.getRange(message.date);
            message.hasSeparator = index === 0 || message.range !== arr[index - 1].range;
            return message;
        });
}
