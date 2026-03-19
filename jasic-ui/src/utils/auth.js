import Cookies from 'js-cookie'

const TOKEN_KEY = 'jasic_token'
const COMPANY_KEY = 'jasic_company_id'

export function getToken() {
  return Cookies.get(TOKEN_KEY)
}

export function setToken(token) {
  return Cookies.set(TOKEN_KEY, token)
}

export function removeToken() {
  return Cookies.remove(TOKEN_KEY)
}

export function getCompanyId() {
  return Cookies.get(COMPANY_KEY)
}

export function setCompanyId(companyId) {
  return Cookies.set(COMPANY_KEY, companyId)
}

export function removeCompanyId() {
  return Cookies.remove(COMPANY_KEY)
}
