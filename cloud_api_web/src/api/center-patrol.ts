import request from '/@/api/http/request'

const prefix = '/api/center-patrol'
export const patrolPage = async (page = 1, size = 20) => (await request.get(`${prefix}/page`, { params: { page, size } })).data
export const patrolState = async (id: number, state: number) => (await request.post(`${prefix}/${id}/state/${state}`)).data
export const patrolExecutions = async (id: number, page = 1, size = 20) => (await request.get(`${prefix}/${id}/executions`, { params: { page, size } })).data
