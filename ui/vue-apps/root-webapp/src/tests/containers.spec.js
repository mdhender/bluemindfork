import { Verb } from "@bluemind/core.container.api";
import inject from "@bluemind/inject";
import {
    default as calendarHelper,
    CalendarRight
} from "../components/preferences/fields/customs/ContainersManagement/Calendars/helper";

const setAccessControlList = jest.fn();
inject.register({ provide: "ContainerManagementPersistence", factory: () => ({ setAccessControlList }) });

const subject = "toto";

describe("Containers", () => {
    describe("Calendar", () => {
        const uid = "calendarUid";

        test.each`
            rightName                    | expectedAcl
            ${"CANT_INVITE_ME"}          | ${[]}
            ${"CAN_INVITE_ME"}           | ${[{ subject, verb: Verb.Invitation }]}
            ${"CAN_SEE_MY_AVAILABILITY"} | ${[{ subject, verb: Verb.Invitation }]}
            ${"CAN_SEE_MY_EVENTS"}       | ${[{ subject, verb: Verb.Read }]}
            ${"CAN_EDIT_MY_EVENTS"}      | ${[{ subject, verb: Verb.Write }]}
            ${"CAN_MANAGE_SHARES"}       | ${[{ subject, verb: Verb.Write }, { subject, verb: Verb.Manage }]}
        `("saveRights $rightName", async ({ rightName, expectedAcl }) => {
            const right = CalendarRight[rightName];
            await calendarHelper.saveRights({ [subject]: right }, { uid });
            expect(setAccessControlList).toHaveBeenCalledWith(expectedAcl);
        });
    });
});
