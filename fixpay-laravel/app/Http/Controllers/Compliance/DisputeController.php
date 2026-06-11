<?php

namespace App\Http\Controllers\Compliance;

use App\Http\Controllers\Controller;
use App\Models\Dispute;
use App\Models\Transfer;
use App\Models\VtpassPayment;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Spatie\QueryBuilder\QueryBuilder;

class DisputeController extends Controller
{
    /** POST /api/disputes */
    public function create(Request $request): JsonResponse
    {
        $data = $request->validate([
            'related_payment_id'   => 'nullable|string',
            'related_payment_type' => 'nullable|in:VTPASS,TRANSFER',
            'category'             => 'required|in:WRONG_AMOUNT,NOT_DELIVERED,UNAUTHORIZED,DUPLICATE,OTHER',
            'reason'               => 'required|string|max:1000',
            'phone_number'         => 'required|string',
            'email'                => 'required|email',
            'transaction_date'     => 'nullable|date',
        ]);

        $user = $request->user();

        if (!empty($data['related_payment_id']) && !empty($data['related_payment_type'])) {
            $paymentId = $data['related_payment_id'];
            $isUuid = \Illuminate\Support\Str::isUuid($paymentId);
            
            if ($data['related_payment_type'] === 'VTPASS') {
                $paymentExists = VtpassPayment::where('user_id', $user->id)
                    ->where(function ($query) use ($paymentId, $isUuid) {
                        if ($isUuid) {
                            $query->where('id', $paymentId)->orWhere('payment_reference', $paymentId);
                        } else {
                            $query->where('payment_reference', $paymentId);
                        }
                    })
                    ->exists();
                if (!$paymentExists) {
                    return response()->json(['message' => 'Invalid or unauthorized payment ID.'], 403);
                }
            } elseif ($data['related_payment_type'] === 'TRANSFER') {
                $transferExists = Transfer::where('user_id', $user->id)
                    ->where(function ($query) use ($paymentId, $isUuid) {
                        if ($isUuid) {
                            $query->where('id', $paymentId)->orWhere('transfer_reference', $paymentId);
                        } else {
                            $query->where('transfer_reference', $paymentId);
                        }
                    })
                    ->exists();
                if (!$transferExists) {
                    return response()->json(['message' => 'Invalid or unauthorized payment ID.'], 403);
                }
            }
        }

        $dispute = Dispute::create(array_merge($data, [
            'user_id'      => $user->id,
            'tenant_id'    => $user->tenant_id,
            'status'       => 'OPEN',
            'sla_deadline' => now()->addWeekdays(3),
        ]));

        \Illuminate\Support\Facades\Log::info("DisputeAcknowledged Email Stub: Dispute {$dispute->ticket_number} raised by {$data['email']}");

        return response()->json([
            'dispute_id'    => $dispute->id,
            'ticket_number' => $dispute->ticket_number,
            'status'        => $dispute->status,
            'sla_deadline'  => $dispute->sla_deadline,
        ], 201);
    }

    /** GET /api/disputes */
    public function index(Request $request): JsonResponse
    {
        $disputes = Dispute::where('user_id', $request->user()->id)
            ->orderByDesc('created_at')
            ->paginate($request->input('per_page', 20));

        return response()->json($disputes);
    }

    /** GET /api/disputes/{id} */
    public function show(Request $request, string $id): JsonResponse
    {
        $dispute = Dispute::where('id', $id)->where('user_id', $request->user()->id)->firstOrFail();

        return response()->json($dispute);
    }

    // ── Admin actions ─────────────────────────────────────────────────────

    /** GET /api/admin/disputes */
    public function adminIndex(Request $request): JsonResponse
    {
        $disputes = QueryBuilder::for(Dispute::class)
            ->allowedFilters(['status', 'category'])
            ->allowedSorts(['created_at', 'sla_deadline'])
            ->defaultSort('sla_deadline')
            ->with('user')
            ->paginate($request->input('per_page', 20));

        return response()->json($disputes);
    }

    /** PUT /api/admin/disputes/{id} */
    public function resolve(Request $request, string $id): JsonResponse
    {
        $data = $request->validate([
            'status'           => 'required|in:UNDER_REVIEW,RESOLVED,REJECTED',
            'resolution_notes' => 'nullable|string',
        ]);

        $dispute = Dispute::findOrFail($id);
        $dispute->update(array_merge($data, [
            'assigned_to' => $request->user()?->id,
            'resolved_at' => in_array($data['status'], ['RESOLVED', 'REJECTED']) ? now() : null,
        ]));

        return response()->json(['message' => "Dispute {$data['status']}."]);
    }
}
