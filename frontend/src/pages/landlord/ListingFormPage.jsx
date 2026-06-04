import { useState, useEffect, useRef } from 'react'
import { useNavigate, useParams, Link } from 'react-router-dom'
import { createListing, updateListing, getListing, uploadImage } from '@/api/listings'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from '@/components/ui/card'
import { ArrowLeft, ImagePlus, X, Loader2 } from 'lucide-react'

const EMPTY = {
  title: '', description: '', address: '', city: '', state: '', zipCode: '',
  pricePerMonth: '', bedrooms: '', bathrooms: '', squareFeet: '',
  amenities: '', status: 'ACTIVE',
}

export default function ListingFormPage() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()
  const fileInputRef = useRef(null)

  const [form, setForm] = useState(EMPTY)
  const [images, setImages] = useState([])       // array of URL strings
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState('')
  const [loading, setLoading] = useState(isEdit)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!isEdit) return
    getListing(id)
      .then((l) => {
        setForm({ ...l, pricePerMonth: l.pricePerMonth, amenities: (l.amenities ?? []).join(', ') })
        setImages(l.imageUrls ?? [])
      })
      .catch(() => setError('Failed to load listing'))
      .finally(() => setLoading(false))
  }, [id, isEdit])

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleFileChange = async (e) => {
    const files = Array.from(e.target.files)
    if (!files.length) return
    setUploadError('')
    setUploading(true)
    try {
      const urls = await Promise.all(files.map((f) => uploadImage(f)))
      setImages((prev) => [...prev, ...urls])
    } catch (err) {
      setUploadError(err.response?.data?.message ?? 'Upload failed — max 10 MB, JPEG/PNG/WebP only')
    } finally {
      setUploading(false)
      // Reset input so same file can be re-selected
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  const removeImage = (url) => setImages((prev) => prev.filter((u) => u !== url))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSaving(true)
    const payload = {
      ...form,
      pricePerMonth: Number(form.pricePerMonth),
      bedrooms: Number(form.bedrooms),
      bathrooms: Number(form.bathrooms),
      squareFeet: Number(form.squareFeet),
      amenities: form.amenities.split(',').map((a) => a.trim()).filter(Boolean),
      imageUrls: images,
    }
    try {
      if (isEdit) await updateListing(id, payload)
      else await createListing(payload)
      navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.message ?? 'Failed to save listing')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div className="container mx-auto px-4 py-16 text-center text-muted-foreground">Loading…</div>

  return (
    <div className="container mx-auto px-4 py-8 max-w-2xl space-y-6">
      <Button variant="ghost" size="sm" asChild>
        <Link to="/dashboard"><ArrowLeft className="h-4 w-4" />Back to dashboard</Link>
      </Button>

      <Card>
        <CardHeader>
          <CardTitle>{isEdit ? 'Edit Listing' : 'New Listing'}</CardTitle>
        </CardHeader>

        <form onSubmit={handleSubmit}>
          <CardContent className="space-y-5">
            {error && <div className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">{error}</div>}

            {/* ── Photos ── */}
            <div className="space-y-2">
              <Label>Photos</Label>

              {images.length > 0 && (
                <div className="grid grid-cols-3 gap-2">
                  {images.map((url) => (
                    <div key={url} className="relative group aspect-video rounded-md overflow-hidden border">
                      <img
                        src={url.startsWith('/') ? `http://localhost:8080${url}` : url}
                        alt="listing"
                        className="w-full h-full object-cover"
                      />
                      <button
                        type="button"
                        onClick={() => removeImage(url)}
                        className="absolute top-1 right-1 bg-black/60 text-white rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              {uploadError && (
                <p className="text-xs text-destructive">{uploadError}</p>
              )}

              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                multiple
                className="hidden"
                onChange={handleFileChange}
              />
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={uploading}
                onClick={() => fileInputRef.current?.click()}
              >
                {uploading
                  ? <><Loader2 className="h-4 w-4 animate-spin" />Uploading…</>
                  : <><ImagePlus className="h-4 w-4" />Add Photos</>
                }
              </Button>
              <p className="text-xs text-muted-foreground">JPEG, PNG or WebP · max 10 MB each · multiple allowed</p>
            </div>

            {/* ── Details ── */}
            <div className="space-y-2">
              <Label htmlFor="title">Title *</Label>
              <Input id="title" name="title" placeholder="Cozy Studio in Austin"
                value={form.title} onChange={handleChange} required />
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <textarea id="description" name="description" placeholder="Describe the property…"
                value={form.description} onChange={handleChange}
                className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring" />
            </div>

            <div className="space-y-2">
              <Label htmlFor="address">Address *</Label>
              <Input id="address" name="address" placeholder="123 Main St"
                value={form.address} onChange={handleChange} required />
            </div>

            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-2 col-span-2">
                <Label htmlFor="city">City *</Label>
                <Input id="city" name="city" placeholder="Austin"
                  value={form.city} onChange={handleChange} required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="state">State</Label>
                <Input id="state" name="state" placeholder="TX"
                  value={form.state} onChange={handleChange} />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label htmlFor="zipCode">ZIP Code</Label>
                <Input id="zipCode" name="zipCode" placeholder="78701"
                  value={form.zipCode} onChange={handleChange} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="pricePerMonth">Price / month ($) *</Label>
                <Input id="pricePerMonth" name="pricePerMonth" type="number" min={0} placeholder="1500"
                  value={form.pricePerMonth} onChange={handleChange} required />
              </div>
            </div>

            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-2">
                <Label htmlFor="bedrooms">Bedrooms</Label>
                <Input id="bedrooms" name="bedrooms" type="number" min={0} placeholder="1"
                  value={form.bedrooms} onChange={handleChange} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="bathrooms">Bathrooms</Label>
                <Input id="bathrooms" name="bathrooms" type="number" min={0} placeholder="1"
                  value={form.bathrooms} onChange={handleChange} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="squareFeet">Sq ft</Label>
                <Input id="squareFeet" name="squareFeet" type="number" min={0} placeholder="650"
                  value={form.squareFeet} onChange={handleChange} />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="amenities">Amenities (comma-separated)</Label>
              <Input id="amenities" name="amenities" placeholder="WiFi, Parking, Gym"
                value={form.amenities} onChange={handleChange} />
            </div>

            <div className="space-y-2">
              <Label htmlFor="status">Status</Label>
              <select id="status" name="status" value={form.status} onChange={handleChange}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
                {['ACTIVE', 'INACTIVE', 'PENDING'].map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
          </CardContent>

          <CardFooter className="gap-3">
            <Button type="submit" disabled={saving || uploading}>
              {saving ? 'Saving…' : isEdit ? 'Save Changes' : 'Create Listing'}
            </Button>
            <Button type="button" variant="outline" onClick={() => navigate('/dashboard')}>Cancel</Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  )
}
