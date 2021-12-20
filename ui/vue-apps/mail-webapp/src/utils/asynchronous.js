export function asynchronous(fn) {
    return function () {
        return new Promise(resolve => {
            setTimeout(() => resolve(fn.apply(undefined, arguments)));
        });
    };
}
