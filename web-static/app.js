const page = document.querySelector('.page')
const toggle = document.querySelector('.nav-toggle')
const drawerLinks = document.querySelectorAll('[data-close]')
const faqItems = document.querySelectorAll('.faq-item')
const form = document.querySelector('.form')
const note = document.querySelector('.form-note')

if (toggle) {
  toggle.addEventListener('click', () => {
    page.classList.toggle('menu-open')
  })
}

drawerLinks.forEach((link) => {
  link.addEventListener('click', () => {
    page.classList.remove('menu-open')
  })
})

faqItems.forEach((item, index) => {
  const button = item.querySelector('button')
  button.addEventListener('click', () => {
    faqItems.forEach((other, otherIndex) => {
      const isOpen = otherIndex === index && !item.classList.contains('open')
      other.classList.toggle('open', isOpen)
      const btn = other.querySelector('button')
      btn.setAttribute('aria-expanded', isOpen ? 'true' : 'false')
    })
  })
})

if (form) {
  form.addEventListener('submit', (event) => {
    event.preventDefault()
    if (note) {
      note.hidden = false
    }
  })
}
