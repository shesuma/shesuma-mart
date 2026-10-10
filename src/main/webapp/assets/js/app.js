
"use strict";

const SHESUMA_CART_KEY = "shesumaMartCart";
const SHESUMA_ORDERS_KEY = "shesumaMartOrders";
const SHESUMA_SELLER_PRODUCTS_KEY = "shesumaMartSellerProducts";

/* Safely read arrays from localStorage */

function readStorageArray(key) {
    try {
        const value = JSON.parse(localStorage.getItem(key) || "[]");
        return Array.isArray(value) ? value : [];
    } catch (error) {
        console.error("Unable to read stored data:", key, error);
        return [];
    }
}

/* Save arrays to localStorage */

function writeStorageArray(key, value) {
    if (!Array.isArray(value)) {
        console.error("Storage value must be an array.");
        return false;
    }

    try {
        localStorage.setItem(key, JSON.stringify(value));
        return true;
    } catch (error) {
        console.error("Unable to save data:", key, error);
        return false;
    }
}

/* Format Indian Rupees */

function formatRupees(value) {
    const amount = Number(value);

    if (!Number.isFinite(amount)) {
        return "₹0";
    }

    return "₹" + amount.toLocaleString("en-IN", {
        maximumFractionDigits: 2
    });
}

/* Update every cart counter on the current page */

function updateCartCount() {
    const cart = readStorageArray(SHESUMA_CART_KEY);

    const count = cart.reduce((total, item) => {
        const quantity = Number(item.quantity || 1);

        return total + (
            Number.isFinite(quantity) && quantity > 0
                ? Math.floor(quantity)
                : 1
        );
    }, 0);

    document.querySelectorAll("#cartCount, .cart-count").forEach(element => {
        element.textContent = count;
    });

    return count;
}

/* Add a grocery product to the cart */

function addToCart(product) {
    if (!product || !product.name) {
        alert("Product information is missing.");
        return false;
    }

    const price = Number(product.price);

    if (!Number.isFinite(price) || price < 0) {
        alert("Please check the product price.");
        return false;
    }

    const cart = readStorageArray(SHESUMA_CART_KEY);
    const productId = String(product.id ?? product.name);

    const existingProduct = cart.find(item => {
        return String(item.id ?? item.name) === productId;
    });

    if (existingProduct) {
        existingProduct.quantity = Math.max(
            1,
            Number(existingProduct.quantity || 1)
        ) + 1;
    } else {
        cart.push({
            id: product.id ?? product.name,
            name: String(product.name),
            price: price,
            quantity: 1,
            emoji: String(product.emoji || "🛒"),
            unit: String(product.unit || "per item")
        });
    }

    if (!writeStorageArray(SHESUMA_CART_KEY, cart)) {
        alert("Unable to save your cart. Please check browser storage.");
        return false;
    }

    updateCartCount();
    return true;
}

/* Remove a product from the cart */

function removeFromCart(productId) {
    const cart = readStorageArray(SHESUMA_CART_KEY);

    const updatedCart = cart.filter(item => {
        return String(item.id ?? item.name) !== String(productId);
    });

    if (!writeStorageArray(SHESUMA_CART_KEY, updatedCart)) {
        alert("Unable to update your cart.");
        return false;
    }

    updateCartCount();
    return true;
}

/* Change the quantity of a cart product */

function changeCartQuantity(productId, change) {
    const cart = readStorageArray(SHESUMA_CART_KEY);

    const item = cart.find(product => {
        return String(product.id ?? product.name) === String(productId);
    });

    if (!item) {
        return false;
    }

    const nextQuantity = Number(item.quantity || 1) + Number(change);

    if (nextQuantity <= 0) {
        return removeFromCart(productId);
    }

    item.quantity = Math.floor(nextQuantity);

    if (!writeStorageArray(SHESUMA_CART_KEY, cart)) {
        alert("Unable to update quantity.");
        return false;
    }

    updateCartCount();
    return true;
}

/* Calculate cart subtotal */

function getCartSubtotal() {
    return readStorageArray(SHESUMA_CART_KEY).reduce((total, item) => {
        const price = Number(item.price || 0);
        const quantity = Number(item.quantity || 1);

        if (!Number.isFinite(price) || !Number.isFinite(quantity)) {
            return total;
        }

        return total + price * quantity;
    }, 0);
}

/* Create a browser-only demo order */

function placeDemoOrder() {
    const cart = readStorageArray(SHESUMA_CART_KEY);

    if (cart.length === 0) {
        alert("Your cart is empty. Add groceries before checkout.");
        return false;
    }

    const subtotal = getCartSubtotal();
    const delivery = subtotal >= 499 ? 0 : 30;

    const order = {
        id: "SM" + Date.now(),
        date: new Date().toISOString(),
        status: "Placed",
        items: cart.map(item => ({
            id: item.id ?? item.name,
            name: String(item.name || "Grocery Item"),
            price: Number(item.price || 0),
            quantity: Math.max(1, Number(item.quantity || 1)),
            emoji: String(item.emoji || "🛒"),
            unit: String(item.unit || "per item")
        })),
        subtotal: subtotal,
        delivery: delivery,
        total: subtotal + delivery
    };

    const orders = readStorageArray(SHESUMA_ORDERS_KEY);
    orders.unshift(order);

    if (!writeStorageArray(SHESUMA_ORDERS_KEY, orders)) {
        alert("Unable to save the order.");
        return false;
    }

    if (!writeStorageArray(SHESUMA_CART_KEY, [])) {
        alert("Order was saved, but the cart could not be cleared.");
        return false;
    }

    updateCartCount();
    alert("Demo order placed successfully! Order ID: " + order.id);

    window.location.href = "orders.html";
    return true;
}

/* Safely display a message inside a page */

function showAppMessage(selector, message, type) {
    const element = document.querySelector(selector);

    if (!element) {
        return;
    }

    element.textContent = message;
    element.classList.remove("success", "error");

    if (type === "success" || type === "error") {
        element.classList.add(type);
    }
}

/* Demo form handler
   Real login/register authentication must be connected to the Java backend. */

function setupDemoForms() {
    document.querySelectorAll("[data-demo-form]").forEach(form => {
        if (form.dataset.appHandlerAttached === "true") {
            return;
        }

        form.dataset.appHandlerAttached = "true";

        form.addEventListener("submit", event => {
            event.preventDefault();

            const message = form.querySelector(".message");

            if (message) {
                message.textContent =
                    "Demo form submitted. Java backend authentication is not connected yet.";
            }
        });
    });
}

/* Initialize shared frontend features */

document.addEventListener("DOMContentLoaded", () => {
    updateCartCount();
    setupDemoForms();
});

/* Keep the cart counter updated if another browser tab changes the cart */

window.addEventListener("storage", event => {
    if (event.key === SHESUMA_CART_KEY) {
        updateCartCount();
    }
});
