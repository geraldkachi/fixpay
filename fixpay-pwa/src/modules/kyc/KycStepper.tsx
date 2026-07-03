import { useRef, useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { motion, AnimatePresence } from 'motion/react'
import { CheckCircleIcon } from '@heroicons/react/24/solid'
import { api } from '@/lib/api'
import { useAuthStore } from '@/store/auth.store'
import { PageHeader } from '@/components/layout/PageHeader'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { cn } from '@/lib/utils'

type Step = 0 | 1 | 2 // NIN, BVN, Selfie
const STEPS = ['NIN', 'BVN', 'Selfie'] as const

const ninSchema = z.object({ nin: z.string().length(11, 'NIN must be exactly 11 digits').regex(/^\d+$/, 'Digits only') })
const bvnSchema = z.object({
  bvn: z.string().length(11, 'BVN must be exactly 11 digits').regex(/^\d+$/, 'Digits only'),
  dob: z.string().min(1, 'Date of birth is required')
})

function NinStep({ onDone, onSkip }: { onDone: () => void; onSkip: () => void }) {
  const [err, setErr] = useState('')
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<{ nin: string }>({ resolver: zodResolver(ninSchema) })
  const onSubmit = async (data: { nin: string }) => {
    setErr('')
    try { await api.post('/kyc/nin', data); onDone() }
    catch { setErr('Could not verify NIN. Check the number and retry.') }
  }
  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
      <div className="text-center mb-2">
        <p className="text-[28px]">🪪</p>
        <h2 className="text-[20px] font-bold text-gray-900 mt-2">National Identity Number</h2>
        <p className="text-[14px] text-gray-500 mt-1">Enter your 11-digit NIN for identity verification.</p>
      </div>
      <Input label="NIN" type="tel" inputMode="numeric" maxLength={11} placeholder="12345678901"
        error={errors.nin?.message} {...register('nin')} />
      {err && <p className="text-ios-red text-[13px] text-center">{err}</p>}
      <Button type="submit" fullWidth loading={isSubmitting}>Verify NIN</Button>
      <Button type="button" variant="ghost" onClick={onSkip} className="text-brand" fullWidth>Continue Later</Button>
      <p className="text-[12px] text-center text-gray-400">Demo: use any 11-digit number</p>
    </form>
  )
}

// function BvnStep({ onDone, onSkip }: { onDone: () => void; onSkip: () => void }) {
//   const [err, setErr] = useState('')
//   const [awaiting, setAwaiting] = useState(false)
//   const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<{ bvn: string; dob: string }>({ resolver: zodResolver(bvnSchema) })
  
//   const startPolling = async (attempt = 0) => {
//     const delays = [10000, 240000, 600000, 1200000, 1500000]
//     if (attempt >= delays.length) {
//       setErr('BVN verification timed out. Please try again.')
//       setAwaiting(false)
//       return
//     }
    
//     setTimeout(async () => {
//       try {
//         const res = await api.get('/kyc/status')
//         const bvnStatus = res.data.verifications?.find((v: any) => v.type === 'BVN_CONSENT' || v.type === 'BVN')?.status
//         if (bvnStatus === 'VERIFIED') {
//           onDone()
//         } else if (bvnStatus === 'FAILED') {
//           setErr('BVN verification failed at NIBSS.')
//           setAwaiting(false)
//         } else {
//           startPolling(attempt + 1)
//         }
//       } catch (e) {
//         startPolling(attempt + 1)
//       }
//     }, delays[attempt])
//   }

//   const onSubmit = async (data: { bvn: string; dob: string }) => {
//     setErr('')
//     try { 
//       // const res = await api.post('/kyc/bvn/consent/initiate', data)  post
//       const res = await api.get('/kyc/bvn') 
//       if (res.data.status === 'PENDING') {
//         setAwaiting(true)
//         if (res.data.consentUrl) {
//           window.open(res.data.consentUrl, '_blank')
//         }
//         startPolling(0)
//       } else if (res.data.status === 'VERIFIED') {
//         onDone()
//       }
//     }
//     catch { setErr('Could not verify BVN. Check the number and retry.') }
//   }

