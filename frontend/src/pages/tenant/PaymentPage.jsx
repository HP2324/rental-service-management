import { useState, useEffect } from 'react'
import { useParams, useLocation, useNavigate, Link } from 'react-router-dom'
import { loadStripe } from '@stripe/stripe-js'
import { Elements, CardElement, useStripe, useElements } from '@stripe/react-stripe-js'
import { getListing, updateListingStatus } from '@/api/listings'
import { initiatePayment, syncStripeStatus } from '@/api/payments'
import PayPalButton from '@/components/PayPalButton'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
import { ArrowLeft, CheckCircle, XCircle } from 'lucide-react'

const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY ?? '')

const CARD_ELEMENT_OPTIONS = {
  style: {
    base: {
      fontSize: '16px',
      color: '#1a1a1a',
      '::placeholder': { color: '#9ca3af' },
    },
    invalid: { color: '#dc2626' },
  },
}

// ── Inner form (needs Stripe context) ──────────────────────────────────────────
function CheckoutForm({ listing, listingId, onSuccess, onError }) {
  const stripe = useStripe()
  const elements = useElements()
  const [loading, setLoading] = useState(false)
  const [clientSecret, setClientSecret] = useState(null)
  const [paymentId, setPaymentId] = useState(null)
  const [initiated, setInitiated] = useState(false)

  // Step 1 — create PaymentIntent on the backend
  const initiate = async () => {
    setLoading(true)
    try {
      const payment = await initiatePayment({
        listingId: listing.id,
        landlordId: listing.landlordId,
        amount: listing.pricePerMonth,
        currency: 'USD',
        provider: 'STRIPE',
        description: `Rent for ${listing.title}`,
      })
      setClientSecret(payment.clientSecret)
      setPaymentId(payment.id)
      setInitiated(true)
    } catch (err) {
      onError(err.response?.data?.message ?? 'Failed to start payment')
    } finally {
      setLoading(false)
    }
  }

  // Step 2 — confirm with Stripe.js + sync status back
  const confirmPayment = async (e) => {
    e.preventDefault()
    if (!stripe || !elements || !clientSecret) return
    setLoading(true)
    try {
      const { error, paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
        payment_method: { card: elements.getElement(CardElement) },
      })

      if (error) {
        onError(error.message)
        return
      }

      if (paymentIntent.status === 'succeeded') {
        await syncStripeStatus(paymentId)
        // Mark the listing as inactive so it no longer appears in browse
        await updateListingStatus(listingId, 'INACTIVE')
        onSuccess()
      } else {
        onError(`Unexpected status: ${paymentIntent.status}`)
      }
    } catch {
      onError('Payment confirmation failed')
    } finally {
      setLoading(false)
    }
  }

  if (!initiated) {
    return (
      <div className="space-y-4">
        <div className="rounded-lg bg-muted p-4 text-sm space-y-1">
          <p className="font-medium">{listing.title}</p>
          <p className="text-muted-foreground">{listing.city}{listing.state ? `, ${listing.state}` : ''}</p>
          <p className="text-lg font-bold text-primary">${Number(listing.pricePerMonth).toLocaleString()}<span className="text-sm font-normal">/mo</span></p>
        </div>
        <Button className="w-full" onClick={initiate} disabled={loading}>
          {loading ? 'Setting up payment…' : 'Proceed to Payment'}
        </Button>
      </div>
    )
  }

  return (
    <form onSubmit={confirmPayment} className="space-y-6">
      <div className="rounded-lg bg-muted p-4 text-sm space-y-1">
        <p className="font-medium">{listing.title}</p>
        <p className="text-lg font-bold text-primary">${Number(listing.pricePerMonth).toLocaleString()}<span className="text-sm font-normal">/mo</span></p>
      </div>

      <div className="space-y-2">
        <p className="text-sm font-medium">Card details</p>
        <div className="rounded-md border border-input p-3 focus-within:ring-2 focus-within:ring-ring">
          <CardElement options={CARD_ELEMENT_OPTIONS} />
        </div>
        <p className="text-xs text-muted-foreground">Use test card: 4242 4242 4242 4242 · any future date · any CVC</p>
      </div>

      <Button type="submit" className="w-full" disabled={loading || !stripe}>
        {loading ? 'Processing…' : `Pay $${Number(listing.pricePerMonth).toLocaleString()}`}
      </Button>
    </form>
  )
}

// ── Page wrapper ───────────────────────────────────────────────────────────────
export default function PaymentPage() {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()

  const [listing, setListing] = useState(location.state?.listing ?? null)
  const [loadingListing, setLoadingListing] = useState(!listing)
  const [status, setStatus] = useState('idle') // idle | success | error
  const [errorMsg, setErrorMsg] = useState('')
  const [provider, setProvider] = useState('STRIPE') // STRIPE | PAYPAL

  useEffect(() => {
    if (listing) return
    getListing(id)
      .then(setListing)
      .catch(() => setErrorMsg('Listing not found'))
      .finally(() => setLoadingListing(false))
  }, [id, listing])

  if (loadingListing) return <div className="container mx-auto px-4 py-16 text-center text-muted-foreground">Loading…</div>

  if (status === 'success') {
    return (
      <div className="container mx-auto px-4 py-16 max-w-sm text-center space-y-4">
        <CheckCircle className="h-16 w-16 text-green-500 mx-auto" />
        <h1 className="text-2xl font-bold">Payment successful!</h1>
        <p className="text-muted-foreground">Your rental payment for <strong>{listing?.title}</strong> has been confirmed.</p>
        <Button onClick={() => navigate('/listings')}>Browse more listings</Button>
      </div>
    )
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-md space-y-6">
      <Button variant="ghost" size="sm" asChild>
        <Link to={`/listings/${id}`}><ArrowLeft className="h-4 w-4" />Back to listing</Link>
      </Button>

      <Card>
        <CardHeader>
          <CardTitle>Complete your payment</CardTitle>
          <CardDescription>Choose your payment method</CardDescription>
        </CardHeader>

        <CardContent className="space-y-6">
          {/* Provider toggle */}
          <div className="flex rounded-md border overflow-hidden">
            <button
              onClick={() => { setProvider('STRIPE'); setErrorMsg('') }}
              className={`flex-1 py-2 text-sm font-medium transition-colors ${
                provider === 'STRIPE'
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-background text-muted-foreground hover:bg-muted'
              }`}
            >
              Credit / Debit Card
            </button>
            <button
              onClick={() => { setProvider('PAYPAL'); setErrorMsg('') }}
              className={`flex-1 py-2 text-sm font-medium transition-colors ${
                provider === 'PAYPAL'
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-background text-muted-foreground hover:bg-muted'
              }`}
            >
              PayPal
            </button>
          </div>

          {errorMsg && (
            <div className="flex items-center gap-2 rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
              <XCircle className="h-4 w-4 shrink-0" />{errorMsg}
            </div>
          )}

          {listing && provider === 'STRIPE' && (
            <Elements stripe={stripePromise}>
              <CheckoutForm
                listing={listing}
                listingId={id}
                onSuccess={() => setStatus('success')}
                onError={(msg) => setErrorMsg(msg)}
              />
            </Elements>
          )}

          {listing && provider === 'PAYPAL' && (
            <PayPalButton
              listing={listing}
              listingId={id}
              onSuccess={() => setStatus('success')}
              onError={(msg) => setErrorMsg(msg)}
            />
          )}
        </CardContent>
      </Card>
    </div>
  )
}
