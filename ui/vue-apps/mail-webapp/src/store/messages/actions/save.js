import { scheduleAction, Actions } from "./draftActionsScheduler";
import { isReadyToBeSaved, save as doSave } from "./saveHelper";

export async function debouncedSave(context, { draft }) {
    return scheduleAction(() => save(context, draft), Actions.SAVE);
}

export async function saveAsap(context, { draft }) {
    return scheduleAction(() => save(context, draft), Actions.SAVE, true);
}

async function waitUntilReady(draft) {
    if (!isReadyToBeSaved(draft)) {
        await new Promise(resolve => {
            setTimeout(resolve, 500);
        });
        return waitUntilReady(draft);
    }
    return Promise.resolve();
}

export async function save(context, draft) {
    if (context.state[draft.key]) {
        await waitUntilReady(draft);
        return doSave(context, draft);
    }
}
