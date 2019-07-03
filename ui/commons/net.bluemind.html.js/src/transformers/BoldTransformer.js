import EmptyTransformer from './EmptyTransformer';

export default class {
    constructor(transformer) {
        this.transformer = transformer || new EmptyTransformer();
    }

    transform(text) {
        return strongify(this.transformer.transform(text));
    }
}

function strongify(text) {
    let lines = text.split("\n");
    const boldRegex = /[*].+?[*]/g;
    return lines.map(line => line.replace(boldRegex, "<strong>$&</strong>")).join("\n");
}