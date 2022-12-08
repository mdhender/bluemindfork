export const iframe = {
    id: "@bluemind/iframe",
    listeners: {
        "interactions:action-start": ({ iEvent }) =>
            iEvent.interactable.context().body.classList.add("bm-drag-in-progress"),
        "interactions:action-end": ({ iEvent }) =>
            iEvent.interactable.context().body.classList.remove("bm-drag-in-progress")
    }
};
