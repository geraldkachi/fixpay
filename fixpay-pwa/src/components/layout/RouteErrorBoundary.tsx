import { useRouteError, isRouteErrorResponse, useNavigate } from 'react-router-dom'
import { ExclamationTriangleIcon } from '@heroicons/react/24/outline'

export function RouteErrorBoundary() {
  const error = useRouteError()
  const navigate = useNavigate()
  
  let errorMessage = 'An unexpected error occurred.'
  if (isRouteErrorResponse(error)) {
    errorMessage = error.statusText || error.data
  } else if (error instanceof Error) {
    errorMessage = error.message
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-[100dvh] bg-[#F2F2F7] px-6 text-center">
      <div className="w-16 h-16 rounded-full bg-red-100 flex items-center justify-center mb-6">
        <ExclamationTriangleIcon className="w-8 h-8 text-ios-red" />
      </div>
      <h1 className="text-[22px] font-bold text-gray-900 mb-2">Something went wrong</h1>
      <p className="text-[14px] text-gray-500 mb-8 max-w-[280px]">
        {errorMessage}
      </p>
      <button 
        onClick={() => {
          navigate('/', { replace: true })
          window.location.reload()
        }}
        className="w-full max-w-[200px] py-3.5 bg-gray-900 text-white rounded-full font-semibold text-[15px] pressable"
      >
        Reload App
      </button>
    </div>
  )
}
