// Initialize Bootstrap components
document.addEventListener("DOMContentLoaded", () => {
    // Enable Bootstrap dropdowns
    var dropdownElementList = [].slice.call(document.querySelectorAll(".dropdown-toggle"))
    var dropdownList = dropdownElementList.map((dropdownToggleEl) => new bootstrap.Dropdown(dropdownToggleEl))

    // Enable tooltips if any
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
    var tooltipList = tooltipTriggerList.map((tooltipTriggerEl) => new bootstrap.Tooltip(tooltipTriggerEl))

    // Enable popovers if any
    var popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'))
    var popoverList = popoverTriggerList.map((popoverTriggerEl) => new bootstrap.Popover(popoverTriggerEl))

    console.log("Bootstrap components initialized")
})
