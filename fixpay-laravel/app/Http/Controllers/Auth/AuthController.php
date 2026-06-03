<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;
use App\Models\AppUser;
use App\Models\Wallet;
use App\Services\OtpService;
use App\Services\Wallet\WalletService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;

class AuthController extends Controller
{
    public function __construct(
        private readonly OtpService $otpService,
        private readonly WalletService $walletService,
    ) {}

    /** POST /api/auth/register */
    public function register(Request $request): JsonResponse
    {
        $data = $request->validate([
            'phone'      => 'required|string|unique:app_users,phone',
            'email'      => 'required|email|unique:app_users,email',
            'first_name' => 'required|string|max:80',
            'last_name'  => 'required|string|max:80',
            'password'   => 'required|string|min:8',
        ]);

        $user = AppUser::create([
            'phone'         => $data['phone'],
            'email'         => $data['email'],
            'first_name'    => $data['first_name'],
            'last_name'     => $data['last_name'],
            'password_hash' => Hash::make($data['password']),
            'kyc_status'    => 'UNVERIFIED',
            'tier'          => 1,
            'status'        => 'ACTIVE',
        ]);

        // Create wallet
        $this->walletService->createWallet($user);

        // Send email OTP
        $this->otpService->send($user->email, 'email', 'verification');

        return response()->json([
            'message' => 'Registration successful. Check your email for a verification code.',
            'user_id' => $user->id,
        ], 201);
    }

    /** POST /api/auth/verify-otp */
    public function verifyOtp(Request $request): JsonResponse
    {
        $data = $request->validate([
            'identifier' => 'required|string',
            'purpose'    => 'required|in:verification,login',
            'code'       => 'required|string|size:6',
        ]);

        $valid = $this->otpService->verify($data['identifier'], $data['purpose'], $data['code']);

        if (! $valid) {
            return response()->json(['message' => 'Invalid or expired OTP.'], 422);
        }

        if ($data['purpose'] === 'verification') {
            // Mark email verified
            $field = str_contains($data['identifier'], '@') ? 'email' : 'phone';
            AppUser::where($field, $data['identifier'])->update([
                "{$field}_verified_at" => now(),
            ]);
        }

        return response()->json(['message' => 'OTP verified.']);
    }

    /** POST /api/auth/login */
    public function login(Request $request): JsonResponse
    {
        $data = $request->validate([
            'identifier' => 'required|string', // email or phone
            'password'   => 'required|string',
        ]);

        $user = AppUser::where('email', $data['identifier'])
            ->orWhere('phone', $data['identifier'])
            ->first();

        if (! $user || ! Hash::check($data['password'], $user->password_hash)) {
            return response()->json(['message' => 'Invalid credentials.'], 401);
        }

        if ($user->status !== 'ACTIVE') {
            return response()->json(['message' => 'Account is not active.'], 403);
        }

        $ttl = (int) config('sanctum.access_token_expiration', 60);
        $token = $user->createToken('access', ['*'], now()->addMinutes($ttl));

        return response()->json([
            'access_token' => $token->plainTextToken,
            'token_type' => 'Bearer',
            'expires_in' => $ttl * 60,
            'user' => [
                'id' => $user->id,
                'email' => $user->email,
                'phone' => $user->phone,
                'first_name' => $user->first_name,
                'last_name' => $user->last_name,
                'kyc_status' => $user->kyc_status,
                'tier' => $user->tier,
            ],
        ]);
    }

    /** POST /api/auth/logout */
    public function logout(Request $request): JsonResponse
    {
        $request->user()->currentAccessToken()->delete();

        return response()->json(['message' => 'Logged out.']);
    }

    /** POST /api/auth/resend-otp */
    public function resendOtp(Request $request): JsonResponse
    {
        $data = $request->validate([
            'identifier' => 'required|string',
            'purpose'    => 'required|in:verification,login',
        ]);

        $type = str_contains($data['identifier'], '@') ? 'email' : 'sms';
        $this->otpService->send($data['identifier'], $type, $data['purpose']);

        return response()->json(['message' => 'OTP sent.']);
    }
}
