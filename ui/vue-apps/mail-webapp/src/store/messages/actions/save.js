import debounce from "lodash/debounce";

import { isReadyToBeSaved, save } from "./saveHelper";

let debounceRef;

const DEBOUNCE_TIME = 3000;
export async function debouncedSave(context, { draft, messageCompose }) {
    cancelDebounce();
    return new Promise(resolve => {
        debounceRef = debounce(() => resolve(saveOrDebounce(context, draft, messageCompose)), DEBOUNCE_TIME);
        debounceRef();
    });
}

export async function saveAsap(context, { draft, messageCompose, files }) {
    await waitUntilReady(draft, files);
    return save(draft, messageCompose, files);
}

function cancelDebounce() {
    if (debounceRef) {
        debounceRef.cancel();
    }
}

function saveOrDebounce(context, draft, messageCompose) {
    if (context.state[draft.key]) {
        if (!isReadyToBeSaved(draft)) {
            return debouncedSave({ draft, messageCompose });
        }
        return save(draft, messageCompose);
    }
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
