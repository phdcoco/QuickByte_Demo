const state = {
    token: localStorage.getItem("quickbite.token") || "",
    user: JSON.parse(localStorage.getItem("quickbite.user") || "null"),
    restaurants: [],
    lastOrder: null,
    lastPayment: null,
    eventSource: null
};

const $ = (id) => document.getElementById(id);

function authHeaders() {
    return state.token ? { Authorization: `Bearer ${state.token}` } : {};
}

async function api(path, options = {}) {
    const response = await fetch(path, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...authHeaders(),
            ...(options.headers || {})
        }
    });
    const text = await response.text();
    const body = text ? JSON.parse(text) : null;
    if (!response.ok) {
        const message = body?.message || `${response.status} ${response.statusText}`;
        throw new Error(message);
    }
    return body;
}

function saveSession(payload) {
    state.token = payload.accessToken;
    state.user = payload.user;
    localStorage.setItem("quickbite.token", state.token);
    localStorage.setItem("quickbite.user", JSON.stringify(state.user));
    renderSession();
}

function renderSession() {
    $("sessionLabel").textContent = state.user
        ? `${state.user.displayName} · ${state.user.role}`
        : "로그인 전";
}

function showToast(message) {
    const toast = $("toast");
    toast.textContent = message;
    toast.classList.add("show");
    window.setTimeout(() => toast.classList.remove("show"), 2600);
}

function logEvent(message, payload) {
    const target = $("eventLog");
    const stamp = new Date().toLocaleTimeString();
    const line = payload ? `${stamp} ${message}\n${JSON.stringify(payload, null, 2)}` : `${stamp} ${message}`;
    target.textContent = target.textContent === "이벤트 로그 대기 중..." ? line : `${line}\n\n${target.textContent}`;
}

function currentAuthMode() {
    return document.querySelector(".tab.active")?.dataset.authMode || "customer";
}

function applyAuthMode(mode) {
    document.querySelectorAll(".tab").forEach((tab) => tab.classList.toggle("active", tab.dataset.authMode === mode));
    const isAdmin = mode === "admin";
    $("email").value = isAdmin ? "admin@quickbite.local" : "ryu@example.com";
    $("password").value = isAdmin ? "quickbite-admin-local" : "delivery-demo-123";
    $("displayNameWrap").style.display = isAdmin ? "none" : "grid";
    $("signupButton").style.display = isAdmin ? "none" : "inline-block";
}

async function signup(event) {
    event.preventDefault();
    try {
        const payload = await api("/api/auth/signup", {
            method: "POST",
            body: JSON.stringify({
                email: $("email").value,
                displayName: $("displayName").value,
                password: $("password").value
            })
        });
        saveSession(payload);
        showToast("가입과 로그인이 완료됐습니다.");
    } catch (error) {
        showToast(error.message);
    }
}

async function login() {
    try {
        const payload = await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({
                email: $("email").value,
                password: $("password").value
            })
        });
        saveSession(payload);
        showToast(`${currentAuthMode() === "admin" ? "관리자" : "고객"} 로그인 완료`);
    } catch (error) {
        showToast(error.message);
    }
}

async function loadRestaurants() {
    try {
        state.restaurants = await api("/api/restaurants", { headers: {} });
        renderRestaurants();
        renderOrderOptions();
    } catch (error) {
        showToast(error.message);
    }
}

function renderRestaurants() {
    const root = $("restaurants");
    if (!state.restaurants.length) {
        root.innerHTML = `<div class="result-block muted">등록된 식당이 없습니다. 관리자로 로그인 후 샘플 식당을 생성하세요.</div>`;
        return;
    }
    root.innerHTML = state.restaurants.map((restaurant) => `
        <article class="restaurant-card">
            <h3>${escapeHtml(restaurant.name)}</h3>
            <p>${escapeHtml(restaurant.address)} · 배달비 ${restaurant.deliveryFee}</p>
            <ul>
                ${(restaurant.menu || []).map((item) => `<li>${escapeHtml(item.name)} · ${item.price}</li>`).join("") || "<li>메뉴 없음</li>"}
            </ul>
        </article>
    `).join("");
}

function renderOrderOptions() {
    const restaurantSelect = $("restaurantSelect");
    restaurantSelect.innerHTML = state.restaurants.map((restaurant) => (
        `<option value="${restaurant.id}">${escapeHtml(restaurant.name)}</option>`
    )).join("");
    renderMenuOptions();
}

function renderMenuOptions() {
    const restaurantId = Number($("restaurantSelect").value);
    const restaurant = state.restaurants.find((candidate) => candidate.id === restaurantId);
    $("menuSelect").innerHTML = (restaurant?.menu || []).map((item) => (
        `<option value="${item.id}">${escapeHtml(item.name)} · ${item.price}</option>`
    )).join("");
}

