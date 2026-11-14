import { CircleMarker, LayersControl, MapContainer, TileLayer, ZoomControl, useMapEvents } from 'react-leaflet'
import type { Ponto } from '../tipos'

// Vitória da Conquista (BA): base de testes do projeto.
const CENTRO: [number, number] = [-14.866, -40.839]

interface Props {
  ponto: Ponto | null
  aoEscolher: (p: Ponto) => void
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

export function MapaTerreno({ ponto, aoEscolher }: Props) {
  return (
    <MapContainer center={CENTRO} zoom={11} className="mapa__leaflet" zoomControl={false}>
      <ZoomControl position="topright" />
      <LayersControl position="topright">
        <LayersControl.BaseLayer checked name="Mapa">
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
        </LayersControl.BaseLayer>
        <LayersControl.BaseLayer name="Satélite">
          <TileLayer
            attribution="Imagens &copy; Esri"
            url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
          />
        </LayersControl.BaseLayer>
      </LayersControl>
      <CliqueNoMapa aoEscolher={aoEscolher} />
      {ponto && (
        <CircleMarker
          center={[ponto.latitude, ponto.longitude]}
          radius={10}
          pathOptions={{ color: '#fff', weight: 3, fillColor: '#2e4a3f', fillOpacity: 1 }}
        />
      )}
    </MapContainer>
  )
}
