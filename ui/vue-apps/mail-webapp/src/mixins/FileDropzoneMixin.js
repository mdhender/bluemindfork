export default {
    methods: {
        shouldActivate(event) {
            // Fallback for Safari: its event.dataTransfer.items is an empty FilesList
            if (event.dataTransfer.items.length === 0) {
                return true;
            }
            const files = getFilesFromEvent(event);
            const regex = "^(?!.*image/(jpeg|jpg|png|gif)).*$";
            const matchFunction = f => f.type.match(new RegExp(regex, "i"));
            return files.some(matchFunction);
        },
        shouldActivateForImages(event) {
            const regex = "image/(jpeg|jpg|png|gif)";
            const files = getFilesFromEvent(event);
            const matchFunction = f => f.type.match(new RegExp(regex, "i"));
            return files.length > 0 && files.every(matchFunction);
        }
    }
};

function getFilesFromEvent(event) {
    return event.dataTransfer.items.length
        ? Object.keys(event.dataTransfer.items).map(key => event.dataTransfer.items[key])
        : [];
}
