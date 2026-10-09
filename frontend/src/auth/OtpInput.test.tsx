import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { OtpInput } from './OtpInput'

function Harness() {
  const [value, setValue] = useState('')
  return <OtpInput value={value} onChange={setValue} />
}

describe('OtpInput', () => {
  it('auto-advances as digits are typed', async () => {
    const user = userEvent.setup()
    render(<Harness />)

    await user.type(screen.getByLabelText('Digit 1'), '1')
    await user.type(screen.getByLabelText('Digit 2'), '2')
    await user.type(screen.getByLabelText('Digit 3'), '3')

    expect(screen.getByLabelText('Digit 1')).toHaveValue('1')
    expect(screen.getByLabelText('Digit 2')).toHaveValue('2')
    expect(screen.getByLabelText('Digit 3')).toHaveValue('3')
  })

  it('distributes pasted codes across boxes', () => {
    render(<Harness />)
    const first = screen.getByLabelText('Digit 1')

    fireEvent.paste(first, { clipboardData: { getData: () => '123456' } })

    expect(screen.getByLabelText('Digit 1')).toHaveValue('1')
    expect(screen.getByLabelText('Digit 6')).toHaveValue('6')
  })

  it('clears the previous digit on backspace', async () => {
    const user = userEvent.setup()
    render(<Harness />)

    await user.type(screen.getByLabelText('Digit 1'), '4')
    await user.type(screen.getByLabelText('Digit 2'), '5')
    await user.keyboard('{Backspace}')

    expect(screen.getByLabelText('Digit 2')).toHaveValue('')
    expect(screen.getByLabelText('Digit 1')).toHaveValue('4')
  })
})
