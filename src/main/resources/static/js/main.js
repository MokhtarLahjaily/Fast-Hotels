/**
 * Main JavaScript file for the Hotel Reservation System
 */

document.addEventListener("DOMContentLoaded", () => {
    console.log("Document loaded and ready")

    // Initialize Bootstrap components
    initializeBootstrapComponents()

    // Initialize lazy loading for images
    initializeLazyLoading()

    // Handle image loading errors
    handleImageErrors()

    // Add loading state to forms
    setupFormLoadingState()

    // Smooth scrolling for anchor links
    setupSmoothScrolling()

    // Back to top button
    setupBackToTopButton()

    // Mobile detection for optimizations
    detectMobileDevice()
})

/**
 * Initialize Bootstrap components
 */
function initializeBootstrapComponents() {
    // Initialize tooltips if Bootstrap is loaded
    if (typeof bootstrap !== "undefined") {
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
        tooltipTriggerList.forEach((tooltipTriggerEl) => {
            new bootstrap.Tooltip(tooltipTriggerEl)
        })

        // Initialize popovers
        const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'))
        popoverTriggerList.forEach((popoverTriggerEl) => {
            new bootstrap.Popover(popoverTriggerEl)
        })
    } else {
        console.warn("Bootstrap JS is not loaded. Some components may not work properly.")
    }
}

/**
 * Initialize lazy loading for images
 */
function initializeLazyLoading() {
    const lazyImages = document.querySelectorAll(".lazy-load")

    if ("IntersectionObserver" in window) {
        const imageObserver = new IntersectionObserver((entries, observer) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    const img = entry.target
                    if (img.dataset.src) {
                        img.src = img.dataset.src
                        img.classList.add("loaded")
                        imageObserver.unobserve(img)
                    }
                }
            })
        })

        lazyImages.forEach((img) => {
            imageObserver.observe(img)
        })
    } else {
        // Fallback for browsers without IntersectionObserver support
        lazyImages.forEach((img) => {
            if (img.dataset.src) {
                img.src = img.dataset.src
                img.classList.add("loaded")
            }
        })
    }
}

/**
 * Handle image loading errors
 */
function handleImageErrors() {
    document.querySelectorAll("img").forEach((img) => {
        img.addEventListener("error", function () {
            // Check if this is already the fallback image to prevent infinite loop
            if (!this.src.includes("hotel-placeholder.jpg") && !this.hasAttribute("data-error-handled")) {
                console.log("Image failed to load, using placeholder: ", this.src)
                this.src = "/images/hotel-placeholder.jpg"
                this.setAttribute("data-error-handled", "true")
                this.alt = "Image not available"
            }
        })
    })
}

/**
 * Add loading state to forms
 */
function setupFormLoadingState() {
    document.querySelectorAll("form").forEach((form) => {
        form.addEventListener("submit", function (event) {
            // Skip if form is invalid
            if (!this.checkValidity()) {
                event.preventDefault()
                return
            }

            const submitButton = this.querySelector('button[type="submit"]')
            if (submitButton && !submitButton.classList.contains("no-loading-state")) {
                // Save original button content
                if (!submitButton.hasAttribute("data-original-content")) {
                    submitButton.setAttribute("data-original-content", submitButton.innerHTML)
                }

                // Set loading state
                submitButton.disabled = true
                submitButton.innerHTML =
                    '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Loading...'

                // Show loading overlay if needed
                if (this.classList.contains("show-overlay")) {
                    showSpinner()
                }
            }
        })
    })
}

/**
 * Setup smooth scrolling for anchor links
 */
function setupSmoothScrolling() {
    document.querySelectorAll('a[href^="#"]').forEach((anchor) => {
        anchor.addEventListener("click", function (e) {
            const targetId = this.getAttribute("href")

            // Skip if it's just "#" or empty
            if (targetId === "#" || !targetId) return

            const targetElement = document.querySelector(targetId)
            if (targetElement) {
                e.preventDefault()
                window.scrollTo({
                    top: targetElement.offsetTop - 70, // Adjust for header height
                    behavior: "smooth",
                })
            }
        })
    })
}

/**
 * Setup back to top button
 */
function setupBackToTopButton() {
    const backToTopButton = document.getElementById("back-to-top")
    if (backToTopButton) {
        window.addEventListener("scroll", () => {
            if (window.pageYOffset > 300) {
                backToTopButton.classList.add("show")
            } else {
                backToTopButton.classList.remove("show")
            }
        })

        backToTopButton.addEventListener("click", () => {
            window.scrollTo({
                top: 0,
                behavior: "smooth",
            })
        })
    }
}

/**
 * Detect mobile device and add class to body
 */
function detectMobileDevice() {
    const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
    if (isMobile) {
        document.body.classList.add("mobile-device")
    }
}

/**
 * Format a date as YYYY-MM-DD
 * @param {Date} date - The date to format
 * @returns {string} The formatted date string
 */
