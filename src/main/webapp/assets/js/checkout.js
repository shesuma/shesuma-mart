
"use strict";

const CART_KEY = "shesumaMartCart";

function getCart() {
    try {
        const cart = JSON.parse(localStorage.getItem(CART_KEY) || "[]");
        return Array.isArray(cart) ? cart : [];
    } catch (error) {
        return [];
    }
}

function clearCart() {
    localStorage.setItem(CART_KEY, "[]");
}

async function placeDemoOrder() {
    const cart = getCart();

    if (cart.length === 0) {
        alert("Your cart is empty. Add groceries before checkout.");
        return;
    }

    const items = cart.map(item => ({
        name: String(item.name || "").trim(),
        quantity: Number(item.quantity || 1)
    }));

    if (items.some(item =>
        !item.name ||
        !Number.isInteger(item.quantity) ||
        item.quantity < 1 ||
        item.quantity > 1000
    )) {
        alert("Invalid cart items. Please check your cart.");
        return;
    }

    const button = document.getElementById("placeOrderBtn");

    if (button) {
        button.disabled = true;
        button.textContent = "Placing Order...";
    }

    try {
        const response = await fetch(
            "api/v1/orders",
            {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ items: items })
            }
        );

        const result = await response.json();

        if (!response.ok || !result.success) {
            alert(result.message || "Order failed. Please try again.");
            return;
        }

        clearCart();

        alert(
            "Order placed successfully!\n" +
            "Order ID: " + result.orderId +
            "\nTotal: ₹" + result.total
        );

        window.location.href = "orders.html";

    } catch (error) {
        console.error("Checkout error:", error);
        alert("Server connection failed. Please try again.");
    } finally {
        if (button) {
            button.disabled = false;
            button.textContent = "Place Order";
        }
    }
}

