export default {
    install(socket, { client }) {
        /* 
        Todo : Container change listener : 
        Peut être trop générique comme ça. A mettre dans un ContainerChangeObserver ?
        const listeners = [];

        socket.addEventListener("send", event => {
            const request = event.data;
            let index = this.listeners.indexOf(request.path);
            if (request.method == "register" && index < 0) {
                this.listeners.push(request.path);
            } else if (request.methid == "unregister" && index >= 0) {
                this.listeners.splice(index, 1);
            }
        });
        socket.addEventListener("message", event => {
            const response = event.data;
            if (response.path && response.statusCode != 200 && this.listeners.indexOf(response.path) >= 0) {
                this.listeners.splice(this.listeners.indexOf(response.path), 1);
            } else if (this.listeners.indexOf(response.requestId) >= 0) {
                this.client.$emit(response.requestId, response);
            }
        });
        */
    }
};
