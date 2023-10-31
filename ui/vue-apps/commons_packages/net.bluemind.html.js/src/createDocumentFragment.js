export default function (content = "", rootElement = "div") {
    const fragment = document.createDocumentFragment();
    fragment.appendChild(document.createElement(rootElement));
    fragment.firstElementChild.innerHTML = content;
    return fragment;
}
