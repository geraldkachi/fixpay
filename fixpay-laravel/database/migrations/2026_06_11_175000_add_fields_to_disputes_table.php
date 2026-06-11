<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('disputes', function (Blueprint $table) {
            $table->string('ticket_number')->nullable()->unique()->after('id');
            $table->string('phone_number')->nullable()->after('reason');
            $table->string('email')->nullable()->after('phone_number');
            $table->timestamp('transaction_date')->nullable()->after('email');
            $table->boolean('refund_processed')->default(false)->after('resolved_at');
        });
    }

    public function down(): void
    {
        Schema::table('disputes', function (Blueprint $table) {
            $table->dropColumn([
                'ticket_number',
                'phone_number',
                'email',
                'transaction_date',
                'refund_processed'
            ]);
        });
    }
};
