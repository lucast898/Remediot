const SEEN_KEY = 'remediot_onboarding_seen'

export function hasSeenOnboarding() {
  try {
    return sessionStorage.getItem(SEEN_KEY) === 'true'
  } catch {
    return true
  }
}

export function markOnboardingSeen() {
  try {
    sessionStorage.setItem(SEEN_KEY, 'true')
  } catch {
    // sessionStorage indisponível (ex.: modo privado) — segue sem persistir
  }
}
