import BoldTransformer from "./transformers/BoldTransformer";
import LinkifyTransformer from "./transformers/LinkifyTransformer";
import TelTransformer from "./transformers/TelTransformer";
import PreTransformer from "./transformers/PreTransformer";

export default function (text, userLang, additionnalTransformer) {
    let transformer = new LinkifyTransformer(additionnalTransformer); // must be the first transformer to run
    transformer = new TelTransformer(transformer, userLang);
    transformer = new BoldTransformer(transformer);
    transformer = new PreTransformer(transformer); // must be the last transformer to run

    return transformer.transform(text);
}
