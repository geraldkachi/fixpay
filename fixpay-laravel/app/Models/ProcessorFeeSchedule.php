<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class ProcessorFeeSchedule extends Model
{
    use HasUuids;

    protected $fillable = [
        'config_id', 'min_amount_kobo', 'max_amount_kobo',
        'percentage_fee', 'flat_fee_kobo', 'cap_kobo',
        'effective_from', 'effective_to',
    ];

    protected $casts = [
        'min_amount_kobo' => 'integer',
        'max_amount_kobo' => 'integer',
        'flat_fee_kobo' => 'integer',
        'cap_kobo' => 'integer',
        'percentage_fee' => 'decimal:4',
        'effective_from' => 'datetime',
        'effective_to' => 'datetime',
    ];

    public function config(): BelongsTo
    {
        return $this->belongsTo(PaymentRailConfig::class, 'config_id');
    }

    public function calculateFee(int $amountKobo): int
    {
        $fee = (int) round($amountKobo * (float) $this->percentage_fee) + $this->flat_fee_kobo;

        if ($this->cap_kobo !== null && $fee > $this->cap_kobo) {
            $fee = $this->cap_kobo;
        }

        return $fee;
    }
}
