export const move = {
    id: "@bluemind/move",
    install(scope) {
        scope.Interactable.prototype.move = function () {
            return this.draggable({}).on("dragmove", event => moveFn(event));
        };
    }
};

function moveFn(event) {
    const target = event.target;
    const x = (parseFloat(target.getAttribute("data-x")) || 0) + event.dx;
    const y = (parseFloat(target.getAttribute("data-y")) || 0) + event.dy;
    target.style.transform = "translate(" + x + "px, " + y + "px)";

    target.setAttribute("data-x", x);
    target.setAttribute("data-y", y);
}
