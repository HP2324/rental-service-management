import { useEffect, useRef } from 'react'
import { initiatePayment, capturePayPalPayment } from '@/api/payments'
import { updateListingStatus } from '@/api/listings'

export default function PayPalButton({ listing, listingId, onSuccess, onError }) {
  const containerRef = useRef(null)
  const renderedRef = useRef(false)

  useEffect(() => {
    // Avoid rendering the button twice in React StrictMode
    if (renderedRef.current) return
    if (!window.paypal) {
      onError('PayPal SDK failed to load. Please refresh the page.')
      return
    }

    renderedRef.current = true

    window.paypal.Buttons({
      style: {
        layout: 'vertical',
        color: 'blue',
        shape: 'rect',
        label: 'pay',
      },

      // Step 1: create a PayPal order via our backend
      createOrder: async () => {
        try {
          const payment = await initiatePayment({
            listingId: listing.id,
            landlordId: listing.landlordId,
            amount: listing.pricePerMonth,
            currency: 'USD',
            provider: 'PAYPAL',
            description: `Rent for ${listing.title}`,
          })
          // Return the PayPal order ID so the SDK can open the approval popup
          return payment.providerPaymentId
        } catch (err) {
          onError(err.response?.data?.message ?? 'Failed to create PayPal order')
          throw err
        }
      },

      // Step 2: buyer approved — capture the order via our backend
      onApprove: async (data) => {
        try {
          await capturePayPalPayment(data.orderID)
          await updateListingStatus(listingId, 'INACTIVE')
          onSuccess()
        } catch (err) {
          onError(err.response?.data?.message ?? 'Payment capture failed')
        }
      },

      onError: (err) => {
        console.error('PayPal error', err)
        onError('Something went wrong with PayPal. Please try again.')
      },

      onCancel: () => {
        onError('Payment cancelled.')
      },
    }).render(containerRef.current)
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return <div ref={containerRef} className="w-full" />
}
