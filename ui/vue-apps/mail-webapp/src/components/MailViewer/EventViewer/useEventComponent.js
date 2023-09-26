import { computed } from "vue";
import * as EVENT_COMPONENT from ".";

export default function useEventComponent(eventType) {
    return computed(() => {
        const ImipTypeToComponent = {
            LOADING: EVENT_COMPONENT.EventLoading,
            CANCELED: EVENT_COMPONENT.EventCanceled,
            NOT_FOUND: EVENT_COMPONENT.EventNotFound,
            REQUEST: EVENT_COMPONENT.EventRequest,
            COUNTER: EVENT_COMPONENT.EventCountered,
            REPLY: EVENT_COMPONENT.EventReplied,
            DECLINE_COUNTER: EVENT_COMPONENT.EventDeclineCounter
        };

        return ImipTypeToComponent[eventType.value];
    });
}