//   if (awaiting) {
//     return (
//       <div className="flex flex-col items-center gap-5 pt-8">
//         <p className="text-[28px]">⏳</p>
//         <h2 className="text-[20px] font-bold text-gray-900 mt-2">Awaiting NIBSS Response</h2>
//         <p className="text-[14px] text-gray-500 mt-1 text-center px-4">
//           BVN validation is ongoing with NIBSS. You will be informed as soon as details are received.
//         </p>
//         <p className="text-[12px] text-gray-400 mt-4 text-center px-4">
//           Please complete the authentication on the NIBSS portal that opened in a new tab.
//         </p>
//         <div className="flex flex-col gap-3 w-full mt-4">
//           <Button variant="outline" onClick={() => setAwaiting(false)} className="w-full">Cancel &amp; Retry</Button>
//           <Button variant="ghost" onClick={onSkip} className="text-brand w-full">Continue Later</Button>
//         </div>
//       </div>
//     )
//   }

//   return (
//     <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
//       <div className="text-center mb-2">
//         <p className="text-[28px]">🏦</p>
//         <h2 className="text-[20px] font-bold text-gray-900 mt-2">Bank Verification Number</h2>
//         <p className="text-[14px] text-gray-500 mt-1">Enter your 11-digit BVN linked to your bank account.</p>
//       </div>
//       <Input label="Date of Birth" type="date"
//         error={errors.dob?.message} {...register('dob')} />
//       <Input label="BVN" type="tel" inputMode="numeric" maxLength={11} placeholder="00000000000"
//         error={errors.bvn?.message} {...register('bvn')} />
//       {err && <p className="text-ios-red text-[13px] text-center">{err}</p>}
//       <Button type="submit" fullWidth loading={isSubmitting}>Verify BVN</Button>
//       <Button type="button" variant="ghost" onClick={onSkip} className="text-brand" fullWidth>Continue Later</Button>
//       <p className="text-[12px] text-center text-gray-400">Demo: use any 11-digit number</p>
//     </form>
//   )
// }

