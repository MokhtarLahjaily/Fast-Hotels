document.addEventListener("DOMContentLoaded", () => {
    // Chat elements
    const chatFab = document.getElementById("chat-fab")
    const chatContainer = document.getElementById("chat-container")
    const closeChat = document.getElementById("close-chat")
    const chatMessages = document.getElementById("chat-messages")
    const chatInput = document.getElementById("chat-input")
    const sendButton = document.getElementById("send-button")

    let sessionId = null
    let isTyping = false

    // Open chat when FAB is clicked
    chatFab.addEventListener("click", () => {
        chatContainer.classList.add("active")
        chatInput.focus()

        // If this is the first time opening, show welcome message
        if (chatMessages.children.length === 0) {
            addWelcomeMessage()
        }
    })

    // Close chat when close button is clicked
    closeChat.addEventListener("click", () => {
        chatContainer.classList.remove("active")
    })

    // Send message when send button is clicked or Enter is pressed
    sendButton.addEventListener("click", sendMessage)
    chatInput.addEventListener("keypress", (e) => {
        if (e.key === "Enter") {
            sendMessage()
        }
    })

    // Enable/disable send button based on input
    chatInput.addEventListener("input", () => {
        sendButton.disabled = chatInput.value.trim() === ""
    })

    // Function to add welcome message
    function addWelcomeMessage() {
        const welcomeHtml = `
            <div class="welcome-message">
                <h5>👋 Welcome to Hotel Assistant!</h5>
                <p>I can help you find hotels, check availability, and answer questions about our services.</p>
                <div class="example-queries">
                    Try asking:
                    <ul>
                        <li>"Show me hotels in Paris"</li>
                        <li>"What amenities do you offer?"</li>
                        <li>"Find luxury hotels with a pool"</li>
                    </ul>
                </div>
                <div class="suggestion-chips">
                    <div class="suggestion-chip" onclick="selectSuggestion('Show me featured hotels')">Featured Hotels</div>
                    <div class="suggestion-chip" onclick="selectSuggestion('How do I make a booking?')">How to Book</div>
                    <div class="suggestion-chip" onclick="selectSuggestion('What payment methods do you accept?')">Payment Methods</div>
                </div>
            </div>
        `

        const botMessage = document.createElement("div")
        botMessage.className = "message bot-message"
        botMessage.innerHTML = welcomeHtml
        chatMessages.appendChild(botMessage)
        scrollToBottom()
    }

    // Function to send message
    function sendMessage() {
        const message = chatInput.value.trim()
        if (message === "") return

        // Add user message to chat
        addUserMessage(message)

        // Clear input
        chatInput.value = ""
        sendButton.disabled = true

        // Show typing indicator
        showTypingIndicator()

        // Send message to server
        fetch("/api/chat/support", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                message: message,
                sessionId: sessionId,
            }),
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Network response was not ok")
                }
                return response.json()
            })
            .then((data) => {
                // Hide typing indicator
                hideTypingIndicator()

                // Save session ID for future requests
                sessionId = data.sessionId

                // Add bot message to chat
                addBotMessage(data.message)

                // Process any hotel data if present
                if (data.hotels && data.hotels.length > 0) {
                    addHotelCards(data.hotels)
                }

                // Add suggestions if present
                if (data.suggestions && data.suggestions.length > 0) {
                    addSuggestions(data.suggestions)
                }
            })
            .catch((error) => {
                console.error("Error:", error)
                hideTypingIndicator()
                addBotMessage("I'm sorry, I'm having trouble connecting to the server. Please try again later.")
            })
    }

    // Function to add user message to chat
    function addUserMessage(message) {
        const userMessage = document.createElement("div")
        userMessage.className = "message user-message"
        userMessage.textContent = message
        chatMessages.appendChild(userMessage)
        scrollToBottom()
    }

    // Function to add bot message to chat
    function addBotMessage(message) {
        const botMessage = document.createElement("div")
        botMessage.className = "message bot-message"

        // Check if message contains HTML
        if (message.includes("<") && message.includes(">")) {
            botMessage.innerHTML = message
        } else {
            // Process URLs and line breaks
            const processedMessage = message
                .replace(/\n/g, "<br>")
                .replace(/(https?:\/\/[^\s]+)/g, '<a href="$1" target="_blank">$1</a>')
            botMessage.innerHTML = processedMessage
        }

        chatMessages.appendChild(botMessage)
        scrollToBottom()
    }

    // Function to add hotel cards to chat
    function addHotelCards(hotels) {
        const container = document.createElement("div")
        container.className = "hotel-cards-container"

        hotels.forEach((hotel) => {
            const card = document.createElement("div")
            card.className = "chat-hotel-card"

            const imageUrl = hotel.imageUrl || "/images/hotel-placeholder.jpg"
            const price = hotel.minPrice ? `$${hotel.minPrice}` : "Price on request"

            card.innerHTML = `
                <img src="${imageUrl}" alt="${hotel.name}" class="chat-hotel-image" onerror="this.src='/images/hotel-placeholder.jpg'">
                <div class="chat-hotel-content">
                    <div class="chat-hotel-title">${hotel.name}</div>
                    <div class="chat-hotel-location">${hotel.city}, ${hotel.country}</div>
                    <div class="chat-hotel-price">From ${price} per night</div>
                </div>
                <a href="/hotels/${hotel.id}" class="chat-hotel-link">View Details</a>
            `

            container.appendChild(card)
        })

        chatMessages.appendChild(container)
        scrollToBottom()
    }

    // Function to add suggestions
    function addSuggestions(suggestions) {
        const container = document.createElement("div")
        container.className = "suggestion-chips"

        suggestions.forEach((suggestion) => {
            const chip = document.createElement("div")
            chip.className = "suggestion-chip"
            chip.textContent = suggestion
            chip.onclick = () => {
                selectSuggestion(suggestion)
            }

            container.appendChild(chip)
        })

        chatMessages.appendChild(container)
        scrollToBottom()
    }

    // Function to select a suggestion
    function selectSuggestion(suggestion) {
        chatInput.value = suggestion
        sendMessage()
    }

    // Function to show typing indicator
    function showTypingIndicator() {
        if (isTyping) return

        isTyping = true
        const typingIndicator = document.createElement("div")
        typingIndicator.className = "typing-indicator"
        typingIndicator.id = "typing-indicator"

        for (let i = 0; i < 3; i++) {
            const dot = document.createElement("div")
            dot.className = "typing-dot"
            typingIndicator.appendChild(dot)
        }

        chatMessages.appendChild(typingIndicator)
        scrollToBottom()
    }

    // Function to hide typing indicator
    function hideTypingIndicator() {
        const typingIndicator = document.getElementById("typing-indicator")
        if (typingIndicator) {
            typingIndicator.remove()
        }
        isTyping = false
    }

    // Function to scroll to bottom of chat
    function scrollToBottom() {
        chatMessages.scrollTop = chatMessages.scrollHeight
    }
})
