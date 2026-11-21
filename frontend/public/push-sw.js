/* Line alerts pushed by the server while the app is closed (loaded into the generated service worker). */
const STATUS = {
  CROWDED: ['مزدحم', 'Crowded'], DELAYED: ['متأخر', 'Delayed'], DIVERTED: ['محوّل', 'Diverted'],
  FARE_UPDATED: ['تسعيرة محدثة', 'Fare updated'], NORMAL: ['طبيعي', 'Normal'],
}

self.addEventListener('push', (event) => {
  let data = {}
  try { data = event.data ? event.data.json() : {} } catch { data = {} }
  const [ar, en] = STATUS[data.status] || ['تنبيه', 'Alert']
  const title = data.routeName ? `🚍 ${data.routeName}` : 'مواصلات سوريا الذكية'
  const body = data.fare && data.status === 'FARE_UPDATED' ? `${ar} · ${en}: ${data.fare}` : `${ar} · ${en}`
  event.waitUntil(self.registration.showNotification(title, {
    body,
    tag: data.routeId ? `line-${data.routeId}` : 'line-alert',
    icon: 'icons/icon-192.png',
    badge: 'icons/icon-192.png',
    data: { url: data.url || self.registration.scope },
  }))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = (event.notification.data && event.notification.data.url) || self.registration.scope
  event.waitUntil((async () => {
    const all = await self.clients.matchAll({ type: 'window', includeUncontrolled: true })
    for (const c of all) {
      if (c.url.startsWith(self.registration.scope) && 'focus' in c) { await c.focus(); if ('navigate' in c) c.navigate(url); return }
    }
    await self.clients.openWindow(url)
  })())
})
