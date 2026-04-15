/** 故障补充：1 视频 + 最多 3 张图 */
export function validateFaultMediaSelection(tempFiles: { fileType?: string }[]): boolean {
  let videoCount = 0
  let imageCount = 0
  for (const file of tempFiles) {
    if (file.fileType === 'video') videoCount++
    else if (file.fileType === 'image') imageCount++
  }
  if (videoCount > 1) {
    uni.showToast({ title: '最多只能上传1个视频', icon: 'none', duration: 1500 })
    return false
  }
  if (imageCount > 3) {
    uni.showToast({ title: '最多只能上传3张图片', icon: 'none', duration: 1500 })
    return false
  }
  return true
}
