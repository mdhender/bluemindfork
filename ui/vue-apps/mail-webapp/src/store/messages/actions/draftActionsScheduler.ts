import debounce from "lodash.debounce";

const DEBOUNCE_TIME = 1000;

let resolve: (value?: unknown) => void | undefined;
let reject: (value?: unknown) => void | undefined;
const registeredCallback = new Map();
let promise: Promise<unknown> | undefined;

const waitingRoom = debounce(() => resolve && resolve(), DEBOUNCE_TIME);

function executeActions(immediate = false) {
    if (!isInProgress()) {
        promise = new Promise((res, rej) => {
            resolve = res;
            reject = rej;
        })
            .then(execute.bind(null, Actions.SET_CONTENT))
            .then(execute.bind(null, Actions.SAVE))
            .then(execute.bind(null, Actions.SEND))
            .then(clear)
            .catch(err => {
                promise = undefined;
                if (err !== "CANCEL") {
                    throw err;
                }
            });
    }
    waitingRoom();
    if (immediate) {
        waitingRoom.flush();
    }
    return promise;
}
export enum Actions {
    SET_CONTENT,
    SAVE,
    SEND
}

function execute(type: Actions, ...args: unknown[]) {
    if (registeredCallback.has(type)) {
        const callback = registeredCallback.get(type);
        registeredCallback.delete(type);
        return callback(...args);
    } else {
        return Promise.resolve();
    }
}

export async function scheduleAction(callback: (args: unknown) => Promise<unknown>, type: Actions, immediate = false) {
    registeredCallback.set(type, callback);
    return executeActions(immediate);
}

async function clear(result?: unknown) {
    promise = undefined;
    if (registeredCallback.size > 0) {
        executeActions(true);
    }
    return result;
}

export async function cancelSchedulerActions(checkCondition: () => boolean = () => true) {
    if (isInProgress() && checkCondition()) {
        registeredCallback.clear();
        waitingRoom.cancel();
        reject("CANCEL");
    }
    return;
}

function isInProgress() {
    return !!promise;
}
