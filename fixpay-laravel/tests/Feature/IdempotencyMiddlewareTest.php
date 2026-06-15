<?php

namespace Tests\Feature;

use App\Models\AppUser;
use App\Models\IdempotentRequest;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Str;
use Tests\TestCase;

class IdempotencyMiddlewareTest extends TestCase
{
    use RefreshDatabase;

    private AppUser $user;

    protected function setUp(): void
    {
        parent::setUp();
        $this->user = AppUser::factory()->create();
    }

    public function test_missing_idempotency_header_returns_400_for_mutating_requests(): void
    {
        $response = $this->actingAs($this->user)
            ->postJson('/api/favourites', [
                'type' => 'airtime',
                'counterparty_name' => 'John Doe',
                'service_id' => 'mtn',
                'service_name' => 'MTN Airtime',
            ]);

        $response->assertStatus(400)
            ->assertJsonPath('message', 'X-Idempotency-Key header is required for mutating requests.');
    }

    public function test_invalid_uuid_idempotency_header_returns_400(): void
    {
        $response = $this->actingAs($this->user)
            ->withHeader('X-Idempotency-Key', 'invalid-uuid')
            ->postJson('/api/favourites', [
                'type' => 'airtime',
                'counterparty_name' => 'John Doe',
            ]);

        $response->assertStatus(400)
            ->assertJsonPath('message', 'X-Idempotency-Key must be a valid UUID.');
    }

    public function test_concurrent_request_returns_409(): void
    {
        $key = Str::uuid()->toString();
        Cache::add("idempotency_lock:{$key}", true, 30);

        $response = $this->actingAs($this->user)
            ->withHeader('X-Idempotency-Key', $key)
            ->postJson('/api/favourites', [
                'type' => 'airtime',
                'counterparty_name' => 'John Doe',
            ]);

        $response->assertStatus(409)
            ->assertJsonPath('message', 'Concurrent request in progress.');
    }

    public function test_returns_cached_response_if_completed(): void
    {
        $key = Str::uuid()->toString();

        IdempotentRequest::create([
            'idempotency_key' => $key,
            'user_id' => $this->user->id,
            'request_path' => 'api/favourites',
            'request_method' => 'POST',
            'response_code' => 201,
            'response_body' => ['message' => 'Cached favourite'],
            'status' => 'COMPLETED',
        ]);

        $response = $this->actingAs($this->user)
            ->withHeader('X-Idempotency-Key', $key)
            ->postJson('/api/favourites', [
                'type' => 'airtime',
                'counterparty_name' => 'John Doe',
            ]);

        $response->assertStatus(201)
            ->assertExactJson(['message' => 'Cached favourite']);
    }

    public function test_processes_request_and_stores_response_on_first_attempt(): void
    {
        $key = Str::uuid()->toString();

        $response = $this->actingAs($this->user)
            ->withHeader('X-Idempotency-Key', $key)
            ->postJson('/api/favourites', [
                'type' => 'airtime',
                'counterparty_name' => 'John Doe',
                'service_id' => 'mtn',
                'service_name' => 'MTN Airtime',
            ]);

        $response->assertStatus(201)
            ->assertJsonPath('message', 'Favourite saved successfully.');

        // Verify the idempotent request was saved
        $this->assertDatabaseHas('idempotent_requests', [
            'idempotency_key' => $key,
            'user_id' => $this->user->id,
            'status' => 'COMPLETED',
            'response_code' => 201,
        ]);
        
        // Verify duplicate request returns the same response without creating another DB entry
        $initialCount = \App\Models\Favourite::count();
        
        $response2 = $this->actingAs($this->user)
            ->withHeader('X-Idempotency-Key', $key)
            ->postJson('/api/favourites', [
                'type' => 'airtime',
                'counterparty_name' => 'John Doe',
                'service_id' => 'mtn',
                'service_name' => 'MTN Airtime',
            ]);

        $response2->assertStatus(201)
            ->assertJsonPath('message', 'Favourite saved successfully.');
            
        $this->assertEquals($initialCount, \App\Models\Favourite::count(), "Duplicate entry was created despite idempotency key.");
    }

    public function test_vtpass_airtime_vending_is_idempotent(): void
    {
        \Illuminate\Support\Facades\Http::fake([
            '*' => \Illuminate\Support\Facades\Http::response([
                'code' => '000',
                'content' => [
                    'transactions' => [
                        'status' => 'delivered',
                        'transactionId' => '12345'
                    ]
                ],
                'response_description' => 'TRANSACTION SUCCESSFUL'
            ], 200)
        ]);

        $wallet = \App\Models\Wallet::create([
            'user_id' => $this->user->id,
            'tenant_id' => $this->user->tenant_id,
            'balance_kobo' => 500000,
            'ledger_balance_kobo' => 500000,
            'currency' => 'NGN',
            'status' => 'ACTIVE',
            'virtual_account_number' => '1234567890',
            'virtual_account_bank' => 'Providus Bank',
            'virtual_account_bank_code' => '101',
            'virtual_account_reference' => 'ref-123',
        ]);

        $key = Str::uuid()->toString();

        echo "\n[+] Sending first airtime vending request for NGN 100.00 to 08011111111...\n";
        
        $response1 = $this->actingAs($this->user)
            ->withHeader('X-Idempotency-Key', $key)
            ->postJson('/api/payments/vtpass', [
                'service_id' => 'mtn',
                'amount_kobo' => 10000,
                'phone' => '08011111111',
            ]);

        echo "[+] Response 1 Status: " . $response1->status() . "\n";
        echo "[+] Response 1 Body: " . $response1->getContent() . "\n";

        $response1->assertStatus(200);

        echo "\n[+] Sending duplicate request with the exact same Idempotency Key...\n";

        $response2 = $this->actingAs($this->user)
            ->withHeader('X-Idempotency-Key', $key)
            ->postJson('/api/payments/vtpass', [
                'service_id' => 'mtn',
                'amount_kobo' => 10000,
                'phone' => '08011111111',
            ]);

        echo "[+] Response 2 Status: " . $response2->status() . "\n";
        echo "[+] Response 2 Body: " . $response2->getContent() . "\n";

        $response2->assertStatus(200);
        
        $this->assertEquals(
            $response1->getContent(),
            $response2->getContent(),
            "The responses must be completely identical."
        );
        
        $dbRecords = \App\Models\VtpassPayment::where('user_id', $this->user->id)->count();
        echo "\n[+] VTPass Payment Database Records for this user: {$dbRecords}\n";
        
        $this->assertEquals(1, $dbRecords, "There should only be 1 payment record created.");
    }
}
