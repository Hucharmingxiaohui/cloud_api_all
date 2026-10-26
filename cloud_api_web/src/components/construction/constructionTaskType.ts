// Backend counterpart: ConstructionType.PLAN_TYPE. 5 belongs to EUA.
export const CONSTRUCTION_PLAN_TYPE = 6

export function isConstructionTask (task: { plan_type?: unknown; planType?: unknown } | null | undefined) {
  const type = task?.plan_type ?? task?.planType
  return type === CONSTRUCTION_PLAN_TYPE || type === String(CONSTRUCTION_PLAN_TYPE)
}
