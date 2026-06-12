import { useDuplicatePaymentStore } from '@/store/duplicatePayment.store'

export function DuplicatePaymentModal() {
  const { isOpen, message, proceed, cancel } = useDuplicatePaymentStore()

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div className="bg-white dark:bg-gray-800 rounded-2xl w-full max-w-sm p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
        <div className="flex items-center gap-3 mb-4">
          <div className="w-10 h-10 rounded-full bg-yellow-100 text-yellow-600 flex items-center justify-center">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h3 className="text-lg font-bold text-gray-900 dark:text-white">
            Duplicate Payment?
          </h3>
        </div>
        <p className="text-gray-600 dark:text-gray-300 text-sm mb-6">
          {message}
        </p>
        <div className="flex gap-3">
          <button
            onClick={cancel}
            className="flex-1 px-4 py-3 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 text-gray-900 dark:text-white rounded-xl font-medium transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={proceed}
            className="flex-1 px-4 py-3 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-medium transition-colors"
          >
            Yes, Proceed
          </button>
        </div>
      </div>
    </div>
  )
}
