import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { getMyListings, deleteListing } from '@/api/listings'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Plus, Pencil, Trash2, MapPin } from 'lucide-react'

const statusVariant = { ACTIVE: 'success', INACTIVE: 'secondary', RENTED: 'warning', PENDING: 'outline' }

export default function DashboardPage() {
  const [listings, setListings] = useState([])
  const [loading, setLoading] = useState(true)
  const [deleting, setDeleting] = useState(null)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setListings(await getMyListings())
    } catch {
      setError('Failed to load your listings')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this listing?')) return
    setDeleting(id)
    try {
      await deleteListing(id)
      setListings((prev) => prev.filter((l) => l.id !== id))
    } catch {
      alert('Failed to delete listing')
    } finally {
      setDeleting(null)
    }
  }

  return (
    <div className="container mx-auto px-4 py-8 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">My Listings</h1>
          <p className="text-muted-foreground mt-1">{listings.length} listing{listings.length !== 1 ? 's' : ''}</p>
        </div>
        <Button asChild>
          <Link to="/dashboard/new"><Plus className="h-4 w-4" />New Listing</Link>
        </Button>
      </div>

      {error && <div className="rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">{error}</div>}

      {loading && (
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-24 rounded-lg bg-muted animate-pulse" />
          ))}
        </div>
      )}

      {!loading && listings.length === 0 && (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-16 gap-4">
            <p className="text-muted-foreground">You haven't created any listings yet.</p>
            <Button asChild>
              <Link to="/dashboard/new"><Plus className="h-4 w-4" />Create your first listing</Link>
            </Button>
          </CardContent>
        </Card>
      )}

      {!loading && listings.length > 0 && (
        <div className="space-y-3">
          {listings.map((listing) => (
            <Card key={listing.id} className="flex items-center gap-4 p-4">
              <div className="flex-1 min-w-0 space-y-1">
                <div className="flex items-center gap-2">
                  <p className="font-semibold truncate">{listing.title}</p>
                  <Badge variant={statusVariant[listing.status] ?? 'secondary'}>{listing.status}</Badge>
                </div>
                <p className="text-sm text-muted-foreground flex items-center gap-1">
                  <MapPin className="h-3.5 w-3.5" />{listing.city}{listing.state ? `, ${listing.state}` : ''}
                </p>
                <p className="text-sm font-medium text-primary">
                  ${Number(listing.pricePerMonth).toLocaleString()}/mo · {listing.bedrooms}bd {listing.bathrooms}ba
                </p>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <Button variant="outline" size="sm" asChild>
                  <Link to={`/dashboard/edit/${listing.id}`}><Pencil className="h-3.5 w-3.5" />Edit</Link>
                </Button>
                <Button
                  variant="destructive"
                  size="sm"
                  disabled={deleting === listing.id}
                  onClick={() => handleDelete(listing.id)}
                >
                  <Trash2 className="h-3.5 w-3.5" />
                  {deleting === listing.id ? '…' : 'Delete'}
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
