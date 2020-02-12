import EmptyTransformer from "./EmptyTransformer";

export default class {
    constructor(transformer) {
        this.transformer = transformer || new EmptyTransformer();
    }

    transform(text) {
        return "<pre>" + this.transformer.transform(text) + "</pre>";
    }
}
