import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { getListing } from '@/api/listings'
import { useAuth } from '@/context/AuthContext'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { MapPin, Bed, Bath, Square, ArrowLeft, CreditCard } from 'lucide-react'

const statusVariant = { ACTIVE: 'success', INACTIVE: 'secondary', RENTED: 'warning', PENDING: 'outline' }

export default function ListingDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user, isAuthenticated } = useAuth()

  const [listing, setListing] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getListing(id)
      .then(setListing)
      .catch(() => setError('Listing not found'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <div className="container mx-auto px-4 py-16 text-center text-muted-foreground">Loading…</div>
  if (error || !listing) return (
    <div className="container mx-auto px-4 py-16 text-center">
      <p className="text-destructive mb-4">{error}</p>
      <Button variant="outline" onClick={() => navigate('/listings')}>Back to listings</Button>
    </div>
  )

  const canPay = isAuthenticated && user?.role === 'TENANT' && listing.status === 'ACTIVE'

  return (
    <div className="container mx-auto px-4 py-8 max-w-3xl space-y-6">
      <Button variant="ghost" size="sm" asChild>
        <Link to="/listings"><ArrowLeft className="h-4 w-4" />Back to listings</Link>
      </Button>

      {listing.imageUrls?.length > 0 ? (
        <div className="space-y-2">
          <img
            src={listing.imageUrls[0].startsWith('/') ? `http://localhost:8080${listing.imageUrls[0]}` : listing.imageUrls[0]}
            alt={listing.title}
            className="w-full h-72 object-cover rounded-lg border"
          />
          {listing.imageUrls.length > 1 && (
            <div className="grid grid-cols-4 gap-2">
              {listing.imageUrls.slice(1).map((url, i) => (
                <img
                  key={i}
                  src={url.startsWith('/') ? `http://localhost:8080${url}` : url}
                  alt={`${listing.title} ${i + 2}`}
                  className="h-20 w-full object-cover rounded-md border"
                />
              ))}
            </div>
          )}
        </div>
      ) : (
        <div className="rounded-lg border bg-muted h-72 flex items-center justify-center text-muted-foreground">
          No photos available
        </div>
      )}

      <div className="space-y-4">
        <div className="flex items-start justify-between gap-4">
          <h1 className="text-2xl font-bold">{listing.title}</h1>
          <Badge variant={statusVariant[listing.status] ?? 'secondary'}>{listing.status}</Badge>
        </div>

        <div className="flex items-center gap-1 text-muted-foreground">
          <MapPin className="h-4 w-4" />
          {listing.address}, {listing.city}{listing.state ? `, ${listing.state}` : ''} {listing.zipCode}
        </div>

        <div className="flex items-center gap-6 text-sm text-muted-foreground">
          <span className="flex items-center gap-1"><Bed className="h-4 w-4" />{listing.bedrooms} bedrooms</span>
          <span className="flex items-center gap-1"><Bath className="h-4 w-4" />{listing.bathrooms} bathrooms</span>
          {listing.squareFeet > 0 && (
            <span className="flex items-center gap-1"><Square className="h-4 w-4" />{listing.squareFeet} sqft</span>
          )}
        </div>

        <p className="text-3xl font-bold text-primary">
          ${Number(listing.pricePerMonth).toLocaleString()}
          <span className="text-base font-normal text-muted-foreground"> / month</span>
        </p>

        {listing.description && (
          <Card><CardContent className="pt-4"><p className="text-sm leading-relaxed">{listing.description}</p></CardContent></Card>
        )}

        {listing.amenities?.length > 0 && (
          <div>
            <h2 className="font-semibold mb-2">Amenities</h2>
            <div className="flex flex-wrap gap-2">
              {listing.amenities.map((a) => <Badge key={a} variant="secondary">{a}</Badge>)}
            </div>
          </div>
        )}

        {listing.availableFrom && (
          <p className="text-sm text-muted-foreground">
            Available from: {new Date(listing.availableFrom).toLocaleDateString()}
          </p>
        )}
      </div>

      {canPay && (
        <Button size="lg" className="w-full" asChild>
          <Link to={`/pay/${listing.id}`} state={{ listing }}>
            <CreditCard className="h-5 w-5" />
            Rent this property — ${Number(listing.pricePerMonth).toLocaleString()}/mo
          </Link>
        </Button>
      )}

      {!isAuthenticated && listing.status === 'ACTIVE' && (
        <div className="rounded-md border p-4 text-center text-sm text-muted-foreground">
          <Link to="/login" className="text-primary hover:underline">Sign in</Link> as a tenant to rent this property.
        </div>
      )}
    </div>
  )
}
