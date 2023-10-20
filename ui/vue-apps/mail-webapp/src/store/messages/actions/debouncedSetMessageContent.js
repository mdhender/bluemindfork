import debounce from "lodash.debounce";
import setMessageContent from "./setMessageContent";

const DEBOUNCE_TIME = 1000;

let debounceRef;

export default async function debouncedSetMessageContent(context, { message, content }) {
    debounceRef?.cancel();
    return new Promise(resolve => {
        (debounceRef = debounce(() => resolve(setMessageContent(context, { message, content })), DEBOUNCE_TIME)),
            debounceRef();
    });
}
