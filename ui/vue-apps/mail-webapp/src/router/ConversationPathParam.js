import PathParam from "~/router/PathParam";

export default class ConversationPathParam extends PathParam {
    static parse(path, defaultFolder) {
        return PathParam._parse(path, defaultFolder, id => id);
    }
}
