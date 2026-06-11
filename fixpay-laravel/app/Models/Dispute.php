<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Dispute extends Model
{
    use HasUuids;

    protected $fillable = [
        'user_id', 'tenant_id', 'related_payment_id', 'related_payment_type',
        'category', 'reason', 'status', 'resolution_notes',
        'assigned_to', 'sla_deadline', 'resolved_at',
        'ticket_number', 'refund_processed'
    ];

    protected $casts = [
        'sla_deadline' => 'datetime',
        'resolved_at' => 'datetime',
        'refund_processed' => 'boolean',
    ];

    protected static function boot()
    {
        parent::boot();

        static::creating(function ($dispute) {
            if (empty($dispute->ticket_number)) {
                $dispute->ticket_number = 'DISP-' . now()->format('Ymd') . '-' . strtoupper(\Illuminate\Support\Str::random(6));
            }
        });
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(AppUser::class, 'user_id');
    }
}
