import { text2html, EmptyTransformer } from "@bluemind/html-utils";
import ForwardTextTransformer from "./transformers/ForwardTextTransformer";
import ReplyTextTransformer from "./transformers/ReplyTextTransformer";

export default function(mailText) {
    let textTransformer = new EmptyTransformer();
    textTransformer = new ForwardTextTransformer(textTransformer);
    textTransformer = new ReplyTextTransformer(textTransformer);
    return text2html(mailText, textTransformer);
}
