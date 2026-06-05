import { useTheme } from '../context/ThemeContext'

export default function ThemeToggle({ className = '', variant = 'default' }) {
  const { theme, toggleTheme } = useTheme()
  const isDark = theme === 'dark'
  const baseClass = variant === 'topbar' ? 'icon-btn theme-toggle-topbar' : 'theme-toggle'

  return (
    <button
      type="button"
      className={`${baseClass} ${className}`.trim()}
      onClick={toggleTheme}
      aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      title={isDark ? 'Light mode' : 'Dark mode'}
    >
      <span className="theme-toggle-icon" aria-hidden="true">
        {isDark ? '☀️' : '🌙'}
      </span>
    </button>
  )
}
