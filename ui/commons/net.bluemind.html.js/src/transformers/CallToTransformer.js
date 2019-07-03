import { findNumbers } from 'libphonenumber-js';
import EmptyTransformer from "./EmptyTransformer";

export default class {
    constructor(transformer) {
        this.transformer = transformer || new EmptyTransformer();
    }

    transform(text) {
        return linkify(this.transformer.transform(text), 'FR'); // TODO: Fixme 'FR'
    }
}


function linkify(text, userLang) {
    let lines = text.split("\n");
    lines = lines.map(line => {

        let before = "";
        let after = "";
        const tels = findNumbers(
            line,
            userLang.toUpperCase(),
            { v2: true }
        );
        tels.forEach(tel => {
            before = line.substring(0, tel.startsAt);
            after = line.substring(tel.endsAt, line.length - 1);
            const adaptedCallTo = '<a href="callto:' //
            + tel.number.number //
            + '">' //
            + line.substring(tel.startsAt, tel.endsAt) //
            + '</a>';
            line = before + adaptedCallTo + after;
        });
        return line;
    });
    return lines.join("\n");
}