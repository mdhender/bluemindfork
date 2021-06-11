/**
 * Wait for the task to be finished or a timeout is reached.
 */

export function retrieveTaskResult(taskService, delayTime = 500, maxTries = 60, iteration = 1) {
    return new Promise(resolve => setTimeout(() => resolve(taskService.status()), delayTime)).then(taskStatus => {
        const taskEnded =
            taskStatus && taskStatus.state && taskStatus.state !== "InProgress" && taskStatus.state !== "NotStarted";
        if (taskEnded) {
            return JSON.parse(taskStatus.result);
        } else {
            if (iteration < maxTries) {
                return retrieveTaskResult(taskService, delayTime, maxTries, ++iteration);
            } else {
                return Promise.reject("Timeout while retrieving task result");
            }
        }
    });
}
