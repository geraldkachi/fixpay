<?php

namespace App\Http\Controllers\User;

use App\Contracts\Kyc\AmlProviderInterface;
use App\Contracts\Kyc\KycProviderInterface;
use App\Http\Controllers\Controller;
use App\Models\KycVerification;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;

class KycController extends Controller
{
    public function __construct(
        private readonly KycProviderInterface $kyc,
        private readonly AmlProviderInterface $aml,
    ) {}

    /** POST /api/kyc/bvn */
    public function verifyBvn(Request $request): JsonResponse
    {
        $data = $request->validate([
            'bvn' => 'required|string|size:11',
            'dob' => 'required|date_format:Y-m-d',
        ]);

        $user = $request->user();

        $existing = KycVerification::where('user_id', $user->id)->where('type', 'BVN')->where('verification_status', 'VERIFIED')->first();
        if ($existing) {
            return response()->json(['message' => 'BVN already verified.'], 409);
        }

        $record = KycVerification::create([
            'user_id' => $user->id,
            'type' => 'BVN',
            'identifier' => Hash::make($data['bvn']), // store hashed
            'provider' => $this->kyc->getProviderName(),
            'verification_status' => 'PENDING',
        ]);

        try {
            $result = $this->kyc->verifyBvn($data['bvn'], $data['dob']);
            $status = $result['status'] ? 'VERIFIED' : 'FAILED';

            $record->update([
                'verification_status' => $status,
                'provider_reference' => $result['reference'],
                'response_json' => $result['data'],
                'verified_at' => $status === 'VERIFIED' ? now() : null,
            ]);

            if ($status === 'VERIFIED' && $user->kyc_status === 'UNVERIFIED') {
                $user->update(['kyc_status' => 'PENDING']);
            }

            return response()->json([
                'status' => $status,
                'message' => $status === 'VERIFIED' ? 'BVN verified successfully.' : 'BVN verification failed.',
            ]);
        } catch (\Throwable $e) {
            $record->update(['verification_status' => 'FAILED', 'failure_reason' => $e->getMessage()]);

            return response()->json(['message' => 'Verification service unavailable.'], 503);
        }
    }

    /** POST /api/kyc/nin */
    public function verifyNin(Request $request): JsonResponse
    {
        $data = $request->validate(['nin' => 'required|string|size:11']);

        $user = $request->user();
        $record = KycVerification::create([
            'user_id' => $user->id,
            'type' => 'NIN',
            'identifier' => Hash::make($data['nin']),
            'provider' => $this->kyc->getProviderName(),
            'verification_status' => 'PENDING',
        ]);

        try {
            $result = $this->kyc->verifyNin($data['nin']);
            $status = $result['status'] ? 'VERIFIED' : 'FAILED';

            $record->update([
                'verification_status' => $status,
                'provider_reference' => $result['reference'],
                'response_json' => $result['data'],
                'verified_at' => $status === 'VERIFIED' ? now() : null,
            ]);

            return response()->json(['status' => $status]);
        } catch (\Throwable $e) {
            $record->update(['verification_status' => 'FAILED', 'failure_reason' => $e->getMessage()]);

            return response()->json(['message' => 'Verification service unavailable.'], 503);
        }
    }

    /** GET /api/kyc/status */
    public function status(Request $request): JsonResponse
    {
        $user = $request->user();
        $verifications = KycVerification::where('user_id', $user->id)->get();

        return response()->json([
            'kyc_status' => $user->kyc_status,
            'tier' => $user->tier,
            'verifications' => $verifications->map(fn ($v) => [
                'type' => $v->type,
                'status' => $v->verification_status,
                'provider' => $v->provider,
                'verified_at' => $v->verified_at,
            ]),
        ]);
    }
}
