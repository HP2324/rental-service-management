import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'

/**
 * Wraps a route so only authenticated users (optionally of a specific role) can access it.
 * Unauthenticated users are redirected to /login.
 * Wrong-role users are redirected to /listings.
 */
export function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, user } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (allowedRoles && !allowedRoles.includes(user?.role)) {
    return <Navigate to="/listings" replace />
  }

  return children
}
