/**
 * JavaScript for handling favorites functionality
 */

/**
 * Get CSRF token from cookie or meta tag
 * @returns {string|null} The CSRF token
 */
function getFavCsrfToken() {
    // Try cookie first (Spring Security CookieCsrfTokenRepository)
    const name = "XSRF-TOKEN="
    const decodedCookie = decodeURIComponent(document.cookie)
    const cookies = decodedCookie.split(";")
    for (let cookie of cookies) {
        cookie = cookie.trim()
        if (cookie.startsWith(name)) {
            return cookie.substring(name.length)
        }
    }
    // Fallback to meta tag
    const metaTag = document.querySelector('meta[name="_csrf"]')
    return metaTag ? metaTag.getAttribute("content") : null
}

document.addEventListener("DOMContentLoaded", () => {
    // Toggle favorite via AJAX
    const favoriteToggleButtons = document.querySelectorAll(".favorite-toggle")

    favoriteToggleButtons.forEach((button) => {
        button.addEventListener("click", function (e) {
            e.preventDefault()

            const hotelId = this.dataset.hotelId
            const icon = this.querySelector("i")
            const csrfToken = getFavCsrfToken()

            const headers = {
                "Content-Type": "application/json",
            }
            if (csrfToken) {
                headers["X-XSRF-TOKEN"] = csrfToken
            }

            fetch(`/api/favorites/${hotelId}`, {
                method: "POST",
                headers: headers,
            })
                .then((response) => {
                    if (!response.ok) {
                        if (response.status === 401) {
                            window.location.href = "/login"
                            return
                        }
                        throw new Error("Network response was not ok")
                    }
                    return response.json()
                })
                .then((data) => {
                    // Update the icon based on the response
                    if (data.isFavorite) {
                        icon.classList.remove("far")
                        icon.classList.add("fas")
                        button.classList.remove("btn-outline-danger")
                        button.classList.add("btn-danger")
                        button.setAttribute("title", "Remove from favorites")

                        // Show toast notification
                        showToast("Hotel added to favorites", "success")
                    } else {
                        icon.classList.remove("fas")
                        icon.classList.add("far")
                        button.classList.remove("btn-danger")
                        button.classList.add("btn-outline-danger")
                        button.setAttribute("title", "Add to favorites")

                        // Show toast notification
                        showToast("Hotel removed from favorites", "info")
                    }
                })
                .catch((error) => {
                    console.error("Error toggling favorite:", error)
                    showToast("Error updating favorites. Please try again.", "error")
                })
        })
    })

    // Check favorite status for all hotels on the page
    const hotelCards = document.querySelectorAll("[data-hotel-id]")

    if (hotelCards.length > 0) {
        hotelCards.forEach((card) => {
            const hotelId = card.dataset.hotelId
            const favoriteButton = card.querySelector(".favorite-toggle")

            if (favoriteButton) {
                fetch(`/api/favorites/status/${hotelId}`)
                    .then((response) => response.json())
                    .then((data) => {
                        const icon = favoriteButton.querySelector("i")

                        if (data.isFavorite) {
                            icon.classList.remove("far")
                            icon.classList.add("fas")
                            favoriteButton.classList.remove("btn-outline-danger")
                            favoriteButton.classList.add("btn-danger")
                            favoriteButton.setAttribute("title", "Remove from favorites")
                        } else {
                            icon.classList.remove("fas")
                            icon.classList.add("far")
                            favoriteButton.classList.remove("btn-danger")
                            favoriteButton.classList.add("btn-outline-danger")
                            favoriteButton.setAttribute("title", "Add to favorites")
                        }
                    })
                    .catch((error) => {
                        console.error("Error checking favorite status:", error)
                    })
            }
        })
    }
})

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
        delay: 3000,
    })

    toast.show()
}
