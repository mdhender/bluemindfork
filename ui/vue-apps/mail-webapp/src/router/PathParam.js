export default class PathParam {
    static _parse(path, defaultFolder, mapper) {
        if (path.includes(":")) {
            const [folderKey, id, action, relatedFolderKey, relatedId] = path.split(":");
            return { folderKey, internalId: mapper(id), action, relatedFolderKey, relatedId: mapper(relatedId) };
        } else {
            return { folderKey: defaultFolder, internalId: mapper(path) };
        }
    }

    static build(path, message, action, related) {
        if (message === undefined) {
            return path;
        } else if (message) {
            let builtPath = message.folderRef.key + ":" + message.remoteRef.internalId;
            if (action) {
                builtPath += ":" + action;
            }
            if (related) {
                builtPath += ":" + related.folderRef.key + ":" + related.remoteRef.internalId;
            }
            return builtPath;
        }
    }
}
