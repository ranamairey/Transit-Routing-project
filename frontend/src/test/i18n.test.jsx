import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { I18nProvider, useI18n } from '../i18n/I18nContext.jsx'
import TripOptions from '../components/TripOptions.jsx'

function Probe() {
  const { t, lang, formatDuration, formatFare } = useI18n()
  return <div>{lang}|{t('planner.title')}|{t('trips.count', { n: 2 })}|{formatDuration(3600 + 120)}|{formatFare(1500)}</div>
}

describe('i18n', () => {
  it('falls back to Arabic by default and interpolates variables', () => {
    localStorage.setItem('sst.lang', 'ar')
    render(<I18nProvider><Probe /></I18nProvider>)
    expect(screen.getByText(/ar\|خطط رحلتك\|2 مسار\|1 س 2 د\|/)).toBeInTheDocument()
    expect(document.documentElement.dir).toBe('rtl')
  })
  it('switches to English and LTR', () => {
    localStorage.setItem('sst.lang', 'en')
    render(<I18nProvider><Probe /></I18nProvider>)
    expect(screen.getByText(/en\|Plan your trip\|2 route\(s\)\|1 h 2 min\|1,500 SYP/)).toBeInTheDocument()
    expect(document.documentElement.dir).toBe('ltr')
  })
})

describe('TripOptions', () => {
  it('renders fare, duration, transfers and legs of the selected option', () => {
    localStorage.setItem('sst.lang', 'en')
    const paths = [{
      totalFare: 3500, totalDurationSeconds: 3223, walkingMeters: 0, transfers: 1,
      legs: [
        { type: 'RIDE', routeName: 'Qatana - Somaria', vehicleType: 'SERVIS', routeStatus: 'CROWDED', fromName: 'Qatana', toName: 'Somaria', fare: 2000, durationSeconds: 2110, distanceMeters: 12894 },
        { type: 'WALK', fromName: 'Somaria', toName: 'Bridge', durationSeconds: 300, distanceMeters: 400 },
      ],
    }]
    render(<I18nProvider><TripOptions paths={paths} selected={0} onSelect={() => {}} /></I18nProvider>)
    expect(screen.getByText(/3,500 SYP/)).toBeInTheDocument()
    expect(screen.getByText(/54 min/)).toBeInTheDocument()
    expect(screen.getByText(/1 transfer/)).toBeInTheDocument()
    expect(screen.getByText(/Take the servis/)).toBeInTheDocument()
    expect(screen.getByText('Crowded')).toBeInTheDocument()
    expect(screen.getByText(/Walk 400 m to Bridge/)).toBeInTheDocument()
  })
})
