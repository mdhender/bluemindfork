/*eslint no-control-regex: 0*/
/*eslint no-useless-escape: 0*/
import BoldTransformer from './transformers/BoldTransformer';
import LinkifyTransformer from './transformers/LinkifyTransformer';
import CallToTransformer from './transformers/CallToTransformer';
import PreTransformer from './transformers/PreTransformer';

export default function(text, additionnalTransformer) {
    let transformer = new LinkifyTransformer(additionnalTransformer); // must be the first transformer to run
    transformer = new CallToTransformer(transformer);
    transformer = new BoldTransformer(transformer);
    transformer = new PreTransformer(transformer); // must be the last transformer to run

    return transformer.transform(text);

    // return (
    //     "<div class='text-preformat'><p>" +
    //     linkified
    //         .replace(/\r?\n/g, "\n")
    //         .trim() // normalize line endings
    //         .replace(/[ \t]+$/gm, "")
    //         .trim() // trim empty line endings
    //         .replace(/\n\n+/g, "</p><p>")
    //         .trim() // insert <p> to multiple linebreaks
    //         .replace(/\n/g, "<br/>") + // insert <br> to single linebreaks
    //     "</p></div>"
    // );
}

