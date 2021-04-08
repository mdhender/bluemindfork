export default {
    parse(path, defaultFolder) {
        if (path.includes(":")) {
            const [folderKey, id] = path.split(":");
            return { folderKey, messageId: parseInt(id) };
        } else {
            return { folderKey: defaultFolder, messageId: parseInt(path) };
        }
    },
    build(path, message) {
        if (message === undefined) {
            return path;
        } else if (message) {
            return message.folderRef.key + ":" + message.remoteRef.internalId;
        }
    }
};
