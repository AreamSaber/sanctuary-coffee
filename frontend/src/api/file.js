import request from '@/utils/request'

export function uploadImage(file, scene = 'common') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('scene', scene)

  return request({
    url: '/file/upload/image',
    method: 'post',
    data: formData,
    timeout: 30000
  })
}

export function uploadImages(files, scene = 'common') {
  const formData = new FormData()
  files.forEach((file) => {
    formData.append('files', file)
  })
  formData.append('scene', scene)

  return request({
    url: '/file/upload/images',
    method: 'post',
    data: formData,
    timeout: 60000
  })
}
