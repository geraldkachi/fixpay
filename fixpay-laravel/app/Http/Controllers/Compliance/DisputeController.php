<?php

namespace App\Http\Controllers\Compliance;

use App\Http\Controllers\Controller;
use App\Models\Dispute;
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
        ]);

        $user = $request->user();

        $dispute = Dispute::create(array_merge($data, [
            'user_id'      => $user->id,
            'tenant_id'    => $user->tenant_id,
            'status'       => 'OPEN',
            'sla_deadline' => now()->addBusinessDays(3),
        ]));

        return response()->json([
            'dispute_id' => $dispute->id,
            'status'     => $dispute->status,
            'sla_deadline' => $dispute->sla_deadline,
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
