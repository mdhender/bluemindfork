declare const console: { [key: string]: (...args: any[]) => void };
const methods: { [key: string]: string } = {
    log: "#00acac",
    warn: "#ffbc0c",
    error: "#ff5c5c"
};
const styles = (method: string) => {
    return [
        `background: ${methods[method]}`,
        `border-radius: 0.5em`,
        `color: white`,
        `font-weight: bold`,
        `padding: 2px 0.5em`
    ];
};

function print(method: string) {
    return function (...args: any[]) {
        console[method](...["%cBM/ServiceWorker", styles(method).join(";")], ...args);
    };
}
const logger: any = Object.keys(methods).reduce((acc, method) => {
    return {
        ...acc,
        [method]: print(method)
    };
}, {});

export { logger };
