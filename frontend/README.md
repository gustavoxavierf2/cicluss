# Ciclus — frontend

React + TypeScript + Vite + Leaflet.

```bash
npm install
npm run dev     # http://localhost:5173 (precisa do backend na porta 8080)
npm run build   # gera dist/
```

- `src/App.tsx` — layout (topo, seletores, mapa, abas de resultado, chat).
- `src/components/BarraAnos.tsx` — barra única de anos: passado sólido, futuro tracejado e apagando.
- `src/api.ts` — chamadas ao backend. Para apontar para outro servidor: `VITE_API_URL=https://... npm run build`.
- `src/tipos.ts` — tipos espelhando os records do backend.