function BvnStep({ onDone, onSkip }: { onDone: () => void; onSkip: () => void }) {
  const [err, setErr] = useState('')
  const [awaiting, setAwaiting] = useState(false)
  const [bvnData, setBvnData] = useState<any>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<{ 
    bvn: string; 
    dob: string;
    first_name: string; 
    last_name: string;
  }>({ 
    resolver: zodResolver(
      z.object({
        bvn: z.string().length(11, 'BVN must be exactly 11 digits').regex(/^\d+$/, 'Digits only'),
        dob: z.string().min(1, 'Date of birth is required'),
        first_name: z.string().min(1, 'First name is required'),
        last_name: z.string().min(1, 'Last name is required')
      })
    ) 
  })
  
  const onSubmit = async (data: { bvn: string; dob: string; first_name: string; last_name: string }) => {
    setErr('')
    setAwaiting(true)
    
    try { 
      // POST request with the correct body structure
      const res = await api.post('/kyc/bvn', {
        bvn: parseInt(data.bvn), // Convert to number
        first_name: data.first_name,
        last_name: data.last_name,
        dob: data.dob
      })
      
      // Check if response is successful
      if (res.data.status === 'VERIFIED') {
        // Store BVN data
        setBvnData(res.data.data)
        console.log('BVN Verification Successful:', res.data.data)
        
        // Show success state briefly before proceeding
        setAwaiting(false)
        onDone()
      } else if (res.data.status === 'PENDING') {
        // If pending, show awaiting state
        setAwaiting(true)
        if (res.data.consentUrl) {
          window.open(res.data.consentUrl, '_blank')
        }
        // Start polling if needed
        startPolling(data.bvn, data.first_name, data.last_name)
      } else {
        setErr('BVN verification failed. Please check your details and try again.')
        setAwaiting(false)
      }
    } catch (error: any) {
      console.error('BVN verification error:', error)
      setErr(error?.response?.data?.message || 'Could not verify BVN. Check the number and retry.')
      setAwaiting(false)
    }
  }

  // Polling function for pending status
  const startPolling = async (bvn: string, first_name: string, last_name: string, attempt = 0) => {
    const delays = [10000, 240000, 600000, 1200000, 1500000]
    if (attempt >= delays.length) {
      setErr('BVN verification timed out. Please try again.')
      setAwaiting(false)
      return
    }
    
    setTimeout(async () => {
      try {
        const res = await api.get('/kyc/status')
        const bvnStatus = res.data.verifications?.find((v: any) => v.type === 'BVN_CONSENT' || v.type === 'BVN')?.status
        
        if (bvnStatus === 'VERIFIED') {
          setBvnData(res.data.data)
          setAwaiting(false)
          onDone()
        } else if (bvnStatus === 'FAILED') {
          setErr('BVN verification failed at NIBSS.')
          setAwaiting(false)
        } else {
          // Still pending, continue polling
          startPolling(bvn, first_name, last_name, attempt + 1)
        }
      } catch (e) {
        startPolling(bvn, first_name, last_name, attempt + 1)
      }
    }, delays[attempt])
  }

  if (awaiting) {
    return (
      <div className="flex flex-col items-center gap-5 pt-8">
        <p className="text-[28px]">{bvnData ? '✅' : '⏳'}</p>
        <h2 className="text-[20px] font-bold text-gray-900 mt-2">
          {bvnData ? 'BVN Verified!' : 'Verifying BVN'}
        </h2>
        <p className="text-[14px] text-gray-500 mt-1 text-center px-4">
          {bvnData 
            ? 'Your BVN has been successfully verified.' 
            : 'Please wait while we verify your BVN...'}
        </p>
        
        {/* Display BVN Details if available */}
        {bvnData && (
          <div className="w-full bg-gray-50 rounded-lg p-4 space-y-2">
            <p className="text-sm font-medium text-gray-700">BVN Details:</p>
            <div className="text-sm text-gray-600 space-y-1">
              <p><span className="font-medium">Name:</span> {bvnData.applicant?.firstname} {bvnData.applicant?.lastname}</p>
              <p><span className="font-medium">BVN:</span> {bvnData.bvn?.bvn}</p>
              <p><span className="font-medium">Date of Birth:</span> {bvnData.bvn?.birthdate}</p>
              <p><span className="font-medium">Gender:</span> {bvnData.bvn?.gender}</p>
              <p><span className="font-medium">Phone:</span> {bvnData.bvn?.phone}</p>
            </div>
            <div className="mt-2 pt-2 border-t border-gray-200">
              <p className="text-xs text-green-600 font-medium">
                ✓ Verification Status: {bvnData.status?.status}
              </p>
            </div>
          </div>
        )}
        
        <div className="flex flex-col gap-3 w-full mt-4">
          {bvnData ? (
            <Button onClick={onDone} className="w-full">Continue</Button>
          ) : (
            <>
              <Button variant="outline" onClick={() => setAwaiting(false)} className="w-full">
                Cancel &amp; Retry
              </Button>
              <Button variant="ghost" onClick={onSkip} className="text-brand w-full">
                Continue Later
              </Button>
            </>
          )}
        </div>
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
      <div className="text-center mb-2">
        <p className="text-[28px]">🏦</p>
        <h2 className="text-[20px] font-bold text-gray-900 mt-2">Bank Verification Number</h2>
        <p className="text-[14px] text-gray-500 mt-1">Enter your BVN and personal details for verification.</p>
      </div>
      
      {/* Personal Details */}
      <div className="grid grid-cols-2 gap-3">
        <Input 
          label="First Name" 
          type="text"
          placeholder="Bunch"
          error={errors.first_name?.message} 
          {...register('first_name')} 
        />
        <Input 
          label="Last Name" 
          type="text"
          placeholder="Dillon"
          error={errors.last_name?.message} 
          {...register('last_name')} 
        />
      </div>
      
      <Input 
        label="Date of Birth" 
        type="date"
        error={errors.dob?.message} 
        {...register('dob')} 
      />
      
      <Input 
        label="BVN" 
        type="tel" 
        inputMode="numeric" 
        maxLength={11} 
        placeholder="95888168924"
        error={errors.bvn?.message} 
        {...register('bvn')} 
      />
      
      {err && <p className="text-ios-red text-[13px] text-center">{err}</p>}
      
      <Button type="submit" fullWidth loading={isSubmitting}>
        Verify BVN
      </Button>
      
      <Button type="button" variant="ghost" onClick={onSkip} className="text-brand" fullWidth>
        Continue Later
      </Button>
      
      <p className="text-[12px] text-center text-gray-400">
        Demo: Use any 11-digit BVN number
      </p>
    </form>
  )
}

function SelfieStep({ onDone, loading }: { onDone: () => void; loading: boolean }) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const [cameraReady, setCameraReady] = useState(false)
  const [cameraError, setCameraError] = useState('')
  const [isCapturing, setIsCapturing] = useState(false)
  const [hasPermission, setHasPermission] = useState<boolean | null>(null)

  useEffect(() => {
    let isMounted = true
    let stream: MediaStream | null = null

    const startCamera = async () => {
      try {
        // Stop any existing stream
        if (streamRef.current) {
          streamRef.current.getTracks().forEach(track => track.stop())
          streamRef.current = null
        }

        // Request camera access
        stream = await navigator.mediaDevices.getUserMedia({
          video: { 
            facingMode: 'user',
            width: { ideal: 640 },
            height: { ideal: 480 }
          },
          audio: false
        })

        if (!isMounted) return

        streamRef.current = stream

        if (videoRef.current) {
          // Reset the video element
          videoRef.current.pause()
          videoRef.current.srcObject = null
          
          // Set new stream
          videoRef.current.srcObject = stream
          
          // Wait for metadata to load before playing
          await new Promise<void>((resolve) => {
            if (!videoRef.current) return resolve()
            
            const onLoadedMetadata = () => {
              videoRef.current?.removeEventListener('loadedmetadata', onLoadedMetadata)
              resolve()
            }
            videoRef.current.addEventListener('loadedmetadata', onLoadedMetadata)
            
            // Fallback if metadata doesn't load within 3 seconds
            setTimeout(resolve, 3000)
          })

          // Try to play with error handling
          try {
            await videoRef.current.play()
            if (isMounted) {
              setCameraReady(true)
              setHasPermission(true)
            }
          } catch (playError: any) {
            // Handle play errors gracefully
            if (playError.name === 'AbortError') {
              console.log('Play was aborted, retrying...')
              // Retry playing after a small delay
              setTimeout(async () => {
                try {
                  if (videoRef.current && isMounted) {
                    await videoRef.current.play()
                    if (isMounted) {
                      setCameraReady(true)
                      setHasPermission(true)
                    }
                  }
                } catch (retryError) {
                  console.error('Retry play failed:', retryError)
                  if (isMounted) {
                    setCameraError('Failed to start camera. Please try again.')
                  }
                }
              }, 100)
            } else {
              throw playError
            }
          }
        }
      } catch (error) {
        console.error('Camera error:', error)
        if (!isMounted) return
        
        setCameraError('Could not access camera. Please allow camera permissions.')
        setHasPermission(false)
        
        // Handle specific error types
        if (error instanceof DOMException) {
          if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
            setCameraError('Camera permission denied. Please allow camera access in your browser settings.')
          } else if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
            setCameraError('No camera found on this device.')
          } else if (error.name === 'NotReadableError') {
            setCameraError('Camera is in use by another application.')
          }
        }
      }
    }

    startCamera()

    // Cleanup: stop camera when component unmounts
    return () => {
      isMounted = false
      if (streamRef.current) {
        streamRef.current.getTracks().forEach(track => track.stop())
        streamRef.current = null
      }
      if (videoRef.current) {
        videoRef.current.pause()
        videoRef.current.srcObject = null
      }
    }
  }, [])

  const captureSelfie = () => {
    if (!videoRef.current || !canvasRef.current || !cameraReady) return

    setIsCapturing(true)
    const video = videoRef.current
    const canvas = canvasRef.current

    // Set canvas dimensions to match video
    canvas.width = video.videoWidth || 640
    canvas.height = video.videoHeight || 480

    // Draw the current video frame to canvas
    const ctx = canvas.getContext('2d')
    if (ctx) {
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
      
      // Convert canvas to image data
      const imageData = canvas.toDataURL('image/jpeg', 0.8)
      
      console.log('Selfie captured!', imageData.substring(0, 50) + '...')
      
      // Simulate processing
      setTimeout(() => {
        setIsCapturing(false)
        onDone()
      }, 500)
    }
  }

  const retryCamera = () => {
    setCameraError('')
    setHasPermission(null)
    setCameraReady(false)
    
    // Restart camera
    const startCamera = async () => {
      try {
        if (streamRef.current) {
          streamRef.current.getTracks().forEach(track => track.stop())
          streamRef.current = null
        }

        const stream = await navigator.mediaDevices.getUserMedia({
          video: { 
            facingMode: 'user',
            width: { ideal: 640 },
            height: { ideal: 480 }
          },
          audio: false
        })

        streamRef.current = stream

        if (videoRef.current) {
          videoRef.current.pause()
          videoRef.current.srcObject = null
          videoRef.current.srcObject = stream
          
          await videoRef.current.play()
          setCameraReady(true)
          setHasPermission(true)
          setCameraError('')
        }
      } catch (error) {
        console.error('Camera retry error:', error)
        setCameraError('Failed to restart camera. Please try again.')
      }
    }
    
    startCamera()
  }

  return (
    <div className="flex flex-col items-center gap-5">
      <div className="text-center">
        <p className="text-[28px]">🤳</p>
        <h2 className="text-[20px] font-bold text-gray-900 mt-2">Selfie Verification</h2>
        <p className="text-[14px] text-gray-500 mt-1">Take a quick selfie to complete your identity check.</p>
      </div>

      {/* Camera Preview */}
      <div className="relative w-full max-w-sm aspect-[4/3] bg-gray-900 rounded-xl overflow-hidden">
        <video
          ref={videoRef}
          className={cn(
            "w-full h-full object-cover transition-opacity duration-500",
            cameraReady ? "opacity-100" : "opacity-0"
          )}
          muted
          playsInline
          autoPlay
        />
        
        {/* Loading State */}
        {!cameraReady && !cameraError && (
          <div className="absolute inset-0 flex items-center justify-center bg-gray-900">
            <div className="flex flex-col items-center gap-3">
              <div className="w-10 h-10 rounded-full border-4 border-white border-t-transparent animate-spin" />
              <p className="text-white text-sm">Starting camera...</p>
            </div>
          </div>
        )}

        {/* Error State */}
        {cameraError && (
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-gray-900 p-6 text-center">
            <span className="text-4xl mb-3">🚫</span>
            <p className="text-white text-sm font-medium">{cameraError}</p>
            <p className="text-gray-400 text-xs mt-2">Camera access is required for identity verification.</p>
            <div className="flex gap-3 mt-4">
              <Button variant="outline" onClick={retryCamera} className="text-white border-white/20 hover:bg-white/10">
                Try Again
              </Button>
              <Button 
                variant="ghost" 
                onClick={() => onDone()} 
                className="text-gray-400 hover:text-white"
              >
                Continue Later
              </Button>
            </div>
          </div>
        )}

        {/* Camera Guide Overlay */}
        {cameraReady && (
          <div className="absolute inset-0 pointer-events-none">
            <div className="absolute inset-0 border-2 border-white/30 rounded-xl" />
            <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-32 h-32 rounded-full border-2 border-white/50" />
            <p className="absolute bottom-4 left-1/2 -translate-x-1/2 text-white/60 text-xs bg-black/50 px-3 py-1 rounded-full">
              Center your face in the circle
            </p>
          </div>
        )}
      </div>

      {/* Hidden Canvas for capturing */}
      <canvas ref={canvasRef} className="hidden" />

      {/* Action Buttons */}
      <div className="w-full space-y-3 max-w-sm">
        {cameraReady && (
          <Button 
            fullWidth 
            loading={loading || isCapturing} 
            onClick={captureSelfie}
            className="relative"
          >
            <span className="flex items-center gap-2">
              <span className="text-xl">📸</span>
              Take Selfie & Continue
            </span>
          </Button>
        )}

        {!cameraReady && !cameraError && (
          <Button fullWidth disabled>
            <span className="flex items-center gap-2">
              <div className="w-4 h-4 rounded-full border-2 border-white border-t-transparent animate-spin" />
              Loading Camera...
            </span>
          </Button>
        )}

        <Button 
          type="button" 
          variant="ghost" 
          onClick={() => onDone()} 
          fullWidth
          className="text-brand"
        >
          Continue Later
        </Button>
      </div>

      <p className="text-[12px] text-center text-gray-400">
        {cameraReady ? 'Position your face within the circle and tap to capture' : 'Please allow camera access when prompted'}
      </p>
    </div>
  )
}

