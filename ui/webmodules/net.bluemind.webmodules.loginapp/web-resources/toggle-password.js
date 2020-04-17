(function() {
  document
    .querySelectorAll(".toggle-password")
    .forEach(addPasswordVisibilityBehavior);
})();

function addPasswordVisibilityBehavior(toggleBlock) {
  var toggle = toggleBlock.querySelector("i");
  var passwordField = toggleBlock.querySelector("input");
  var timeout;
  
  toggle.addEventListener("click", function(event) {
    timeout && clearTimeout(timeout);
    if (isVisible(passwordField)) {
      hidePassword(passwordField, toggle);
    } else {
      showPassword(passwordField, toggle);
      timeout = setTimeout(function() {
        hidePassword(passwordField, toggle);
      }, 2000);
    }
  });
  
  function isVisible(input) {
    return input.type === "text";
  }
  
  function showPassword(input, toggle) {
    input.type = "text";
    toggle.classList.add("fa-eye-slash");
    toggle.classList.remove("fa-eye");
  }
  
  function hidePassword(input, toggle) {
    input.type = "password";
    toggle.classList.add("fa-eye");
    toggle.classList.remove("fa-eye-slash");
  }
}