const toggleButton = document.querySelector("#toggle-email");
const emailCells = document.querySelectorAll(".email-column");

if (toggleButton) {
    toggleButton.addEventListener("click", () => {
        const willHide = toggleButton.getAttribute("aria-pressed") === "false";

        emailCells.forEach((cell) => {
            cell.hidden = willHide;
        });
        toggleButton.setAttribute("aria-pressed", String(willHide));
        toggleButton.textContent = willHide ? "显示邮箱" : "隐藏邮箱";
    });
}
