import { base } from "./base";
import { tooltip } from "./tooltip";
import { events } from "./events";
import { states } from "./states";
import { holdover } from "./holdover";
import { refresh } from "./refresh";

describe("Drag And Drop base plugin", () => {
    test("install all other plugins", () => {
        const scope = { usePlugin: jest.fn() };
        base.install(scope);
        expect(scope.usePlugin).toHaveBeenCalledWith(tooltip);
        expect(scope.usePlugin).toHaveBeenCalledWith(events);
        expect(scope.usePlugin).toHaveBeenCalledWith(states);
        expect(scope.usePlugin).toHaveBeenCalledWith(holdover);
        expect(scope.usePlugin).toHaveBeenCalledWith(refresh);
    });
    test("add name and data to the interactable instance", () => {
        const options = { name: "MyName", data: { some: "data" } };
        const interactable = {};
        base.listeners["interactable:new"]({ interactable, options });
        expect(interactable.name).toBe(options.name);
        expect(interactable.data).toBe(options.data);
    });
    test("add a custom activate options for dropzone", () => {
        const scope = {
            interactables: {
                list: [{ options: { drop: { accept: ["foo", "bar"] } } }, { options: { drop: { accept: ["foo"] } } }]
            },
            dynamicDrop: false
        };
        const interaction = { iEvent: { interactable: { name: "foo" } } };

        base.listeners["interactions:action-start"](interaction, scope, "interactions:action-start");
        expect(scope.interactables.list[0].options.drop.enabled).toBeTruthy();
        expect(scope.interactables.list[1].options.drop.enabled).toBeTruthy();

        interaction.iEvent.interactable.name = "bar";
        base.listeners["interactions:action-start"](interaction, scope, "interactions:action-start");
        expect(scope.interactables.list[0].options.drop.enabled).toBeTruthy();
        expect(scope.interactables.list[1].options.drop.enabled).toBeFalsy();

        scope.interactables.list.forEach(i => (i.options.drop.enabled = false));
        base.listeners["interactions:action-move"](interaction, scope, "interactions:action-move");
        scope.interactables.list.forEach(i => expect(i.options.drop.enabled).toBeFalsy());
        scope.dynamicDrop = true;
        interaction.iEvent.interactable.name = "foo";
        base.listeners["interactions:action-move"](interaction, scope, "interactions:action-move");
        scope.interactables.list.forEach(i => expect(i.options.drop.enabled).toBeTruthy());
    });
});
