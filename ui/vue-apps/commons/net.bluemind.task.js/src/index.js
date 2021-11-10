/**
 * Wait for the task to be finished or a timeout is reached.
 */
export function retrieveTaskResult(taskService, inProgressFn, iteration = 1) {
    const delayTime = 500,
        maxTries = 60;
    return new Promise(resolve => setTimeout(() => resolve(taskService.status()), delayTime)).then(taskStatus => {
        if (taskStatus.state === "InProgress" && inProgressFn) {
            inProgressFn(taskStatus);
        }
        if (taskStatus.state === "Success") {
            return JSON.parse(taskStatus.result);
        } else if (taskStatus.state === "InError") {
            return Promise.reject(taskStatus);
        } else if (iteration < maxTries) {
            return retrieveTaskResult(taskService, inProgressFn, ++iteration);
        } else {
            return Promise.reject("Timeout while retrieving task result");
        }
    });
}
