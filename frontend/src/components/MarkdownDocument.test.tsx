import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MarkdownDocument } from './MarkdownDocument'

describe('MarkdownDocument', () => {
  const markdown = '# Build Prompt\n\nSome **bold** instructions.'

  beforeEach(() => {
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: vi.fn().mockResolvedValue(undefined) },
    })
  })

  it('renders the markdown and both actions', () => {
    render(<MarkdownDocument title="Build prompt" markdown={markdown} filename="build-prompt.md" />)
    expect(screen.getByText('Build Prompt')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Copy/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Download/ })).toBeInTheDocument()
    expect(screen.getByText(/characters · markdown/)).toBeInTheDocument()
  })

  it('copies the raw markdown to the clipboard', async () => {
    render(<MarkdownDocument title="Build prompt" markdown={markdown} filename="build-prompt.md" />)

    fireEvent.click(screen.getByRole('button', { name: /Copy/ }))

    await waitFor(() => expect(navigator.clipboard.writeText).toHaveBeenCalledWith(markdown))
  })

  it('downloads a .md file', async () => {
    const createObjectURL = vi.fn().mockReturnValue('blob:test')
    const revokeObjectURL = vi.fn()
    Object.assign(URL, { createObjectURL, revokeObjectURL })
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})

    const user = userEvent.setup()
    render(<MarkdownDocument title="AGENTS.md" markdown={markdown} filename="AGENTS.md" />)
    await user.click(screen.getByRole('button', { name: /Download/ }))

    expect(createObjectURL).toHaveBeenCalled()
    expect(click).toHaveBeenCalled()
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:test')
  })
})
