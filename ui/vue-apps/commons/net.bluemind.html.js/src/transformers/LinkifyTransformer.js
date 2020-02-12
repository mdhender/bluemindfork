import linkify from "linkifyjs/string";
import EmptyTransformer from "./EmptyTransformer";

export default class {
    constructor(transformer) {
        this.transformer = transformer || new EmptyTransformer();
    }

    transform(text) {
        return this.transformer.transform(linkify(text)); // transform http links and mailto
    }
}
