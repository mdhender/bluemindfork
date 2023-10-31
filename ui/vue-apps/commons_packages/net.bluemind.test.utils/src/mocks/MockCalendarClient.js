import { CalendarClient } from "@bluemind/calendar.api";

const mockedCalendarClient = jest.genMockFromModule("@bluemind/calendar.api").CalendarClient;

Object.getOwnPropertyNames(CalendarClient.prototype).forEach(property => {
    // every function of MailboxFoldersClient is mocked and return a Promise.resolve
    if (typeof CalendarClient.prototype[property] === "function") {
        mockedCalendarClient.prototype[property] = jest.fn().mockReturnValue(Promise.resolve());
    }
});

export default mockedCalendarClient;
