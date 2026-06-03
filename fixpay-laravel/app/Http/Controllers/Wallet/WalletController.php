<?php

namespace App\Http\Controllers\Wallet;

use App\Http\Controllers\Controller;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Spatie\QueryBuilder\QueryBuilder;

class WalletController extends Controller
{
    /** GET /api/wallet */
    public function show(Request $request): JsonResponse
    {
        $wallet = $request->user()->wallet;

        if (! $wallet) {
            return response()->json(['message' => 'Wallet not found.'], 404);
        }

        return response()->json([
            'id' => $wallet->id,
            'balance_kobo' => $wallet->balance_kobo,
            'ledger_balance_kobo' => $wallet->ledger_balance_kobo,
            'currency' => $wallet->currency,
            'status' => $wallet->status,
            'virtual_account_number' => $wallet->virtual_account_number,
            'virtual_account_bank' => $wallet->virtual_account_bank,
            'virtual_account_bank_code' => $wallet->virtual_account_bank_code,
        ]);
    }

    /** GET /api/wallet/transactions */
    public function transactions(Request $request): JsonResponse
    {
        $wallet = $request->user()->wallet;

        if (! $wallet) {
            return response()->json(['message' => 'Wallet not found.'], 404);
        }

        $entries = QueryBuilder::for($wallet->ledgerEntries())
            ->allowedFilters(['entry_type', 'correlation_id'])
            ->allowedSorts(['created_at'])
            ->defaultSort('-created_at')
            ->paginate($request->input('per_page', 20));

        return response()->json($entries);
    }
}
