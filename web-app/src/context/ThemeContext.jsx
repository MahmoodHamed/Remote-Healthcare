import { createContext, useContext, useEffect, useState } from 'react'
import { applyTheme, getStoredTheme, persistTheme, toggleTheme } from '../utils/theme'

const ThemeContext = createContext(null)

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(getStoredTheme)

  useEffect(() => {
    applyTheme(theme)
    persistTheme(theme)
  }, [theme])

  const setThemeMode = (next) => setTheme(next)
  const toggle = () => setTheme((current) => toggleTheme(current))

  return (
    <ThemeContext.Provider value={{ theme, setTheme: setThemeMode, toggleTheme: toggle }}>
      {children}
    </ThemeContext.Provider>
  )
}

export function useTheme() {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme must be used within ThemeProvider')
  return ctx
}
