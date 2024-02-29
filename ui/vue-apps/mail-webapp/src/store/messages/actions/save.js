import debounce from "lodash/debounce";

import { isReadyToBeSaved, save as doSave } from "./saveHelper";

let debounceRef;

const DEBOUNCE_TIME = 3000;
export async function debouncedSave(context, { draft }) {
    cancelDebounce();
    return new Promise(resolve => {
        debounceRef = debounce(() => resolve(saveOrDebounce(context, draft)), DEBOUNCE_TIME);
        debounceRef();
    });
}

export async function saveAsap(context, { draft }) {
    await waitUntilReady(draft);
    return save(context, draft);
}

function cancelDebounce() {
    if (debounceRef) {
        debounceRef.cancel();
    }
}

function saveOrDebounce(context, draft) {
    if (!isReadyToBeSaved(draft)) {
        return debouncedSave(context, { draft });
    }
    return save(context, draft);
}

async function waitUntilReady(draft) {
    cancelDebounce();
    if (!isReadyToBeSaved(draft)) {
        await new Promise(resolve => {
            setTimeout(resolve, 500);
        });
        return waitUntilReady(draft);
    }
    return Promise.resolve();
}

function save(context, draft) {
    return doSave(context, draft);
}
