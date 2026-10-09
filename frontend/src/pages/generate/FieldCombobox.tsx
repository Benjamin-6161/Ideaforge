import { Check, ChevronsUpDown } from 'lucide-react'
import { useState } from 'react'
import { Button } from '@/components/ui/button'
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { cn } from '@/lib/utils'
import type { Field } from '@/api/endpoints'

interface FieldComboboxProps {
  fields: Field[]
  value: string | null
  onChange: (fieldId: string) => void
  disabled?: boolean
}

/** Searchable dropdown over all fields. */
export function FieldCombobox({ fields, value, onChange, disabled }: FieldComboboxProps) {
  const [open, setOpen] = useState(false)
  const selected = fields.find((field) => field.id === value)

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          role="combobox"
          aria-expanded={open}
          aria-label="Field"
          disabled={disabled}
          className="h-10 w-full justify-between font-normal"
        >
          <span className={cn(!selected && 'text-muted-foreground')}>
            {selected ? selected.name : 'Pick an industry…'}
          </span>
          <ChevronsUpDown className="size-4 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-[--radix-popover-trigger-width] p-0" align="start">
        <Command>
          <CommandInput placeholder="Search fields…" />
          <CommandList>
            <CommandEmpty>No field matches.</CommandEmpty>
            <CommandGroup>
              {fields.map((field) => (
                <CommandItem
                  key={field.id}
                  value={field.name}
                  onSelect={() => {
                    onChange(field.id)
                    setOpen(false)
                  }}
                >
                  <Check className={cn('size-4', value === field.id ? 'opacity-100' : 'opacity-0')} />
                  <span>{field.name}</span>
                  <span className="ml-auto font-mono text-[10px] text-muted-foreground">
                    {field.niches.length} niches
                  </span>
                </CommandItem>
              ))}
            </CommandGroup>
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  )
}
