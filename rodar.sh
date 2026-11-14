#!/usr/bin/env bash
# Sobe o backend (porta 8080) e o frontend (porta 5173) juntos. Linux / macOS.
# Uso:  ./rodar.sh
# Para ligar a IA do chat, antes de rodar:  export ANTHROPIC_API_KEY="sua-chave"
# Ctrl+C encerra os dois.
set -euo pipefail

raiz="$(cd "$(dirname "$0")" && pwd)"
url="http://localhost:5173"

for cmd in java mvn npm; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Não encontrei '$cmd' no PATH. Instale Java 21+, Maven 3.9+ e Node 20+." >&2
    exit 1
  fi
done

porta_aberta() { (exec 3<>"/dev/tcp/localhost/$1") 2>/dev/null; }

for porta in 8080 5173; do
  if porta_aberta "$porta"; then
    echo "A porta $porta já está em uso. Feche o programa que está usando e rode de novo." >&2
    exit 1
  fi
done

if [ ! -d "$raiz/frontend/node_modules" ]; then
  echo "Instalando dependências do frontend (só na primeira vez)..."
  (cd "$raiz/frontend" && npm install)
fi

if [ -n "${ANTHROPIC_API_KEY:-}" ]; then
  echo "Chat: IA ligada (ANTHROPIC_API_KEY encontrada)."
else
  echo "Chat: modo demonstração (defina ANTHROPIC_API_KEY para ligar a IA)."
fi

echo "Subindo backend e frontend..."
(cd "$raiz/backend" && exec mvn -q spring-boot:run) &
back=$!
front=""

encerrar() {
  trap - INT TERM EXIT
  kill "$back" $front 2>/dev/null || true
  wait "$back" $front 2>/dev/null || true
  echo "Ciclus encerrado."
}
trap encerrar INT TERM EXIT

# O frontend só sobe depois que o backend responde, para a página não abrir sem servidor.
back_pronto=0
for _ in $(seq 1 180); do
  kill -0 "$back" 2>/dev/null || break
  if porta_aberta 8080; then back_pronto=1; break; fi
  sleep 1
done

pronto=0
if [ "$back_pronto" = 1 ]; then
  (cd "$raiz/frontend" && exec npm run dev) &
  front=$!
  for _ in $(seq 1 60); do
    kill -0 "$back" 2>/dev/null && kill -0 "$front" 2>/dev/null || break
    if porta_aberta 5173; then pronto=1; break; fi
    sleep 1
  done
fi

if [ "$pronto" = 1 ]; then
  echo
  echo "=================================================="
  echo "  Ciclus rodando. Abra no navegador: $url"
  echo "  Ctrl+C para encerrar."
  echo "=================================================="
  echo
  while kill -0 "$back" 2>/dev/null && kill -0 "$front" 2>/dev/null; do sleep 1; done
else
  echo "O backend ou o frontend não subiu. Veja as mensagens acima." >&2
  exit 1
fi
