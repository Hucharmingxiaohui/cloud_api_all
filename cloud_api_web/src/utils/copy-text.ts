export async function copyText (text: string): Promise<boolean> {
  if (navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text)
      return true
    } catch (_) {}
  }
  const previousFocus = document.activeElement as HTMLElement | null
  const previousSelection = previousFocus && 'selectionStart' in previousFocus
    ? {
        start: (previousFocus as HTMLInputElement | HTMLTextAreaElement).selectionStart,
        end: (previousFocus as HTMLInputElement | HTMLTextAreaElement).selectionEnd,
        direction: (previousFocus as HTMLInputElement | HTMLTextAreaElement).selectionDirection
      }
    : null
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  textarea.style.top = '0'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  let copied = false
  try {
    textarea.focus()
    textarea.select()
    textarea.setSelectionRange(0, textarea.value.length)
    copied = document.execCommand('copy')
  } catch (_) {
    copied = false
  } finally {
    textarea.remove()
    if (previousFocus && typeof previousFocus.focus === 'function') {
      previousFocus.focus()
      if (previousSelection && typeof (previousFocus as HTMLInputElement | HTMLTextAreaElement).setSelectionRange === 'function' && previousSelection.start !== null && previousSelection.end !== null) {
        ;(previousFocus as HTMLInputElement | HTMLTextAreaElement).setSelectionRange(previousSelection.start, previousSelection.end, previousSelection.direction || undefined)
      }
    }
  }
  return copied
}
