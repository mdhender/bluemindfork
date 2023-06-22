declare const console: Console;
type Logger = Pick<Console, "log" | "warn" | "error">;
type LoggingFn = typeof console.log;

const methods: { [key in keyof Logger]: string } = {
    log: "#00acac",
    warn: "#ffbc0c",
    error: "#ff5c5c"
};
const styles = (method: keyof Logger) => {
    return [
        `background: ${methods[method]}`,
        `border-radius: 0.5em`,
        `color: white`,
        `font-weight: bold`,
        `padding: 2px 0.5em`
    ];
};
const prefix = "%cBM/ServiceWorker";

function print(method: keyof Logger): LoggingFn {
    return function (args: Parameters<LoggingFn>) {
        console[method](...[prefix, styles(method).join(";")], ...args);
    };
}

Object.keys(methods);
const logger: Logger = (Object.keys(methods) as (keyof Logger)[]).reduce((acc, method) => {
    return {
        ...acc,
        [method]: print(method)
    };
}, {} as Logger);

export default logger;
export { logger };
