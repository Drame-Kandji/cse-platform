export const SENEGAL_MAP_CENTER: [number, number] = [14.4974, -14.4524]

export const SENEGAL_CITIES = {
  Dakar: { latitude: 14.6928, longitude: -17.4467 },
  Thiès: { latitude: 14.7886, longitude: -16.926 },
  'Saint-Louis': { latitude: 16.0179, longitude: -16.4896 },
  Kaolack: { latitude: 14.152, longitude: -16.0726 },
  Ziguinchor: { latitude: 12.5681, longitude: -16.2719 },
  Touba: { latitude: 14.85, longitude: -15.8833 },
} as const

export const RESPONSABLE_MATERIEL_ID = 'b2222222-2222-2222-2222-222222222222'
export const RESPONSABLE_ACHATS_ID = 'b3333333-3333-3333-3333-333333333333'
export const DEMO_PERIODE = '2026-Q4'

export const EXPORT_FORMATS = [
  { value: 'pdf', label: 'PDF' },
  { value: 'xlsx', label: 'Excel' },
] as const
