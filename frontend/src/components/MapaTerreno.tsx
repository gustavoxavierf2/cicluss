import { useEffect, useMemo, useState } from 'react'
import {
  CircleMarker, LayerGroup, LayersControl, MapContainer, TileLayer, ZoomControl, useMap, useMapEvents,
} from 'react-leaflet'
import type { Ponto } from '../tipos'

// Vitória da Conquista (BA): base de testes do projeto.
const CENTRO: [number, number] = [-14.866, -40.839]
const ZOOM_INICIAL = 8
const ZOOM_DO_TERRENO = 11

// Chuva medida por satélite (NASA GPM IMERG), um quadro por dia, servida pelo NASA GIBS.
const DIAS_DE_CHUVA = 10
const ATRASO_EM_DIAS = 2 // a NASA publica o dia com cerca de dois dias de atraso
const TEMPO_POR_QUADRO_MS = 900
const URL_CHUVA =
  'https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/IMERG_Precipitation_Rate/default/{dia}/GoogleMapsCompatible_Level6/{z}/{y}/{x}.png'

interface Props {
  ponto: Ponto | null
  aoEscolher: (p: Ponto) => void
}

/** Os últimos dias com imagem de chuva, do mais antigo para o mais novo, no formato AAAA-MM-DD. */
function diasDeChuva(): string[] {
  const dias: string[] = []
  for (let i = DIAS_DE_CHUVA - 1; i >= 0; i--) {
    const d = new Date(Date.now() - (i + ATRASO_EM_DIAS) * 86_400_000)
    dias.push(d.toISOString().slice(0, 10))
  }
  return dias
}

function diaEmTexto(dia: string) {
  const [, mes, d] = dia.split('-')
  const meses = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']
  return `${Number(d)} de ${meses[Number(mes) - 1]}`
}

function CliqueNoMapa({ aoEscolher }: { aoEscolher: (p: Ponto) => void }) {
  useMapEvents({
    click(e) {
      aoEscolher({
        latitude: Number(e.latlng.lat.toFixed(4)),
        longitude: Number(e.latlng.lng.toFixed(4)),
      })
    },
  })
  return null
}

/** Leva o mapa até o terreno marcado com um voo suave. */
function VoarParaOTerreno({ ponto }: { ponto: Ponto | null }) {
  const mapa = useMap()
  useEffect(() => {
    if (!ponto) return
    mapa.flyTo([ponto.latitude, ponto.longitude], Math.max(mapa.getZoom(), ZOOM_DO_TERRENO), { duration: 1.4 })
  }, [mapa, ponto])
  return null
}

export function MapaTerreno({ ponto, aoEscolher }: Props) {
  const dias = useMemo(diasDeChuva, [])
  const [chuvaLigada, setChuvaLigada] = useState(true)
  const [tocando, setTocando] = useState(true)
  const [quadro, setQuadro] = useState(dias.length - 1)

  useEffect(() => {
    if (!chuvaLigada || !tocando) return
    // Quem pediu menos movimento no sistema vê a chuva parada no último dia.
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const relogio = setInterval(() => setQuadro((q) => (q + 1) % dias.length), TEMPO_POR_QUADRO_MS)
    return () => clearInterval(relogio)
  }, [chuvaLigada, tocando, dias.length])

  return (
    <div className="mapa-terreno">
      <MapContainer center={CENTRO} zoom={ZOOM_INICIAL} className="mapa__leaflet" zoomControl={false}>
        <ZoomControl position="topright" />
        <LayersControl position="topright">
          <LayersControl.BaseLayer checked name="Satélite">
            <LayerGroup>
              <TileLayer
                attribution="Imagens &copy; Esri"
                url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
              />
              <TileLayer
                url="https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}"
              />
            </LayerGroup>
          </LayersControl.BaseLayer>
          <LayersControl.BaseLayer name="Mapa de ruas">
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
          </LayersControl.BaseLayer>
        </LayersControl>

        {chuvaLigada &&
          dias.map((dia, i) => (
            <TileLayer
              key={dia}
              className="chuva-quadro"
              attribution="Chuva: NASA GPM IMERG (GIBS)"
              url={URL_CHUVA.replace('{dia}', dia)}
              opacity={i === quadro ? 0.85 : 0}
              maxNativeZoom={6}
              zIndex={5}
            />
          ))}

        <CliqueNoMapa aoEscolher={aoEscolher} />
        <VoarParaOTerreno ponto={ponto} />
        {ponto && (
          <>
            <CircleMarker
              key={`pulso-${ponto.latitude}-${ponto.longitude}`}
              center={[ponto.latitude, ponto.longitude]}
              radius={10}
              interactive={false}
              pathOptions={{ className: 'marcador-pulso', color: '#f6efe6', fill: false }}
            />
            <CircleMarker
              center={[ponto.latitude, ponto.longitude]}
              radius={9}
              pathOptions={{ color: '#fff', weight: 3, fillColor: '#e29556', fillOpacity: 1 }}
            />
          </>
        )}
      </MapContainer>

      <div className="chuva-controle">
        {chuvaLigada && (
          <button type="button" className="chuva-controle__tocar" onClick={() => setTocando(!tocando)}
            aria-label={tocando ? 'Pausar a animação da chuva' : 'Tocar a animação da chuva'}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
              {tocando ? <path d="M7 5h4v14H7zM13 5h4v14h-4z" /> : <path d="M8 5v14l11-7z" />}
            </svg>
          </button>
        )}
        <div className="chuva-controle__texto">
          <b>Chuva vista do satélite</b>
          <span>{chuvaLigada ? `${diaEmTexto(dias[quadro])} · NASA` : 'desligada'}</span>
        </div>
        {chuvaLigada && (
          <input type="range" className="chuva-controle__dias" min={0} max={dias.length - 1} value={quadro}
            aria-label="Dia da imagem de chuva"
            onChange={(e) => {
              setTocando(false)
              setQuadro(Number(e.target.value))
            }} />
        )}
        <button type="button" className="chuva-controle__ligar" aria-pressed={chuvaLigada}
          onClick={() => setChuvaLigada(!chuvaLigada)}>
          {chuvaLigada ? 'Esconder' : 'Mostrar'}
        </button>
      </div>
    </div>
  )
}
