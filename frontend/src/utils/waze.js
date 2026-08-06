export function wazeUrl(latitude, longitude) {
  if (latitude == null || longitude == null) {
    return null
  }
  return `https://waze.com/ul?ll=${latitude},${longitude}&navigate=yes`
}