// export function KycStepper() {
//   const navigate = useNavigate()
//   const { setKycCompleted, setKycDeferred } = useAuthStore()
  
//   const [loadingStatus, setLoadingStatus] = useState(true)
//   const [unvalidatedSteps, setUnvalidatedSteps] = useState<Step[]>([0, 1, 2])
//   const [currentStepIndex, setCurrentStepIndex] = useState(0)

//   const [selfieLoading, setSelfieLoading] = useState(false)
//   const [done, setDone] = useState(false)

//   useEffect(() => {
//     async function checkStatus() {
//       try {
//         const res = await api.get('/kyc/status')
//         const verifications = res.data.verifications || []
//         const hasNin = verifications.some((v: any) => v.type === 'NIN' && v.status === 'VERIFIED')
//         const hasBvn = verifications.some((v: any) => (v.type === 'BVN' || v.type === 'BVN_CONSENT') && v.status === 'VERIFIED')
        
//         const steps: Step[] = []
//         if (!hasNin) steps.push(0)
//         if (!hasBvn) steps.push(1)
//         steps.push(2) // Selfie is always last
        
//         setUnvalidatedSteps(steps)
//       } catch (e) {
//         // use default [0,1,2] if error
//       } finally {
//         setLoadingStatus(false)
//       }
//     }
//     checkStatus()
//   }, [])

