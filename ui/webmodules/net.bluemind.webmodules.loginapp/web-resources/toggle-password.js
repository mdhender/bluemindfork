(function() {
  var toggle = document.getElementById("toggle-password");
  var passwordField = document.getElementById(toggle.getAttribute("toggle"));
  var timeout;

  toggle.addEventListener("click", function(event) {
    timeout && clearTimeout(timeout);
    if (isVisible(passwordField)) {
      hidePassword();
    } else {
      showPassword();
      timeout = setTimeout(hidePassword, 2000);
    }
  });

  function isVisible(input) {
    return input.type === "text";
  }

  function showPassword() {
    passwordField.type = "text";
    toggle.classList.add("fa-eye-slash");
    toggle.classList.remove("fa-eye");
  }

  function hidePassword() {
    passwordField.type = "password";
    toggle.classList.add("fa-eye");
    toggle.classList.remove("fa-eye-slash");
  }
})();
