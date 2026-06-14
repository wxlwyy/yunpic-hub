// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** generateVipCode POST /api/vip/code/generate */
export async function generateVipCodeUsingPost(
  body: API.GenerateVipCodeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListString_>('/api/vip/code/generate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listVipCode GET /api/vip/code/list */
export async function listVipCodeUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listVipCodeUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageVipCode_>('/api/vip/code/list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** toggleVipCode POST /api/vip/code/toggle/${param0} */
export async function toggleVipCodeUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.toggleVipCodeUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/vip/code/toggle/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** listVipOrder GET /api/vip/order/list */
export async function listVipOrderUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listVipOrderUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageVipOrder_>('/api/vip/order/list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