//   const handleNext = () => {
//     if (currentStepIndex + 1 < unvalidatedSteps.length) {
//       setCurrentStepIndex(curr => curr + 1)
//     }
//   }

//   const deferKyc = () => {
//     setKycDeferred(true)
//     navigate('/home')
//   }

//   const handleSkip = () => {
//     // If the next step is selfie, and we skipped NIN or BVN, we just defer KYC
//     // because we shouldn't allow completing Selfie if previous steps are explicitly skipped.
//     const nextStep = unvalidatedSteps[currentStepIndex + 1]
//     if (nextStep === 2) {
//       deferKyc()
//     } else {
//       handleNext()
//     }
//   }

//   const handleSelfie = async () => {
//     setSelfieLoading(true)
//     try {
//       await new Promise(r => setTimeout(r, 800)) // simulate network delay
//       setDone(true)
      
//       // If they skipped NIN or BVN, don't mark completely verified globally.
//       // But for this flow, if they successfully reach the end, we consider them done
//       // or we check if there are unvalidated steps left?
//       // Since they only arrive at Selfie if they didn't skip the prior steps, they are fully done.
//       setKycCompleted(true)
//       useAuthStore.getState().setKycDeferred(false)
//       setTimeout(() => navigate('/home', { replace: true }), 1800)
//     } catch { /* ignore */ }
//     finally { setSelfieLoading(false) }
//   }

