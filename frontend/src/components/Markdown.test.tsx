import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Markdown } from './Markdown'

describe('Markdown', () => {
  it('renders basic markdown', () => {
    render(<Markdown>{'**bold** and a list\n\n- one\n- two'}</Markdown>)
    expect(screen.getByText('bold')).toBeInTheDocument()
    expect(screen.getByText('one')).toBeInTheDocument()
  })

  it('strips raw HTML and unsafe links', () => {
    render(
      <Markdown>{'<script>alert(1)</script>\n\n[click](javascript:alert(2))\n\n<img src=x onerror=alert(3)>'}</Markdown>,
    )
    expect(document.querySelector('script')).toBeNull()
    expect(document.querySelector('img')).toBeNull()
    const link = screen.queryByRole('link', { name: 'click' })
    if (link) {
      expect(link.getAttribute('href') ?? '').not.toContain('javascript:')
    }
  })

  it('adds safe rel attributes to external links', () => {
    render(<Markdown>{'[example](https://example.com)'}</Markdown>)
    const link = screen.getByRole('link', { name: 'example' })
    expect(link).toHaveAttribute('rel', 'noopener noreferrer nofollow')
    expect(link).toHaveAttribute('target', '_blank')
  })
})
