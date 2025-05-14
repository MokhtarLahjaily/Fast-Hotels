/**
 * Global image error handler
 * This script adds error handling to all images on the page
 * If an image fails to load, it will be replaced with a placeholder image
 */
document.addEventListener("DOMContentLoaded", () => {
    // Add error handling for all images
    function setupImageErrorHandling() {
        document.querySelectorAll("img").forEach((img) => {
            img.addEventListener("error", function () {
                // Skip if already using a placeholder
                if (this.src.indexOf("hotel-placeholder.jpg") === -1 && this.src.indexOf("room-placeholder.jpg") === -1) {
                    // Choose appropriate placeholder based on alt text or context
                    if (this.alt && this.alt.toLowerCase().includes("room")) {
                        this.src = "/images/room-placeholder.jpg"
                    } else {
                        this.src = "/images/hotel-placeholder.jpg"
                    }

                    console.log("Image replaced with placeholder:", this.alt || "unnamed image")
                }
            })
        })
    }

    // Run immediately
    setupImageErrorHandling()

    // Also run when content changes (for dynamically loaded content)
    const observer = new MutationObserver((mutations) => {
        setupImageErrorHandling()
    })

    observer.observe(document.body, {
        childList: true,
        subtree: true,
    })
})
