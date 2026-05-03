export type ApiResponse<T> = {
  success: boolean
  message: string
  data: T
}

export type PageResponse<T> = {
  totalRecords: number
  totalPages: number
  currPage: number
  size: number
  records: T[]
}
