import allDay from "../data/events/allDay";
import basic from "../data/events/basic";
import recurrentEveryDay from "../data/events/recurrentEveryDay";
import recurrentOnceAWeek from "../data/events/recurrentOnceAWeek";
import recurrentOnceAWeekAllDay from "../data/events/recurrentOnceAWeekAllDay";
import recurrentTwiceAMonth from "../data/events/recurrentTwiceAMonth";

import EventHelper from "../../helpers/EventHelper";

import { InjectI18NDependency } from "@bluemind/test-utils";
InjectI18NDependency.registerCommonL10N();

describe("EventHelper adapter", () => {
    const userUid = "B2CBEEFD-147C-451A-9229-1B6C9697D202";

    test("adapt events match snapshots", () => {
        const events = [
            allDay,
            basic,
            recurrentEveryDay,
            recurrentOnceAWeek,
            recurrentOnceAWeekAllDay,
            recurrentTwiceAMonth
        ];
        events.forEach(e => {
            const adapted = EventHelper.adapt(e, userUid);
            expect(adapted).toMatchSnapshot();
        });
    });
});