//   if (loadingStatus) {
//     return (
//       <div className="h-[100dvh] flex items-center justify-center bg-[#F2F2F7]">
//         <div className="w-8 h-8 rounded-full border-4 border-gray-200 border-t-brand animate-spin" />
//       </div>
//     )
//   }

//   if (done) return (
//     <div className="h-[100dvh] flex flex-col items-center justify-center gap-4 animate-scale-in">
//       <CheckCircleIcon className="w-20 h-20 text-ios-green" />
//       <h2 className="text-[24px] font-bold text-gray-900">KYC Complete!</h2>
//       <p className="text-gray-500">Redirecting to your dashboard…</p>
//     </div>
//   )

//   const step = unvalidatedSteps[currentStepIndex]

//   return (
//     <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
//       <PageHeader title="Identity Verification" onBack={currentStepIndex > 0 ? () => setCurrentStepIndex(curr => curr - 1) : undefined} />

//       {/* Progress bar visually maps 0, 1, 2 even if skipping internally */}
//       <div className="flex gap-2 px-4 pb-4 shrink-0">
//         {STEPS.map((s, i) => {
//           const isActiveOrPassed = i <= step
//           return (
//             <div key={s} onClick={() => setCurrentStepIndex(i)} className="flex-1 flex flex-col items-center gap-1">
//               <div className={cn('h-1 w-full rounded-full transition-all duration-500', isActiveOrPassed ? 'opacity-100' : 'bg-gray-200')}
//                 style={isActiveOrPassed ? { background: 'var(--brand-primary)' } : undefined} />
//               <span className={cn('text-[11px] font-medium', isActiveOrPassed ? 'text-brand' : 'text-gray-400')}
//                 style={isActiveOrPassed ? { color: 'var(--brand-primary)' } : undefined}>{s}</span>
//             </div>
//           )
//         })}
//       </div>

