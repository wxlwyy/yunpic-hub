// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addCategory POST /api/admin/category/add */
export async function addCategoryUsingPost(
  body: API.AddCategoryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/admin/category/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deleteCategory POST /api/admin/category/delete */
export async function deleteCategoryUsingPost(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/admin/category/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listCategories GET /api/admin/category/list */
export async function listCategoriesUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseListPictureCategory_>('/api/admin/category/list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** updateCategory POST /api/admin/category/update */
export async function updateCategoryUsingPost(
  body: API.UpdateCategoryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/admin/category/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
