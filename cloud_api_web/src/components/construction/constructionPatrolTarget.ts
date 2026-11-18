import type { ConstructionAreaRecord, ConstructionLine, ConstructionPoint } from '/@/api/construction'
import { areaCoverage, pointsWithinCoverage } from './dockCoverageGeometry'

export type PatrolTarget = ConstructionAreaRecord & { line?: ConstructionLine }
export function lineTarget (line: ConstructionLine): PatrolTarget {
  return { id: `line:${line.line_id}`, name: line.name, version: line.version, enabled: line.enabled, archived: line.archived, category: line.type, notes: line.notes, boundary: [], line }
}
export function targetCovered (center: ConstructionPoint, radius: number, target: PatrolTarget): boolean {
  return target.line
    ? target.line.points.length >= 2 && pointsWithinCoverage(center, radius, target.line.points)
    : areaCoverage(center, radius, target.boundary || []) === 'full'
}
export function targetLabel (target: PatrolTarget): string { return target.line ? `线路 · ${target.name}` : target.name }
