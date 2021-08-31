import PathParam from "~/router/PathParam";

export default class ConversationPathParam extends PathParam {
    static parse(path, defaultFolder) {
        return PathParam._parse(path, defaultFolder, id => id);
    }

    static build(path, conversation, action, related) {
        if (conversation === undefined) {
            return path;
        } else if (conversation) {
            let builtPath = conversation.folderRef.key + ":" + conversation.remoteRef.uid;
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
