# GuruAI Frontend

React 19 + TypeScript + Vite + Tailwind CSS 4. Talks exclusively to the
API gateway (`VITE_API_BASE_URL`, defaults to `http://localhost:8080`) —
never directly to a downstream service.

## Setup

```bash
npm install
cp .env.example .env.local   # adjust if the gateway isn't on localhost:8080
npm run dev                   # http://localhost:3000
```

## Structure

```
src/
├── lib/          API client (axios + JWT refresh interceptor), per-domain API calls
├── context/      AuthContext — session state, login/register/logout
├── components/   Shared UI (Layout, ProtectedRoute, ...)
├── pages/        Route-level screens
└── types/        TypeScript types mirroring backend DTOs (ApiResponse<T>, etc.)
```

## Status

Stage 1 (this commit): scaffold, routing, API client, auth context — auth
pages are placeholders. See the main repo README's Status section for the
rest of the build plan.
