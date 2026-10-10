# D3 Sequence Diagram Notes

Buyer checkout:
Browser → Checkout Servlet → Order Service → Order DAO → Database → JSON response → Browser

Authentication:
Browser → Auth Servlet → Auth Service → User DAO → Database → HttpSession → Browser
