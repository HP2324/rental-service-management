import { useState, useEffect, useCallback } from 'react'
import { getListings, searchListings } from '@/api/listings'
import { ListingCard } from '@/components/ListingCard'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Search, X } from 'lucide-react'

export default function ListingsPage() {
  const [listings, setListings] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [filters, setFilters] = useState({ city: '', maxPrice: '', minBedrooms: '' })
  const [activeFilters, setActiveFilters] = useState(false)

  const fetchAll = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await getListings()
      setListings(data)
      setActiveFilters(false)
    } catch {
      setError('Failed to load listings')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchAll() }, [fetchAll])

  const handleSearch = async (e) => {
    e.preventDefault()
    if (!filters.city.trim()) return
    setLoading(true)
    setError('')
    try {
      const data = await searchListings({
        city: filters.city,
        maxPrice: filters.maxPrice || undefined,
        minBedrooms: filters.minBedrooms || 0,
      })
      setListings(data)
      setActiveFilters(true)
    } catch {
      setError('Search failed')
    } finally {
      setLoading(false)
    }
  }

  const clearFilters = () => {
    setFilters({ city: '', maxPrice: '', minBedrooms: '' })
    fetchAll()
  }

  return (
    <div className="container mx-auto px-4 py-8 space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Browse Rentals</h1>
        <p className="text-muted-foreground mt-1">Find your next home</p>
      </div>

      {/* Search bar */}
      <form onSubmit={handleSearch} className="flex flex-wrap items-end gap-3 p-4 rounded-lg border bg-card">
        <div className="space-y-1 min-w-[160px] flex-1">
          <Label htmlFor="city">City</Label>
          <Input id="city" placeholder="e.g. Austin" value={filters.city}
            onChange={(e) => setFilters((f) => ({ ...f, city: e.target.value }))} />
        </div>
        <div className="space-y-1 w-36">
          <Label htmlFor="maxPrice">Max price / mo</Label>
          <Input id="maxPrice" type="number" placeholder="e.g. 2000" value={filters.maxPrice}
            onChange={(e) => setFilters((f) => ({ ...f, maxPrice: e.target.value }))} />
        </div>
        <div className="space-y-1 w-32">
          <Label htmlFor="minBedrooms">Min bedrooms</Label>
          <Input id="minBedrooms" type="number" min={0} placeholder="0" value={filters.minBedrooms}
            onChange={(e) => setFilters((f) => ({ ...f, minBedrooms: e.target.value }))} />
        </div>
        <Button type="submit"><Search className="h-4 w-4" />Search</Button>
        {activeFilters && (
          <Button type="button" variant="ghost" onClick={clearFilters}>
            <X className="h-4 w-4" />Clear
          </Button>
        )}
      </form>

      {/* Results */}
      {loading && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-72 rounded-lg bg-muted animate-pulse" />
          ))}
        </div>
      )}

      {!loading && error && (
        <div className="rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">{error}</div>
      )}

      {!loading && !error && listings.length === 0 && (
        <div className="text-center py-16 text-muted-foreground">
          No listings found{activeFilters ? ' — try different filters' : ''}.
        </div>
      )}

      {!loading && !error && listings.length > 0 && (
        <>
          <p className="text-sm text-muted-foreground">{listings.length} listing{listings.length !== 1 ? 's' : ''} found</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {listings.map((l) => <ListingCard key={l.id} listing={l} />)}
          </div>
        </>
      )}
    </div>
  )
}
