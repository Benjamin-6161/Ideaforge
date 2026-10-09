import { useRef, type ClipboardEvent, type KeyboardEvent } from 'react'
import { cn } from '@/lib/utils'

interface OtpInputProps {
  value: string
  onChange: (value: string) => void
  length?: number
  disabled?: boolean
  autoFocus?: boolean
}

/**
 * Six single-digit boxes with auto-advance, backspace navigation, and paste.
 * Keyboard and screen-reader accessible.
 */
export function OtpInput({ value, onChange, length = 6, disabled, autoFocus }: OtpInputProps) {
  const inputs = useRef<Array<HTMLInputElement | null>>([])
  const digits = Array.from({ length }, (_, i) => value[i] ?? '')

  const setDigit = (index: number, digit: string) => {
    const next = digits.slice()
    next[index] = digit
    onChange(next.join('').slice(0, length))
  }

  const focusAt = (index: number) => {
    const clamped = Math.max(0, Math.min(length - 1, index))
    inputs.current[clamped]?.focus()
    inputs.current[clamped]?.select()
  }

  const handleInput = (index: number, raw: string) => {
    const digit = raw.replace(/\D/g, '').slice(-1)
    if (!digit) {
      setDigit(index, '')
      return
    }
    setDigit(index, digit)
    focusAt(index + 1)
  }

  const handleKeyDown = (index: number, event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Backspace') {
      event.preventDefault()
      if (digits[index]) {
        setDigit(index, '')
      } else {
        setDigit(index - 1, '')
        focusAt(index - 1)
      }
    } else if (event.key === 'ArrowLeft') {
      event.preventDefault()
      focusAt(index - 1)
    } else if (event.key === 'ArrowRight') {
      event.preventDefault()
      focusAt(index + 1)
    }
  }

  const handlePaste = (event: ClipboardEvent<HTMLInputElement>) => {
    event.preventDefault()
    const pasted = event.clipboardData.getData('text').replace(/\D/g, '').slice(0, length)
    if (pasted) {
      onChange(pasted)
      focusAt(pasted.length)
    }
  }

  return (
    <div role="group" aria-label="Verification code" className="flex items-center gap-2">
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={(element) => {
            inputs.current[index] = element
          }}
          value={digit}
          onChange={(event) => handleInput(index, event.target.value)}
          onKeyDown={(event) => handleKeyDown(index, event)}
          onPaste={handlePaste}
          onFocus={(event) => event.target.select()}
          inputMode="numeric"
          autoComplete={index === 0 ? 'one-time-code' : 'off'}
          maxLength={1}
          disabled={disabled}
          autoFocus={autoFocus && index === 0}
          aria-label={`Digit ${index + 1}`}
          className={cn(
            'h-12 w-10 rounded-md border border-input bg-background text-center font-mono text-lg shadow-xs outline-none transition',
            'focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50',
            'disabled:cursor-not-allowed disabled:opacity-50',
          )}
        />
      ))}
    </div>
  )
}
