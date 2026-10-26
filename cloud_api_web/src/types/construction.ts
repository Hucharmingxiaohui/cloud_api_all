export interface ConstructionPoint { lng: number; lat: number }
export type ConstructionLens = 'dual' | 'visible' | 'infrared'
export interface ConstructionParams {
  altitude: number
  speed: number
  pitch: number
  interval: number
  lens: ConstructionLens
}
export interface ConstructionArea {
  id: string
  name: string
  category: string
  enabled: boolean
  notes: string
  version: number
  boundary: ConstructionPoint[]
  params: ConstructionParams
  updatedAt: string
}
export interface ConstructionDock {
  id: string
  name: string
  mode: 'mobile' | 'fixed'
  position: ConstructionPoint
}
export interface ConstructionPlan {
  id: string
  name: string
  areaId: string
  areaVersion: number
  dockId: string
  params: ConstructionParams
  source: 'generated' | 'imported'
  fileName: string
  schedule: 'manual' | 'scheduled'
  scheduledAt: string
  createdAt: string
}
export interface ConstructionTask {
  id: string
  name: string
  planId: string
  area: ConstructionArea
  dock: ConstructionDock
  params: ConstructionParams
  createdAt: string
  status: 'preview' | 'sample'
}
export interface ConstructionData {
  version: 1
  areas: ConstructionArea[]
  plans: ConstructionPlan[]
  tasks: ConstructionTask[]
}
