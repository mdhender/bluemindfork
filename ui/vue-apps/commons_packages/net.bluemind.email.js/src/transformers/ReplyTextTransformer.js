import { EmptyTransformer } from "@bluemind/html-utils";

export default class {
    constructor(transformer) {
        this.transformer = transformer || new EmptyTransformer();
    }

    transform(text) {
        return this.transformer.transform(processReplies(text));
    }
}

function processReplies(text) {
    let lines = text.split("\n");
    const blockquoteReplyBlock = "<blockquote class='reply'>";
    const endOfBlockquote = "</blockquote>";
    const replySymbol = "&gt;";

    let newReplyParsingNeeded = true;

    while (newReplyParsingNeeded) {
        let preIsBlockQuote = false;
        newReplyParsingNeeded = false;
        for (let i = 0; i < lines.length; i++) {
            let parsedLine = lines[i];
            let keepMe = "";

            while (parsedLine.startsWith(blockquoteReplyBlock)) {
                keepMe += blockquoteReplyBlock;
                parsedLine = parsedLine.substring(blockquoteReplyBlock.length, parsedLine.length);
            }
            while (parsedLine.startsWith(endOfBlockquote)) {
                keepMe += endOfBlockquote;
                parsedLine = parsedLine.substring(endOfBlockquote.length, parsedLine.length);
            }

            const lineIsBlockQuote = parsedLine.startsWith(replySymbol);

            if (lineIsBlockQuote) {
                newReplyParsingNeeded = true;
                parsedLine = parsedLine.substring(replySymbol.length, parsedLine.length);
            }

            if ((preIsBlockQuote && lineIsBlockQuote) || (!preIsBlockQuote && !lineIsBlockQuote)) {
                lines[i] = keepMe + parsedLine;
                continue;
            }
            if (!preIsBlockQuote && lineIsBlockQuote) {
                preIsBlockQuote = true;
                parsedLine = blockquoteReplyBlock + parsedLine;
            } else if (preIsBlockQuote && !lineIsBlockQuote) {
                preIsBlockQuote = false;
                parsedLine = endOfBlockquote + parsedLine;
            }

            lines[i] = keepMe + parsedLine;
        }
    }

    return lines.join("\n");
}