//       <div className="flex-1 overflow-y-auto no-scrollbar px-4 pb-8">
//         <AnimatePresence mode="wait">
//           <motion.div key={step} initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -40 }}
//             transition={{ duration: 0.25 }}>
//             {step === 0 && <NinStep onDone={handleNext} onSkip={handleSkip} />}
//             {step === 1 && <BvnStep onDone={handleNext} onSkip={handleSkip} />}
//             {step === 2 && <SelfieStep onDone={handleSelfie} loading={selfieLoading} />}
//           </motion.div>
//         </AnimatePresence>
//       </div>
//     </div>
//   )
// }


export function KycStepper() {
  const navigate = useNavigate()
  const { setKycCompleted, setKycDeferred } = useAuthStore()
  
  const [loadingStatus, setLoadingStatus] = useState(true)
  const [unvalidatedSteps, setUnvalidatedSteps] = useState<Step[]>([0, 1, 2])
  const [currentStepIndex, setCurrentStepIndex] = useState(0)

  const [selfieLoading, setSelfieLoading] = useState(false)
  const [done, setDone] = useState(false)

  useEffect(() => {
    async function checkStatus() {
      try {
        const res = await api.get('/kyc/status')
        const verifications = res.data.verifications || []
        const hasNin = verifications.some((v: any) => v.type === 'NIN' && v.status === 'VERIFIED')
        const hasBvn = verifications.some((v: any) => (v.type === 'BVN' || v.type === 'BVN_CONSENT') && v.status === 'VERIFIED')
        
        const steps: Step[] = []
        if (!hasNin) steps.push(0)
        if (!hasBvn) steps.push(1)
        steps.push(2) // Selfie is always last
        
        setUnvalidatedSteps(steps)
      } catch (e) {
        // use default [0,1,2] if error
      } finally {
        setLoadingStatus(false)
      }
    }
    checkStatus()
  }, [])

  const handleNext = () => {
    if (currentStepIndex + 1 < unvalidatedSteps.length) {
      setCurrentStepIndex(curr => curr + 1)
    }
  }

  const deferKyc = () => {
    setKycDeferred(true)
    navigate('/home')
  }

  const handleSkip = () => {
    const nextStep = unvalidatedSteps[currentStepIndex + 1]
    if (nextStep === 2) {
      deferKyc()
    } else {
      handleNext()
    }
  }

  const handleSelfie = async () => {
    setSelfieLoading(true)
    try {
      await new Promise(r => setTimeout(r, 800))
      setDone(true)
      setKycCompleted(true)
      useAuthStore.getState().setKycDeferred(false)
      setTimeout(() => navigate('/home', { replace: true }), 1800)
    } catch { /* ignore */ }
    finally { setSelfieLoading(false) }
  }

  // Function to navigate to any step - NO RESTRICTIONS
  const goToStep = (stepIndex: number) => {
    // Check if the step exists in unvalidatedSteps
    const stepExists = unvalidatedSteps.includes(stepIndex as Step)
    if (stepExists) {
      setCurrentStepIndex(unvalidatedSteps.indexOf(stepIndex as Step))
    } else {
      // If step doesn't exist in unvalidatedSteps, add it
      // This allows navigation to ANY step regardless of validation status
      const newSteps = [...unvalidatedSteps]
      if (!newSteps.includes(stepIndex as Step)) {
        newSteps.push(stepIndex as Step)
        newSteps.sort((a, b) => a - b) // Sort to maintain order
        setUnvalidatedSteps(newSteps)
        setCurrentStepIndex(newSteps.indexOf(stepIndex as Step))
      }
    }
  }

  if (loadingStatus) {
    return (
      <div className="h-[100dvh] flex items-center justify-center bg-[#F2F2F7]">
        <div className="w-8 h-8 rounded-full border-4 border-gray-200 border-t-brand animate-spin" />
      </div>
    )
  }

  if (done) return (
    <div className="h-[100dvh] flex flex-col items-center justify-center gap-4 animate-scale-in">
      <CheckCircleIcon className="w-20 h-20 text-ios-green" />
      <h2 className="text-[24px] font-bold text-gray-900">KYC Complete!</h2>
      <p className="text-gray-500">Redirecting to your dashboard…</p>
    </div>
  )

  const step = unvalidatedSteps[currentStepIndex]

  return (
    <div className="flex flex-col h-[100dvh] bg-[#F2F2F7]">
      <PageHeader title="Identity Verification" onBack={currentStepIndex > 0 ? () => setCurrentStepIndex(curr => curr - 1) : undefined} />

      {/* Clickable Progress Bar - NO RESTRICTIONS */}
      <div className="flex gap-2 px-4 pb-4 shrink-0">
        {STEPS.map((s, i) => {
          const isActive = i === step
          const isPassed = i < step
          
          // All steps are always clickable - NO RESTRICTIONS
          const isClickable = true

          const handleStepClick = () => {
            goToStep(i) // Navigate to ANY step
          }

          return (
            <div 
              key={s} 
              onClick={handleStepClick}
              className={cn(
                'flex-1 flex flex-col items-center gap-1 transition-all duration-200',
                'cursor-pointer hover:scale-105 group'
              )}
              title={`Go to ${s}`}
            >
              {/* Progress Bar */}
              <div 
                className={cn(
                  'h-1 w-full rounded-full transition-all duration-500',
                  isActive ? 'bg-brand' : isPassed ? 'bg-brand/60' : 'bg-gray-200',
                  !isActive && 'group-hover:bg-brand/40'
                )}
              />
              
              {/* Step Label with Icon */}
              <div className="flex items-center gap-1.5">
                {/* Step Icon */}
                <span className="text-xs">
                  {s === 'NIN' && '🪪'}
                  {s === 'BVN' && '🏦'}
                  {s === 'Selfie' && '🤳'}
                </span>
                
                {/* Step Name */}
                <span 
                  className={cn(
                    'text-[11px] font-medium transition-colors',
                    isActive ? 'text-brand font-semibold' : isPassed ? 'text-brand/70' : 'text-gray-400',
                    !isActive && 'group-hover:text-brand/80'
                  )}
                >
                  {s}
                </span>
                
                {/* Checkmark for completed steps */}
                {isPassed && (
                  <svg className="w-3 h-3 text-brand/70" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                )}
              </div>
            </div>
          )
        })}
      </div>

      <div className="flex-1 overflow-y-auto no-scrollbar px-4 pb-8">
        <AnimatePresence mode="wait">
          <motion.div key={step} initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -40 }}
            transition={{ duration: 0.25 }}>
            {step === 0 && <NinStep onDone={handleNext} onSkip={handleSkip} />}
            {step === 1 && <BvnStep onDone={handleNext} onSkip={handleSkip} />}
            {step === 2 && <SelfieStep onDone={handleSelfie} loading={selfieLoading} />}
          </motion.div>
        </AnimatePresence>
      </div>
    </div>
  )
}