function formatDate(date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, "0")
    const day = String(date.getDate()).padStart(2, "0")
    return `${year}-${month}-${day}`
}

/**
 * Format a price with currency symbol
 * @param {number} price - The price to format
 * @param {string} currency - The currency symbol (default: $)
 * @returns {string} The formatted price string
 */
function formatPrice(price, currency = "$") {
    return `${currency}${Number.parseFloat(price).toFixed(2)}`
}

/**
 * Show a toast notification
 * @param {string} message - The message to display
 * @param {string} type - The type of toast (success, error, warning, info)
 */
function showToast(message, type = "info") {
    // Check if Bootstrap is available
    if (typeof bootstrap === "undefined") {
        console.warn("Bootstrap JS is not loaded. Toast notifications won't work.")
        alert(message)
        return
    }

    let toastContainer = document.getElementById("toast-container")
    if (!toastContainer) {
        // Create toast container if it doesn't exist
        const container = document.createElement("div")
        container.id = "toast-container"
        container.className = "position-fixed bottom-0 end-0 p-3"
        container.style.zIndex = "1050"
        document.body.appendChild(container)
        toastContainer = container
    }

    const toastId = "toast-" + Date.now()
    const bgClass = type === "error" ? "bg-danger" : `bg-${type}`

    const toastHTML = `
        <div id="${toastId}" class="toast align-items-center text-white ${bgClass}" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `

    toastContainer.insertAdjacentHTML("beforeend", toastHTML)

    const toastElement = document.getElementById(toastId)
    const toast = new bootstrap.Toast(toastElement, {
        autohide: true,
        delay: 5000,
    })

    toast.show()
}

/**
 * Show loading spinner
 */
function showSpinner() {
    // Create spinner element if it doesn't exist
    if (!document.getElementById("loadingSpinner")) {
        const spinnerHtml = `
            <div id="loadingSpinner" class="spinner-overlay">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
        `
        document.body.insertAdjacentHTML("beforeend", spinnerHtml)
    } else {
        document.getElementById("loadingSpinner").style.display = "flex"
    }
}

/**
 * Hide loading spinner
 */
function hideSpinner() {
    const spinner = document.getElementById("loadingSpinner")
    if (spinner) {
        spinner.style.display = "none"
    }
}

/**
 * Setup date validation for check-in/check-out inputs
 * @param {string} checkInId - The ID of the check-in date input
 * @param {string} checkOutId - The ID of the check-out date input
 */
function setupDateValidation(checkInId, checkOutId) {
    const checkIn = document.getElementById(checkInId)
    const checkOut = document.getElementById(checkOutId)

    if (checkIn && checkOut) {
        // Set default dates
        const today = new Date()
        const tomorrow = new Date(today)
        tomorrow.setDate(tomorrow.getDate() + 1)

        checkIn.value = formatDate(today)
        checkOut.value = formatDate(tomorrow)

        // Set minimum dates
        checkIn.min = formatDate(today)
        checkOut.min = formatDate(tomorrow)

        // Ensure check-out is always after check-in
        checkIn.addEventListener("change", function () {
            const newCheckIn = new Date(this.value)
            const currentCheckOut = new Date(checkOut.value)

            if (newCheckIn >= currentCheckOut) {
                const newCheckOut = new Date(newCheckIn)
                newCheckOut.setDate(newCheckOut.getDate() + 1)
                checkOut.value = formatDate(newCheckOut)
            }

            // Update minimum date for checkout
            const minCheckOut = new Date(newCheckIn)
            minCheckOut.setDate(minCheckOut.getDate() + 1)
            checkOut.min = formatDate(minCheckOut)
        })
    }
}

/**
 * Calculate the number of nights between two dates
 * @param {string|Date} checkIn - Check-in date
 * @param {string|Date} checkOut - Check-out date
 * @returns {number} Number of nights
 */
function calculateNights(checkIn, checkOut) {
    const oneDay = 24 * 60 * 60 * 1000 // hours*minutes*seconds*milliseconds
    const firstDate = new Date(checkIn)
    const secondDate = new Date(checkOut)

    return Math.round(Math.abs((firstDate - secondDate) / oneDay))
}

/**
 * Validate a form
 * @param {HTMLFormElement} form - The form to validate
 * @returns {boolean} Whether the form is valid
 */
function validateForm(form) {
    if (!form.checkValidity()) {
        form.classList.add("was-validated")
        return false
    }
    return true
}

// Handle image loading errors
document.addEventListener("DOMContentLoaded", () => {
    // Set default image for all images that fail to load
    document.querySelectorAll("img").forEach((img) => {
        img.addEventListener("error", function () {
            // Check if this is a hotel image
            if (this.src.includes("hotel") || this.classList.contains("card-img-top")) {
                this.src = "/images/hotel-placeholder.jpg"
            }
            // Check if this is a room image
            else if (this.src.includes("room")) {
                this.src = "/images/room-placeholder.jpg"
            }
            // Default fallback
            else {
                this.src = "/images/placeholder.jpg"
            }
        })
    })
})
