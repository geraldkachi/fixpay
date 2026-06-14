<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Models\LedgerEntry;
use App\Models\Transfer;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Carbon\Carbon;

class TransactionAdminController extends Controller
{
    /** GET /api/admin/transactions */
    public function index(Request $request): JsonResponse
    {
        $days = $request->query('days', 7);
        $since = Carbon::now()->subDays((int)$days);

        $query = Transfer::with(['user', 'wallet', 'wallet.tenant'])
            ->where('created_at', '>=', $since)
            ->orderBy('created_at', 'desc');

        if ($request->has('status')) {
            $query->where('status', $request->query('status'));
        }
        
        if ($request->has('tenant_id')) {
            $query->where('tenant_id', $request->query('tenant_id'));
        }

        $transfers = $query->paginate(50);

        return response()->json($transfers);
    }
    
    /** GET /api/admin/transactions/ledger */
    public function ledger(Request $request): JsonResponse
    {
        $days = $request->query('days', 7);
        $since = Carbon::now()->subDays((int)$days);

        $query = LedgerEntry::with(['wallet', 'wallet.tenant'])
            ->where('created_at', '>=', $since)
            ->orderBy('created_at', 'desc');

        $entries = $query->paginate(100);

        return response()->json($entries);
    }
}
