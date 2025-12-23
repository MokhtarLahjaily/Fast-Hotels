// Initialize Bootstrap components
document.addEventListener("DOMContentLoaded", () => {
    // Enable Bootstrap dropdowns
    const dropdownElementList = Array.from(document.querySelectorAll(".dropdown-toggle"))
    const dropdownList = dropdownElementList.map((dropdownToggleEl) => new bootstrap.Dropdown(dropdownToggleEl))

    // Enable tooltips if any
    const tooltipTriggerList = Array.from(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
    const tooltipList = tooltipTriggerList.map((tooltipTriggerEl) => new bootstrap.Tooltip(tooltipTriggerEl))

    // Enable popovers if any
    const popoverTriggerList = Array.from(document.querySelectorAll('[data-bs-toggle="popover"]'))
    const popoverList = popoverTriggerList.map((popoverTriggerEl) => new bootstrap.Popover(popoverTriggerEl))

    console.log("Bootstrap components initialized")
})