async function seedRestaurant() {
    try {
        const suffix = Math.floor(Math.random() * 900 + 100);
        const restaurant = await api("/api/admin/restaurants", {
            method: "POST",
            body: JSON.stringify({
                name: `Seoul Night Noodles ${suffix}`,
                address: "12 Mapo-ro, Seoul",
                deliveryFee: 3500
            })
        });
        await api(`/api/admin/restaurants/${restaurant.id}/menu-items`, {
            method: "POST",
            body: JSON.stringify({
                name: "Spicy Beef Udon",
                description: "broth, brisket, scallion",
                price: 12900,
                available: true
            })
        });
        await api(`/api/admin/restaurants/${restaurant.id}/menu-items`, {
            method: "POST",
            body: JSON.stringify({
                name: "Crispy Mandu Set",
                description: "pan-fried dumplings, soy dip",
                price: 8900,
                available: true
            })
        });
        await loadRestaurants();
        showToast("샘플 식당과 메뉴를 만들었습니다.");
    } catch (error) {
        showToast(`관리자 권한이 필요합니다: ${error.message}`);
    }
}

async function placeOrder(event) {
    event.preventDefault();
    try {
        const payload = await api("/api/orders", {
            method: "POST",
            body: JSON.stringify({
                restaurantId: Number($("restaurantSelect").value),
                deliveryAddress: $("deliveryAddress").value,
                customerNote: $("customerNote").value,
                items: [{
                    menuItemId: Number($("menuSelect").value),
                    quantity: Number($("quantity").value)
                }]
            })
        });
        state.lastOrder = payload.order;
        state.lastPayment = payload.payment;
        $("orderResult").classList.remove("muted");
        $("orderResult").innerHTML = `<strong>주문 #${payload.order.id}</strong><br>상태: ${payload.order.status}<br>결제 요청 #${payload.payment.id}<br>총액: ${payload.order.total}`;
        logEvent("주문 생성", payload);
    } catch (error) {
        showToast(error.message);
    }
}

async function authorizePayment() {
    if (!state.lastPayment) {
        showToast("먼저 주문을 생성하세요.");
        return;
    }
    try {
        const payload = await api(`/api/payments/${state.lastPayment.id}/authorize`, {
            method: "POST",
            body: JSON.stringify({
                providerReference: `frontend-sandbox-${Date.now()}`
            })
        });
        state.lastPayment = payload;
        logEvent("결제 승인", payload);
        showToast("결제를 승인했고 Redis 큐에 주문이 들어갔습니다.");
    } catch (error) {
        showToast(error.message);
    }
}

function openEvents() {
    if (!state.lastOrder) {
        showToast("먼저 주문을 생성하세요.");
        return;
    }
    if (state.eventSource) {
        state.eventSource.close();
    }
    const url = `/api/orders/${state.lastOrder.id}/events?access_token=${encodeURIComponent(state.token)}`;
    state.eventSource = new EventSource(url);
    state.eventSource.addEventListener("order-status", (event) => {
        logEvent("SSE 주문 상태", JSON.parse(event.data));
    });
    state.eventSource.onerror = () => {
        logEvent("SSE 연결 오류 또는 재시도 중", { orderId: state.lastOrder.id });
    };
    showToast("주문 상태 스트림을 연결했습니다.");
}

async function loadDelivery() {
    if (!state.lastOrder) {
        showToast("먼저 주문을 생성하세요.");
        return;
    }
    try {
        const payload = await api(`/api/deliveries/orders/${state.lastOrder.id}`);
        logEvent("배달 추적 조회", payload);
    } catch (error) {
        showToast(error.message);
    }
}

function logout() {
    state.token = "";
    state.user = null;
    state.lastOrder = null;
    state.lastPayment = null;
    localStorage.removeItem("quickbite.token");
    localStorage.removeItem("quickbite.user");
    renderSession();
    showToast("로그아웃했습니다.");
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

document.querySelectorAll(".tab").forEach((tab) => {
    tab.addEventListener("click", () => applyAuthMode(tab.dataset.authMode));
});
$("authForm").addEventListener("submit", signup);
$("loginButton").addEventListener("click", login);
$("logoutButton").addEventListener("click", logout);
$("refreshRestaurants").addEventListener("click", loadRestaurants);
$("seedRestaurant").addEventListener("click", seedRestaurant);
$("restaurantSelect").addEventListener("change", renderMenuOptions);
$("orderForm").addEventListener("submit", placeOrder);
$("authorizePayment").addEventListener("click", authorizePayment);
$("openEvents").addEventListener("click", openEvents);
$("loadDelivery").addEventListener("click", loadDelivery);

renderSession();
applyAuthMode("customer");
loadRestaurants();
