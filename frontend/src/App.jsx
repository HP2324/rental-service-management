import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from '@/context/AuthContext'
import { Navbar } from '@/components/Navbar'
import { ProtectedRoute } from '@/components/ProtectedRoute'

import LoginPage from '@/pages/auth/LoginPage'
import RegisterPage from '@/pages/auth/RegisterPage'
import ListingsPage from '@/pages/listings/ListingsPage'
import ListingDetailPage from '@/pages/listings/ListingDetailPage'
import DashboardPage from '@/pages/landlord/DashboardPage'
import ListingFormPage from '@/pages/landlord/ListingFormPage'
import PaymentPage from '@/pages/tenant/PaymentPage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="min-h-screen flex flex-col">
          <Navbar />
          <main className="flex-1">
            <Routes>
              {/* Public */}
              <Route path="/" element={<Navigate to="/listings" replace />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/listings" element={<ListingsPage />} />
              <Route path="/listings/:id" element={<ListingDetailPage />} />

              {/* Landlord only */}
              <Route path="/dashboard" element={
                <ProtectedRoute allowedRoles={['LANDLORD', 'ADMIN']}>
                  <DashboardPage />
                </ProtectedRoute>
              } />
              <Route path="/dashboard/new" element={
                <ProtectedRoute allowedRoles={['LANDLORD', 'ADMIN']}>
                  <ListingFormPage />
                </ProtectedRoute>
              } />
              <Route path="/dashboard/edit/:id" element={
                <ProtectedRoute allowedRoles={['LANDLORD', 'ADMIN']}>
                  <ListingFormPage />
                </ProtectedRoute>
              } />

              {/* Tenant only */}
              <Route path="/pay/:id" element={
                <ProtectedRoute allowedRoles={['TENANT']}>
                  <PaymentPage />
                </ProtectedRoute>
              } />

              {/* Catch-all */}
              <Route path="*" element={<Navigate to="/listings" replace />} />
            </Routes>
          </main>
        </div>
      </BrowserRouter>
    </AuthProvider>
  )
}
