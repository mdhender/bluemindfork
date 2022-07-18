import debounce from "lodash/debounce";

import { isReadyToBeSaved, save } from "./saveHelper";

let debounceRef;

const DEBOUNCE_TIME = 3000;
export async function debouncedSave(context, { draft, messageCompose, files }) {
    cancelDebounce();
    return new Promise(resolve => {
        debounceRef = debounce(() => resolve(saveOrDebounce(context, draft, messageCompose, files)), DEBOUNCE_TIME);
        debounceRef();
    });
}

export async function saveAsap(context, { draft, messageCompose, files }) {
    await waitUntilReady(draft, files);
    return save(context, draft, messageCompose, files);
}

function cancelDebounce() {
    if (debounceRef) {
        debounceRef.cancel();
    }
}

function saveOrDebounce(context, draft, messageCompose, files) {
    if (context.state[draft.key]) {
        if (!isReadyToBeSaved(draft, files)) {
            return debouncedSave(context, { draft, messageCompose, files });
        }
        return save(context, draft, messageCompose, files);
    }
}

async function waitUntilReady(draft, files) {
    cancelDebounce();
    if (!isReadyToBeSaved(draft, files)) {
        await new Promise(resolve => {
            setTimeout(resolve, 500);
        });
        return waitUntilReady(draft, files);
    }
    return Promise.resolve();
}
