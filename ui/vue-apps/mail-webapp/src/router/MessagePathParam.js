import PathParam from "~/router/PathParam";

export default class MessagePathParam extends PathParam {
    static parse(path, defaultFolder) {
        return PathParam._parse(path, defaultFolder, parseInt);
    }
}
