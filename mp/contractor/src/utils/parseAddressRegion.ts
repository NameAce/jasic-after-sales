/**
 * 将合并后的中文地址拆成省 / 市 / 区与详细地址，供公司地址簿（仅 address 字段）编辑回显与 region picker 使用。
 * 规则与表单保存时 `${province}${city}${county}${detail}` 拼接方式互逆，无法识别时整段落入 detail。
 */
export type ParsedAddressRegion = {
  province: string
  city: string
  county: string
  detail: string
}

/** 直辖市：省级与地级同为该市名 */
const MUNICIPALITIES = ['北京市', '天津市', '上海市', '重庆市'] as const

const RE_PROVINCE = /^(.+?(?:省|自治区|特别行政区))/
const RE_CITY = /^(.+?(?:市|自治州|地区|盟))/
const RE_COUNTY = /^(.+?(?:区|县|旗|新区|林区|市|岛))/

/**
 * 解析整段地址为省市区 + 详细地址
 * @param line 完整地址文案
 */
export function parseFullAddressLine(line: string): ParsedAddressRegion {
  let rest = String(line ?? '').trim()
  if (!rest) {
    return { province: '', city: '', county: '', detail: '' }
  }

  let province = ''
  let city = ''
  let county = ''

  for (const m of MUNICIPALITIES) {
    if (rest.startsWith(m)) {
      province = m
      city = m
      rest = rest.slice(m.length)
      break
    }
  }

  if (!province) {
    const m = rest.match(RE_PROVINCE)
    if (m) {
      province = m[1]
      rest = rest.slice(province.length)
    }
  }

  if (!city) {
    const m = rest.match(RE_CITY)
    if (m) {
      city = m[1]
      rest = rest.slice(city.length)
    }
  }

  const countyMatch = rest.match(RE_COUNTY)
  if (countyMatch) {
    county = countyMatch[1]
    rest = rest.slice(county.length)
  }

  const detail = rest.trim()
  if (!province && !city && !county) {
    return { province: '', city: '', county: '', detail: String(line).trim() }
  }

  return { province, city, county, detail }
}

/**
 * 将 SavedAddress 中的省市区补齐（优先已有字段，否则从 fullAddress / detail 解析）
 */
export function resolveSavedAddressRegion(item: {
  province?: string
  city?: string
  county?: string
  detail?: string
  fullAddress?: string
}): ParsedAddressRegion {
  const province = String(item.province ?? '').trim()
  const city = String(item.city ?? '').trim()
  const county = String(item.county ?? '').trim()
  const detail = String(item.detail ?? '').trim()

  if (province || city) {
    return { province, city, county, detail }
  }

  const line = String(item.fullAddress ?? '').trim() || detail
  return parseFullAddressLine(line)
}
