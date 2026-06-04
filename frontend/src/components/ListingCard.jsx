import { Link } from 'react-router-dom'
import { Card, CardContent, CardFooter } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { MapPin, Bed, Bath, Square } from 'lucide-react'

const statusVariant = {
  ACTIVE: 'success',
  INACTIVE: 'secondary',
  RENTED: 'warning',
  PENDING: 'outline',
}

export function ListingCard({ listing }) {
  return (
    <Card className="flex flex-col overflow-hidden hover:shadow-md transition-shadow">
      {listing.imageUrls?.length > 0 ? (
        <img
          src={listing.imageUrls[0].startsWith('/') ? `http://localhost:8080${listing.imageUrls[0]}` : listing.imageUrls[0]}
          alt={listing.title}
          className="h-48 w-full object-cover"
        />
      ) : (
        <div className="h-48 bg-muted flex items-center justify-center text-muted-foreground text-sm">
          No photos yet
        </div>
      )}

      <CardContent className="flex-1 pt-4 space-y-3">
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-semibold leading-tight line-clamp-2">{listing.title}</h3>
          <Badge variant={statusVariant[listing.status] ?? 'secondary'} className="shrink-0">
            {listing.status}
          </Badge>
        </div>

        <div className="flex items-center gap-1 text-sm text-muted-foreground">
          <MapPin className="h-3.5 w-3.5" />
          {listing.city}{listing.state ? `, ${listing.state}` : ''}
        </div>

        <div className="flex items-center gap-4 text-sm text-muted-foreground">
          <span className="flex items-center gap-1"><Bed className="h-3.5 w-3.5" />{listing.bedrooms} bd</span>
          <span className="flex items-center gap-1"><Bath className="h-3.5 w-3.5" />{listing.bathrooms} ba</span>
          {listing.squareFeet > 0 && (
            <span className="flex items-center gap-1"><Square className="h-3.5 w-3.5" />{listing.squareFeet} sqft</span>
          )}
        </div>

        <p className="text-xl font-bold text-primary">
          ${Number(listing.pricePerMonth).toLocaleString()}<span className="text-sm font-normal text-muted-foreground">/mo</span>
        </p>
      </CardContent>

      <CardFooter className="pt-0">
        <Button variant="outline" size="sm" className="w-full" asChild>
          <Link to={`/listings/${listing.id}`}>View Details</Link>
        </Button>
      </CardFooter>
    </Card>
  )
}
